package com.example.calibretv.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.calibretv.data.model.TtsEngineMode
import com.example.calibretv.data.storage.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Controlador de Text-to-Speech de alta fidelidad para CalibroTV en Android TV / Fire TV OS.
 * Motor principal: Microsoft Edge Neural TTS en streaming con prefetch entre páginas.
 */
class TtsController(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "CalibroTTS"
    private val edgeEngine = EdgeTtsEngine(context)
    private var nativeTts: TextToSpeech? = null
    private var isNativeReady = false
    private var currentNativePackage: String? = null

    private val prefs = PreferencesManager(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var playbackJob: Job? = null
    private var prefetchedEdgeNextPageDeferred: Deferred<ByteArray?>? = null

    var onPageFinishedListener: (() -> Unit)? = null
    var nextPageFirstChunkProvider: (() -> String?)? = null

    private val _engineMode = MutableStateFlow(TtsEngineMode.EDGE_ONLINE)
    val engineMode: StateFlow<TtsEngineMode> = _engineMode

    private val _useNativeEngine = MutableStateFlow(false)
    val useNativeEngine: StateFlow<Boolean> = _useNativeEngine

    private val _nativeEngineName = MutableStateFlow("Nativo Fire OS")
    val nativeEngineName: StateFlow<String> = _nativeEngineName

    private val _currentSentenceIndex = MutableStateFlow(-1)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex

    private val _currentSentenceText = MutableStateFlow("")
    val currentSentenceText: StateFlow<String> = _currentSentenceText

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isEngineAvailable = MutableStateFlow(true)
    val isEngineAvailable: StateFlow<Boolean> = _isEngineAvailable

    // Estados de descarga para UI
    private val _isModelDownloading = MutableStateFlow(false)
    val isModelDownloading: StateFlow<Boolean> = _isModelDownloading

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress

    private val _downloadStatusText = MutableStateFlow("")
    val downloadStatusText: StateFlow<String> = _downloadStatusText

    private var sentences = listOf<String>()

    init {
        initNativeTts()
    }

    fun setEngineMode(mode: TtsEngineMode) {
        _engineMode.value = TtsEngineMode.EDGE_ONLINE
    }

    fun setUseNativeEngine(enable: Boolean) {
        _engineMode.value = TtsEngineMode.EDGE_ONLINE
    }

    private fun initNativeTts(enginePackage: String? = null) {
        try {
            nativeTts?.stop()
            nativeTts?.shutdown()
            isNativeReady = false
            currentNativePackage = enginePackage

            val listener = TextToSpeech.OnInitListener { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val tts = nativeTts ?: return@OnInitListener
                    try {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        tts.setAudioAttributes(audioAttributes)
                        isNativeReady = true
                        _isEngineAvailable.value = true

                        val currentEngine = enginePackage ?: tts.defaultEngine ?: "desconocido"
                        Log.i(TAG, "Native TTS initialized with engine: $currentEngine")
                        Log.i(TAG, "Available engines: ${tts.engines.map { it.name }}")

                        val esAvail = tts.isLanguageAvailable(Locale("es", "ES"))
                        val enAvail = tts.isLanguageAvailable(Locale.US)
                        Log.i(TAG, "Language availability for $currentEngine: es-ES=$esAvail, en-US=$enAvail")

                        // Si el motor actual no soporta español y estamos en el motor por defecto (ej. Ivona solo inglés),
                        // cambiar automáticamente a Pico que sí tiene librerías es-ES en ROM.
                        if (esAvail < TextToSpeech.LANG_AVAILABLE && enginePackage == null) {
                            val hasPico = tts.engines.any { it.name == "com.svox.pico" }
                            if (hasPico) {
                                Log.i(TAG, "Motor predeterminado no soporta español ($esAvail). Conmutando a com.svox.pico...")
                                initNativeTts("com.svox.pico")
                                return@OnInitListener
                            }
                        }

                        val nameStr = when {
                            currentEngine.contains("ivona", ignoreCase = true) -> "Nativo (Ivona)"
                            currentEngine.contains("pico", ignoreCase = true) -> "Nativo (Pico)"
                            currentEngine.contains("google", ignoreCase = true) -> "Nativo (Google)"
                            else -> "Nativo Fire OS"
                        }
                        _nativeEngineName.value = nameStr
                    } catch (e: Exception) {
                        Log.e(TAG, "Error configuring native TTS after init", e)
                    }
                } else {
                    Log.w(TAG, "Failed to initialize native TTS (engine=$enginePackage), status=$status")
                }
            }

            if (enginePackage != null) {
                nativeTts = TextToSpeech(context.applicationContext, listener, enginePackage)
            } else {
                nativeTts = TextToSpeech(context.applicationContext, listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing native TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        // Fallback interface callback
    }

    fun isModelInstalled(localeCode: String): Boolean = true

    fun warmUpEngine(localeCode: String) {}

    fun downloadModel(localeCode: String, onFinished: ((Boolean) -> Unit)? = null) {
        onFinished?.invoke(true)
    }

    fun readPage(text: String, speedRate: Float = 1.0f, pitch: Float = 1.0f, localeCode: String = TtsVoiceCatalog.SPANISH_VOICE.code) {
        playbackJob?.cancel()
        playbackJob = null
        prefetchedEdgeNextPageDeferred?.cancel()
        prefetchedEdgeNextPageDeferred = null

        if (text.isBlank()) return

        Log.i(TAG, "Leyendo página con Microsoft Edge TTS (Voz: $localeCode)...")
        readPageEdge(text, speedRate, pitch, localeCode)
    }

    private fun readPageEdge(text: String, speedRate: Float, pitch: Float, localeCode: String) {
        val lang = if (localeCode.startsWith("en", ignoreCase = true)) "en" else "es"
        val voice = if (localeCode.contains("Neural", ignoreCase = true)) {
            localeCode
        } else {
            TtsVoiceCatalog.ensureValidVoiceCode(localeCode, lang)
        }
        _isPlaying.value = true

        playbackJob = scope.launch(Dispatchers.IO) {
            val speechChunks = splitIntoSpeechChunks(text)
            sentences = speechChunks
            if (speechChunks.isEmpty() || !isActive) {
                _isPlaying.value = false
                return@launch
            }

            // 1. Usar prefetch si ya estaba precargado el primer fragmento de la siguiente página
            val prefetched = try {
                prefetchedEdgeNextPageDeferred?.await()
            } catch (_: Exception) {
                null
            }
            prefetchedEdgeNextPageDeferred = null

            val firstAudio = prefetched ?: edgeEngine.generateAudio(speechChunks[0], speedRate, pitch, voice)
            var nextAudio: ByteArray? = firstAudio
            var completedAll = true

            // Si falla la conexión a internet en el primer chunk, usar motor nativo como fallback
            if (firstAudio == null) {
                Log.w(TAG, "Edge TTS online no respondió (posible falta de internet). Activando motor nativo como fallback...")
                withContext(Dispatchers.Main) {
                    readPageNative(text, speedRate, pitch, localeCode)
                }
                return@launch
            }

            for (idx in speechChunks.indices) {
                if (!isActive) {
                    completedAll = false
                    break
                }

                val currentChunkText = speechChunks[idx]
                val currentAudio = nextAudio ?: edgeEngine.generateAudio(currentChunkText, speedRate, pitch, voice)

                if (currentAudio == null) {
                    Log.w(TAG, "Edge TTS chunk $idx failed to generate, skipping...")
                    continue
                }

                // Iniciar precarga de la siguiente oración en segundo plano
                val prefetchJob = if (idx + 1 < speechChunks.size && isActive) {
                    launch(Dispatchers.IO) {
                        nextAudio = edgeEngine.generateAudio(speechChunks[idx + 1], speedRate, pitch, voice)
                    }
                } else if (idx + 1 == speechChunks.size && isActive) {
                    // ¡Última oración de la página actual!
                    // Precargamos la primera frase del siguiente pliego
                    launch(Dispatchers.IO) {
                        val nextChunk = withContext(Dispatchers.Main) {
                            nextPageFirstChunkProvider?.invoke()
                        }
                        if (!nextChunk.isNullOrBlank() && isActive) {
                            Log.i(TAG, "Edge TTS Cross-page prefetch: Pre-sintetizando primera oración del siguiente pliego...")
                            prefetchedEdgeNextPageDeferred = async(Dispatchers.IO) {
                                edgeEngine.generateAudio(nextChunk, speedRate, pitch, voice)
                            }
                        }
                    }
                    nextAudio = null
                    null
                } else {
                    nextAudio = null
                    null
                }

                // Actualizar resaltado en pantalla
                _currentSentenceIndex.value = idx
                _currentSentenceText.value = currentChunkText
                Log.i(TAG, "Edge TTS: Reproduciendo chunk $idx / ${speechChunks.size} (${currentChunkText.take(25)}...)")

                // Reproducir el audio MP3
                val played = edgeEngine.playAudio(currentAudio)
                if (!played && !isActive) {
                    completedAll = false
                    prefetchJob?.cancel()
                    break
                }

                // Esperar a que la precarga del siguiente fragmento esté lista
                prefetchJob?.join()

                // Pausa breve natural entre oraciones
                if (isActive && idx < speechChunks.size - 1) {
                    delay(30L)
                }
            }

            if (completedAll && isActive) {
                Log.i(TAG, "Lectura Edge TTS finalizada con éxito.")
                _isPlaying.value = false
                _currentSentenceIndex.value = -1
                _currentSentenceText.value = ""
                withContext(Dispatchers.Main) {
                    onPageFinishedListener?.invoke()
                }
            } else {
                _isPlaying.value = false
                _currentSentenceIndex.value = -1
                _currentSentenceText.value = ""
            }
        }
    }

    fun readPageNative(text: String, speedRate: Float = 1.0f, pitch: Float = 1.0f, localeCode: String = TtsVoiceCatalog.SPANISH_VOICE.code) {
        playbackJob?.cancel()
        playbackJob = null
        prefetchedEdgeNextPageDeferred?.cancel()
        prefetchedEdgeNextPageDeferred = null

        val ttsEngine = nativeTts
        if (!isNativeReady || ttsEngine == null) {
            Log.w(TAG, "Native TTS not ready yet. Scheduling playback when ready...")
            scope.launch(Dispatchers.IO) {
                var attempts = 0
                while (!isNativeReady && attempts < 20) {
                    delay(100)
                    attempts++
                }
                if (isNativeReady) {
                    withContext(Dispatchers.Main) {
                        readPageNative(text, speedRate, pitch, localeCode)
                    }
                } else {
                    Log.e(TAG, "Native TTS never became ready.")
                }
            }
            return
        }

        try {
            val speechChunks = splitIntoSpeechChunks(text)
            sentences = speechChunks
            if (speechChunks.isEmpty()) {
                _isPlaying.value = false
                return
            }

            ttsEngine.stop()
            ttsEngine.setSpeechRate(speedRate)
            ttsEngine.setPitch(pitch)

            val loc = if (localeCode.startsWith("en", ignoreCase = true)) Locale.US else Locale("es", "ES")
            val langAvail = ttsEngine.isLanguageAvailable(loc)
            Log.i(TAG, "readPageNative: language $loc availability = $langAvail on ${currentNativePackage ?: ttsEngine.defaultEngine}")

            if (langAvail >= TextToSpeech.LANG_AVAILABLE) {
                ttsEngine.language = loc
            } else {
                if (loc.language == "es" && currentNativePackage != "com.svox.pico") {
                    val hasPico = ttsEngine.engines.any { it.name == "com.svox.pico" }
                    if (hasPico) {
                        Log.i(TAG, "Conmutando dinámicamente a com.svox.pico para español...")
                        initNativeTts("com.svox.pico")
                        scope.launch(Dispatchers.IO) {
                            var waitMs = 0
                            while (!isNativeReady && waitMs < 2000) {
                                delay(100)
                                waitMs += 100
                            }
                            if (isNativeReady) {
                                withContext(Dispatchers.Main) {
                                    readPageNative(text, speedRate, pitch, localeCode)
                                }
                            }
                        }
                        return
                    }
                }
                Log.w(TAG, "Intentando setLanguage($loc) de todas formas...")
                ttsEngine.setLanguage(loc)
            }

            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    val idx = utteranceId.removePrefix("sentence_").toIntOrNull() ?: -1
                    scope.launch(Dispatchers.Main) {
                        _currentSentenceIndex.value = idx
                        _currentSentenceText.value = sentences.getOrNull(idx) ?: ""
                        _isPlaying.value = true
                    }
                }

                override fun onDone(utteranceId: String) {
                    val idx = utteranceId.removePrefix("sentence_").toIntOrNull() ?: -1
                    if (idx >= sentences.size - 1) {
                        scope.launch(Dispatchers.Main) {
                            _isPlaying.value = false
                            _currentSentenceIndex.value = -1
                            _currentSentenceText.value = ""
                            onPageFinishedListener?.invoke()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String) {
                    val idx = utteranceId.removePrefix("sentence_").toIntOrNull() ?: -1
                    if (idx >= sentences.size - 1) {
                        scope.launch(Dispatchers.Main) {
                            _isPlaying.value = false
                            _currentSentenceIndex.value = -1
                            _currentSentenceText.value = ""
                            onPageFinishedListener?.invoke()
                        }
                    }
                }

                override fun onError(utteranceId: String, errorCode: Int) {
                    Log.e(TAG, "Native TTS Utterance error on $utteranceId, code: $errorCode")
                    val idx = utteranceId.removePrefix("sentence_").toIntOrNull() ?: -1
                    if (idx >= sentences.size - 1) {
                        scope.launch(Dispatchers.Main) {
                            _isPlaying.value = false
                            _currentSentenceIndex.value = -1
                            _currentSentenceText.value = ""
                            onPageFinishedListener?.invoke()
                        }
                    }
                }
            })

            sentences.forEachIndexed { idx, sentence ->
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                val cleaned = sentence
                    .replace("—", " ")
                    .replace("–", " ")
                    .replace("«", "\"")
                    .replace("»", "\"")
                    .trim()
                val queueMode = if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                ttsEngine.speak(cleaned, queueMode, params, "sentence_$idx")
            }
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error in readPageNative", e)
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null

        prefetchedEdgeNextPageDeferred?.cancel()
        prefetchedEdgeNextPageDeferred = null

        try {
            edgeEngine.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Edge TTS", e)
        }

        try {
            nativeTts?.stop()
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
            edgeEngine.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing Edge TTS", e)
        }
        try {
            nativeTts?.shutdown()
            nativeTts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down native TTS", e)
        }
    }

    companion object {
        /**
         * Divide el texto estrictamente por signos de puntuación terminales de fin de oración (. ? ! … ...).
         * Mantiene las oraciones completas con sus comas para que el modelo aplique la prosodia y cadencia
         * melódica continua sin fragmentar la voz artificialmente.
         */
        fun splitIntoSpeechChunks(text: String): List<String> {
            if (text.isBlank()) return emptyList()

            val normalized = text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace(Regex("[ \\t]+"), " ")
                .trim()

            val rawParagraphs = normalized.split(Regex("\\n+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val chunks = mutableListOf<String>()

            for (para in rawParagraphs) {
                // Dividir únicamente por signos de puntuación de fin de oración (. ? ! … ...)
                val sentences = para.split(Regex("(?<=[.!?…])\\s+|(?<=\\.\\.\\.)\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                for (sentence in sentences) {
                    // Preservar la oración completa con todas sus comas y guiones de diálogo.
                    // Las comas NUNCA se cortan por separado porque Piper las modula con prosodia continua.
                    // Solo si un autor escribió una frase anormalmente gigantesca sin puntos (> 280 caracteres),
                    // la dividimos en pausas mayores (; o :) para evitar sobrecargar la memoria.
                    if (sentence.length > 280 && (sentence.contains(";") || sentence.contains(":"))) {
                        val subParts = sentence.split(Regex("(?<=[;:])\\s+"))
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        chunks.addAll(subParts)
                    } else {
                        chunks.add(sentence)
                    }
                }
            }

            return chunks.filter { it.any { c -> c.isLetterOrDigit() } }
        }
    }
}
