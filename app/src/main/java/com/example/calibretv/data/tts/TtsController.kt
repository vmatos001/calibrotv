package com.example.calibretv.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TtsController(context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "CalibroTTS"
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingText: String? = null
    private var pendingSpeed: Float = 1.0f

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
                Log.d(TAG, "TTS initialized successfully with locale $resolvedLocale")

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
        if (!isReady || tts == null) {
            pendingText = text
            pendingSpeed = speedRate
            return
        }
        stop()
        sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        if (sentences.isEmpty()) return

        val ttsEngine = tts ?: return
        try {
            ttsEngine.setSpeechRate(speedRate)
            ttsEngine.setPitch(pitch)
            try {
                val parts = localeCode.split("-")
                val loc = if (parts.size >= 2) java.util.Locale(parts[0], parts[1]) else java.util.Locale(localeCode)
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
            Log.e(TAG, "Error in readPage", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        _isPlaying.value = false
        _currentSentenceIndex.value = -1
        _currentSentenceText.value = ""
    }

    fun destroy() {
        stop()
        try {
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
