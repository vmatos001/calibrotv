package com.example.calibretv.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.calibretv.data.model.AmbientSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gestor de paisajes sonoros inmersivos para BookSpread.
 * Reproduce en bucle sin interrupciones con caché local en disco
 * e implementa Ducking Automático (atenuación al 20% con fade suave)
 * coordinado con la lectura en voz alta (TTS) para evitar colisiones acústicas.
 */
class AmbientSoundManager(private val context: Context) {

    private val TAG = "AmbientSoundManager"
    private var mediaPlayer: MediaPlayer? = null
    var currentSound: AmbientSound = AmbientSound.NONE
        private set

    var baseVolume: Float = 0.4f
        private set

    private var currentActualVolume: Float = 0.4f
    private var isDucked: Boolean = false
    private var fadeJob: Job? = null
    private var playJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private fun getStreamUrl(sound: AmbientSound): String? = when (sound) {
        AmbientSound.NONE -> null
        AmbientSound.RAIN -> "https://bookspread-app-2026.web.app/audio/ambient_rain.ogg"
        AmbientSound.FIREPLACE -> "https://bookspread-app-2026.web.app/audio/ambient_fireplace.ogg"
        AmbientSound.OCEAN -> "https://bookspread-app-2026.web.app/audio/ambient_ocean.ogg"
        AmbientSound.CAFE -> "https://bookspread-app-2026.web.app/audio/ambient_cafe.ogg"
        AmbientSound.FOREST -> "https://bookspread-app-2026.web.app/audio/ambient_forest.ogg"
        AmbientSound.LOFI -> "https://bookspread-app-2026.web.app/audio/ambient_lofi.ogg"
    }

    private fun getCacheFile(sound: AmbientSound): File {
        val dir = File(context.cacheDir, "ambient_sounds").apply { mkdirs() }
        return File(dir, "ambient_${sound.name.lowercase()}.ogg")
    }

    private fun downloadToCache(urlStr: String, targetFile: File): File? {
        val uniqueSuffix = "${System.currentTimeMillis()}_${(1000..9999).random()}"
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.$uniqueSuffix.tmp")
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "CalibroTV/3.41 (Android TV)")
            conn.setRequestProperty("Accept", "*/*")
            conn.connect()

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                conn.inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output, bufferSize = 8192)
                        output.flush()
                    }
                }
                val downloadedSize = tempFile.length()
                Log.i(TAG, "Downloaded $downloadedSize bytes for $urlStr to ${tempFile.name}")
                if (downloadedSize > 5000L) {
                    if (targetFile.exists()) targetFile.delete()
                    if (tempFile.renameTo(targetFile)) {
                        targetFile
                    } else {
                        Log.w(TAG, "renameTo failed, using tempFile directly: ${tempFile.absolutePath}")
                        tempFile
                    }
                } else {
                    Log.w(TAG, "Downloaded file too small: $downloadedSize bytes")
                    tempFile.delete()
                    null
                }
            } else {
                Log.w(TAG, "Download failed for $urlStr, HTTP: $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception downloading $urlStr: ${e.message}", e)
            if (tempFile.exists()) tempFile.delete()
            null
        }
    }

    fun play(sound: AmbientSound, volume: Float = 0.4f) {
        baseVolume = volume.coerceIn(0f, 1f)

        // Si ya está reproduciendo este mismo sonido, o si ya se está descargando/iniciando, sólo actualiza volumen
        if (sound == currentSound && (isPlayerActive() || playJob?.isActive == true)) {
            val target = if (isDucked) (baseVolume * 0.20f) else baseVolume
            setActualVolume(target)
            return
        }

        playJob?.cancel()
        playJob = null
        stop()

        if (sound == AmbientSound.NONE) return

        val streamUrl = getStreamUrl(sound) ?: return
        currentSound = sound

        playJob = scope.launch(Dispatchers.IO) {
            try {
                val cacheFile = getCacheFile(sound)
                val audioFile: File? = if (cacheFile.exists() && cacheFile.length() > 5000L) {
                    Log.d(TAG, "Playing $sound from local cache: ${cacheFile.absolutePath} (${cacheFile.length()} bytes)")
                    cacheFile
                } else {
                    Log.i(TAG, "Downloading $sound from $streamUrl to cache...")
                    downloadToCache(streamUrl, cacheFile)
                }

                withContext(Dispatchers.Main) {
                    if (!isActive || currentSound != sound) return@withContext
                    if (audioFile != null && audioFile.exists()) {
                        startMediaPlayerFromFile(audioFile, sound)
                    } else {
                        Log.e(TAG, "Could not acquire audio file for $sound")
                    }
                }
            } catch (e: Throwable) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e(TAG, "Error preparing ambient sound $sound", e)
                }
            }
        }
    }

    private fun startMediaPlayerFromFile(file: File, sound: AmbientSound) {
        try {
            mediaPlayer?.release()
            currentActualVolume = if (isDucked) (baseVolume * 0.20f) else baseVolume
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                // Usar FileDescriptor garantiza que el reproductor no tenga restricciones de permisos ni problemas de streaming
                FileInputStream(file).use { fis ->
                    setDataSource(fis.fd, 0L, file.length())
                }
                isLooping = true
                setVolume(currentActualVolume, currentActualVolume)
                setOnPreparedListener { mp ->
                    try {
                        mp.start()
                        Log.i(TAG, "Ambient sound $sound started successfully! (file: ${file.name}, size: ${file.length()} bytes, vol: $currentActualVolume)")
                    } catch (e: Throwable) {
                        Log.e(TAG, "Failed to start player for $sound", e)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error ($sound): what=$what extra=$extra")
                    try {
                        if (file.exists()) file.delete()
                    } catch (_: Throwable) {}
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize MediaPlayer for $sound with file ${file.absolutePath}", e)
        }
    }

    private fun isPlayerActive(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: Throwable) {
            false
        }
    }

    fun setVolume(volume: Float) {
        baseVolume = volume.coerceIn(0f, 1f)
        val target = if (isDucked) (baseVolume * 0.20f) else baseVolume
        setActualVolume(target)
    }

    private fun setActualVolume(vol: Float) {
        currentActualVolume = vol.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(currentActualVolume, currentActualVolume)
        } catch (_: Throwable) {}
    }

    /**
     * Aplica Ducking inteligente: Atenúa el audio ambiental al 20%
     * suavemente con una animación de 300 ms cuando el TTS habla,
     * y restaura el volumen al 100% de manera gradual al terminar.
     */
    fun duck(enabled: Boolean, durationMs: Long = 300L) {
        if (isDucked == enabled) return
        isDucked = enabled

        val targetVolume = if (enabled) (baseVolume * 0.20f) else baseVolume
        if (!isPlayerActive()) {
            currentActualVolume = targetVolume
            return
        }

        fadeJob?.cancel()
        val startVolume = currentActualVolume

        fadeJob = scope.launch {
            val steps = 10
            val stepDelay = (durationMs / steps).coerceAtLeast(10L)
            for (i in 1..steps) {
                val progress = i.toFloat() / steps
                val interpolated = startVolume + (targetVolume - startVolume) * progress
                setActualVolume(interpolated)
                delay(stepDelay)
            }
            setActualVolume(targetVolume)
        }
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        fadeJob?.cancel()
        fadeJob = null
        isDucked = false
        try {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                } catch (_: Throwable) {}
                try {
                    player.release()
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
        mediaPlayer = null
        currentSound = AmbientSound.NONE
    }

    fun release() = stop()
}
