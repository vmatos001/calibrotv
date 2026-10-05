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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Controlador de Text-to-Speech híbrido para CalibroTV.
 * Implementa una cola de precarga concurrente (Pipelined Prefetcher) para que mientras
 * se reproduce una oración, las siguientes ya se estén sintetizando en paralelo en el servidor Piper TTS.
 * Esto elimina los silencios de 8-10 segundos entre frases, logrando una locución fluida.
 * Cuenta con fallback automático al motor nativo si el servidor no responde.
 */
class TtsController(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "CalibroTTS"
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingText: String? = null
    private var pendingSpeed: Float = 1.0f

    private val prefs = PreferencesManager(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var producerJob: Job? = null
    private var consumerJob: Job? = null
    private var piperPlayer: MediaPlayer? = null

    // Caché concurrente en RAM para la precarga de oraciones
    private val audioCache = ConcurrentHashMap<Int, ByteArray>()
    private val failedIndices = ConcurrentHashMap.newKeySet<Int>()

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
                    if (!prefs.isPiperTtsEnabled()) {
                        readPage(text, spd)
                    }
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
        val speechChunks = splitIntoSpeechChunks(text, targetSize = 220)
        sentences = speechChunks
        if (sentences.isEmpty()) return

        val usePiper = prefs.isPiperTtsEnabled()
        if (usePiper) {
            audioCache.clear()
            failedIndices.clear()
            _isPlaying.value = true

            val serverUrl = prefs.getPiperTtsUrl()
            val secret = prefs.getPiperTtsSecret()
            val selectedVoice = PiperTtsClient.resolveVoiceId(localeCode, prefs.getPiperVoice())

            // Precarga anticipada del modelo en RAM del servidor para latencia 0 ms
            scope.launch(Dispatchers.IO) {
                PiperTtsClient.preloadVoice(selectedVoice, serverUrl, secret)
            }

            // 1. Productor concurrente: sintetiza bloques continuos por adelantado (Pipelining)
            producerJob = scope.launch(Dispatchers.IO) {
                for (idx in sentences.indices) {
                    if (!isActive) break

                    // Si la cola está suficientemente llena (> 3 bloques por delante), pausa breve
                    while (audioCache.size >= 4 && isActive) {
                        delay(60)
                    }

                    val chunkText = sentences[idx]
                    val result = PiperTtsClient.synthesize(
                        text = chunkText,
                        voice = selectedVoice,
                        speed = speedRate,
                        pitch = pitch,
                        serverUrl = serverUrl,
                        secret = secret
                    )
                    if (result.isSuccess && result.getOrNull() != null) {
                        audioCache[idx] = result.getOrNull()!!
                        Log.d(TAG, "Prefetched speech chunk $idx / ${sentences.size} (${chunkText.take(25)}...)")
                    } else {
                        Log.w(TAG, "Failed prefetching chunk $idx: ${result.exceptionOrNull()?.message}")
                        failedIndices.add(idx)
                    }
                }
            }

            // 2. Consumidor: reproduce la cola sin silencios
            consumerJob = scope.launch(Dispatchers.Main) {
                var fallbackNeeded = false
                var fallbackStartIndex = 0

                for (idx in sentences.indices) {
                    if (!isActive) break
                    val chunkText = sentences[idx]
                    _currentSentenceIndex.value = idx
                    _currentSentenceText.value = chunkText

                    // Esperar a que el fragmento esté sintetizado por el productor.
                    // Para el bloque 0 damos margen de calentamiento (hasta 15s), para los siguientes
                    // ya están en RAM con antelación porque cada bloque habla durante 12-16s.
                    var waitTime = 0
                    val maxWait = if (idx == 0) 15000 else 8000
                    while (!audioCache.containsKey(idx) && !failedIndices.contains(idx) && waitTime < maxWait && isActive) {
                        delay(40)
                        waitTime += 40
                    }

                    val audioBytes = audioCache.remove(idx)
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        val played = playWavBytes(audioBytes, idx)
                        if (!played) {
                            fallbackNeeded = true
                            fallbackStartIndex = idx
                            break
                        }
                    } else {
                        Log.w(TAG, "Timeout or error on Piper speech chunk $idx. Falling back to native TTS.")
                        fallbackNeeded = true
                        fallbackStartIndex = idx
                        break
                    }
                }

                if (fallbackNeeded) {
                    val remainingText = sentences.drop(fallbackStartIndex).joinToString(" ")
                    if (remainingText.isNotBlank()) {
                        readPageNative(remainingText, speedRate, pitch, localeCode)
                    }
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

    private suspend fun playWavBytes(bytes: ByteArray, sentenceIdx: Int): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "piper_tts_$sentenceIdx.wav")
        try {
            FileOutputStream(tempFile).use { fos ->
                fos.write(bytes)
                fos.flush()
            }

            suspendCoroutine { cont ->
                try {
                    val player = piperPlayer ?: MediaPlayer().also { piperPlayer = it }
                    player.reset()
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    player.setDataSource(tempFile.absolutePath)
                    player.setOnCompletionListener {
                        cont.resume(true)
                    }
                    player.setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        cont.resume(false)
                        true
                    }
                    player.prepare()
                    player.start()
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing audio via MediaPlayer", e)
                    cont.resume(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing temporary audio file", e)
            false
        } finally {
            try { tempFile.delete() } catch (_: Exception) {}
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
            sentences = splitIntoSpeechChunks(text, targetSize = 200)
            ttsEngine.setSpeechRate(speedRate)
            ttsEngine.setPitch(pitch)

            val voiceObj = TtsVoiceCatalog.findVoice(localeCode)
            val loc = voiceObj?.nativeLocale ?: run {
                val parts = localeCode.split("-")
                if (parts.size >= 2) Locale(parts[0], parts[1]) else Locale(localeCode)
            }
            if (ttsEngine.isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE) {
                ttsEngine.language = loc
            }

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
                    Log.e(TAG, "Native TTS Utterance error on $utteranceId, code: $errorCode")
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
        producerJob?.cancel()
        producerJob = null
        consumerJob?.cancel()
        consumerJob = null
        audioCache.clear()
        failedIndices.clear()

        try {
            piperPlayer?.stop()
            piperPlayer?.reset()
        } catch (_: Exception) {}

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
            piperPlayer?.release()
            piperPlayer = null
        } catch (_: Exception) {}
        try {
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down native TTS", e)
        }
    }

    companion object {
        /**
         * Agrupa oraciones en fragmentos naturales de locución de ~200-240 caracteres.
         * Cada fragmento tarda entre 12 y 16 segundos en reproducirse, permitiendo
         * que el sintetizador Piper en CPU genere los siguientes bloques con holgura
         * (3-4 segundos de inferencia por bloque), eliminando las pausas de 4-6 segundos.
         */
        fun splitIntoSpeechChunks(text: String, targetSize: Int = 220): List<String> {
            val rawSentences = text.split(Regex("(?<=[.!?:;\\n])\\s+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (rawSentences.isEmpty()) return emptyList()

            val chunks = mutableListOf<String>()
            var currentChunk = StringBuilder()

            fun flushCurrent() {
                if (currentChunk.isNotBlank()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
            }

            for (sentence in rawSentences) {
                if (sentence.length > 280) {
                    flushCurrent()
                    val clauses = sentence.split(Regex("(?<=[,])\\s+"))
                    var clauseBuffer = StringBuilder()
                    for (clause in clauses) {
                        if (clauseBuffer.isNotEmpty() && clauseBuffer.length + clause.length + 1 > targetSize) {
                            chunks.add(clauseBuffer.toString().trim())
                            clauseBuffer = StringBuilder(clause)
                        } else {
                            if (clauseBuffer.isNotEmpty()) clauseBuffer.append(" ")
                            clauseBuffer.append(clause)
                        }
                    }
                    if (clauseBuffer.isNotEmpty()) {
                        chunks.add(clauseBuffer.toString().trim())
                    }
                } else {
                    if (currentChunk.isEmpty()) {
                        currentChunk.append(sentence)
                    } else if (currentChunk.length + sentence.length + 1 <= targetSize) {
                        currentChunk.append(" ").append(sentence)
                    } else {
                        flushCurrent()
                        currentChunk.append(sentence)
                    }
                }
            }
            flushCurrent()
            return chunks
        }
    }
}
