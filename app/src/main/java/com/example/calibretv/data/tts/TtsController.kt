package com.example.calibretv.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.calibretv.data.storage.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Controlador de Text-to-Speech híbrido para CalibroTV.
 * Soporta de manera prioritaria el motor neuronal Piper TTS (VITS Architecture en red local/VPS)
 * con fallback transparente e instantáneo al motor nativo de Android si el servidor no está disponible.
 */
class TtsController(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "CalibroTTS"
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingText: String? = null
    private var pendingSpeed: Float = 1.0f

    private val prefs = PreferencesManager(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var piperJob: Job? = null
    private var piperPlayer: MediaPlayer? = null

    var onPageFinishedListener: (() -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TextToSpeech", e)
        }
    }

    private val _currentSentenceIndex = MutableStateFlow(-1)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex

    private val _currentSentenceText = MutableStateFlow("")
    val currentSentenceText: StateFlow<String> = _currentSentenceText

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isEngineAvailable = MutableStateFlow(true)
    val isEngineAvailable: StateFlow<Boolean> = _isEngineAvailable

    private var sentences = listOf<String>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isEngineAvailable.value = true
            val ttsEngine = tts ?: return
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                ttsEngine.setAudioAttributes(audioAttributes)

                val candidates = listOf(
                    Locale.getDefault(),
                    Locale("es", "ES"),
                    Locale("es", "US"),
                    Locale("es", "MX"),
                    Locale("es"),
                    Locale.US
                )
                var resolvedLocale = Locale.getDefault()
                for (loc in candidates) {
                    val avail = ttsEngine.isLanguageAvailable(loc)
                    if (avail >= TextToSpeech.LANG_AVAILABLE) {
                        resolvedLocale = loc
                        break
                    }
                }
                ttsEngine.language = resolvedLocale
                isReady = true
                Log.d(TAG, "Native TTS initialized successfully with locale $resolvedLocale")

                pendingText?.let { text ->
                    val spd = pendingSpeed
                    pendingText = null
                    readPage(text, spd)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring TTS onInit", e)
            }
        } else {
            _isEngineAvailable.value = false
            Log.e(TAG, "Failed to initialize TextToSpeech engine, status: $status")
        }
    }

    fun setVoiceLocale(locale: Locale) {
        if (isReady && tts != null) {
            try {
                tts?.language = locale
            } catch (_: Exception) {}
        }
    }

    fun setPitch(pitch: Float) {
        if (isReady && tts != null) {
            try {
                tts?.setPitch(pitch)
            } catch (_: Exception) {}
        }
    }

    fun readPage(text: String, speedRate: Float = 1.0f, pitch: Float = 1.0f, localeCode: String = "es-ES") {
        stop()
        sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        if (sentences.isEmpty()) return

        val usePiper = prefs.isPiperTtsEnabled()
        if (usePiper) {
            piperJob = scope.launch {
                val serverUrl = prefs.getPiperTtsUrl()
                val secret = prefs.getPiperTtsSecret()
                val selectedVoice = PiperTtsClient.resolveVoiceId(localeCode, prefs.getPiperVoice())
                
                var piperSuccess = true
                _isPlaying.value = true

                for (idx in sentences.indices) {
                    val sentence = sentences[idx]
                    _currentSentenceIndex.value = idx
                    _currentSentenceText.value = sentence

                    val result = PiperTtsClient.synthesize(
                        text = sentence,
                        voice = selectedVoice,
                        speed = speedRate,
                        serverUrl = serverUrl,
                        secret = secret
                    )

                    if (result.isSuccess) {
                        val audioBytes = result.getOrNull()
                        if (audioBytes != null && audioBytes.isNotEmpty()) {
                            val played = playWavBytes(audioBytes)
                            if (!played) {
                                piperSuccess = false
                                break
                            }
                        }
                    } else {
                        Log.w(TAG, "Piper TTS failed for sentence $idx, falling back to native TTS: ${result.exceptionOrNull()?.message}")
                        piperSuccess = false
                        break
                    }
                }

                if (!piperSuccess) {
                    // Fallback transparente al motor nativo para el resto del texto
                    readPageNative(text, speedRate, pitch, localeCode)
                } else {
                    _isPlaying.value = false
                    _currentSentenceIndex.value = -1
                    _currentSentenceText.value = ""
                    onPageFinishedListener?.invoke()
                }
            }
        } else {
            readPageNative(text, speedRate, pitch, localeCode)
        }
    }

    private suspend fun playWavBytes(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "piper_tts_temp.wav")
            FileOutputStream(tempFile).use { fos ->
                fos.write(bytes)
                fos.flush()
            }

            suspendCoroutine { cont ->
                try {
                    piperPlayer?.release()
                    piperPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        setDataSource(tempFile.absolutePath)
                        setOnCompletionListener {
                            cont.resume(true)
                        }
                        setOnErrorListener { _, what, extra ->
                            Log.e(TAG, "MediaPlayer error playing Piper audio: what=$what, extra=$extra")
                            cont.resume(false)
                            true
                        }
                        prepare()
                        start()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing MediaPlayer for Piper TTS", e)
                    cont.resume(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing temporary Piper audio file", e)
            false
        }
    }

    private fun readPageNative(text: String, speedRate: Float, pitch: Float, localeCode: String) {
        if (!isReady || tts == null) {
            pendingText = text
            pendingSpeed = speedRate
            return
        }

        val ttsEngine = tts ?: return
        try {
            ttsEngine.setSpeechRate(speedRate)
            ttsEngine.setPitch(pitch)
            try {
                val parts = localeCode.split("-")
                val loc = if (parts.size >= 2) Locale(parts[0], parts[1]) else Locale(localeCode)
                if (ttsEngine.isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE) {
                    ttsEngine.language = loc
                }
            } catch (_: Exception) {}

            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    val idx = utteranceId.removePrefix("sentence_").toIntOrNull() ?: -1
                    _currentSentenceIndex.value = idx
                    _currentSentenceText.value = sentences.getOrNull(idx) ?: ""
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String) {
                    val idx = utteranceId.removePrefix("sentence_").toIntOrNull() ?: -1
                    if (idx >= sentences.size - 1) {
                        _isPlaying.value = false
                        _currentSentenceIndex.value = -1
                        _currentSentenceText.value = ""
                        onPageFinishedListener?.invoke()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String) {
                    _isPlaying.value = false
                    _currentSentenceIndex.value = -1
                    _currentSentenceText.value = ""
                }

                override fun onError(utteranceId: String, errorCode: Int) {
                    Log.e(TAG, "TTS Utterance error on $utteranceId, code: $errorCode")
                    _isPlaying.value = false
                    _currentSentenceIndex.value = -1
                    _currentSentenceText.value = ""
                }
            })

            sentences.forEachIndexed { idx, sentence ->
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                ttsEngine.speak(sentence, TextToSpeech.QUEUE_ADD, params, "sentence_$idx")
            }
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error in readPageNative", e)
        }
    }

    fun stop() {
        piperJob?.cancel()
        piperJob = null
        try {
            piperPlayer?.stop()
            piperPlayer?.release()
        } catch (_: Exception) {}
        piperPlayer = null

        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping native TTS", e)
        }
        _isPlaying.value = false
        _currentSentenceIndex.value = -1
        _currentSentenceText.value = ""
    }

    fun destroy() {
        stop()
        scope.cancel()
        try {
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down native TTS", e)
        }
    }
}
