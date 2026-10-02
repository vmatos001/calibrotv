package com.example.calibretv.data.sound

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.calibretv.R
import com.example.calibretv.data.model.AmbientSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Gestor de paisajes sonoros inmersivos para BookSpread.
 * Implementa Ducking Automático (atenuación al 20% con fade suave)
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
    private val scope = CoroutineScope(Dispatchers.Main)

    private fun getResId(sound: AmbientSound): Int? = when (sound) {
        AmbientSound.NONE -> null
        AmbientSound.RAIN -> R.raw.ambient_rain
        AmbientSound.FIREPLACE -> R.raw.ambient_fireplace
        AmbientSound.OCEAN -> R.raw.ambient_ocean
        AmbientSound.CAFE -> R.raw.ambient_cafe
        AmbientSound.FOREST -> R.raw.ambient_forest
    }

    fun play(sound: AmbientSound, volume: Float = 0.4f) {
        try {
            baseVolume = volume.coerceIn(0f, 1f)
            if (sound == currentSound && isPlayerActive()) {
                if (!isDucked) {
                    setActualVolume(baseVolume)
                } else {
                    setActualVolume(baseVolume * 0.20f)
                }
                return
            }
            stop()
            if (sound == AmbientSound.NONE) return
            val resId = getResId(sound) ?: return

            currentActualVolume = if (isDucked) baseVolume * 0.20f else baseVolume
            mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(currentActualVolume, currentActualVolume)
                start()
            }
            currentSound = sound
        } catch (e: Throwable) {
            Log.e(TAG, "Error playing ambient sound $sound", e)
            mediaPlayer = null
            currentSound = AmbientSound.NONE
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
        if (!isDucked) {
            setActualVolume(baseVolume)
        }
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

        if (!isPlayerActive()) return

        fadeJob?.cancel()
        val targetVolume = if (enabled) (baseVolume * 0.20f) else baseVolume
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
        fadeJob?.cancel()
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
