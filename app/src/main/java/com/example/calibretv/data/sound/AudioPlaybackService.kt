package com.example.calibretv.data.sound

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.calibretv.MainActivity
import com.example.calibretv.data.model.AmbientSound

/**
 * Servicio en primer plano desacoplado para reproducción de paisajes sonoros
 * y orquestación de audio inmersivo con gestión de Audio Focus en Android TV.
 */
class AudioPlaybackService : Service(), AudioManager.OnAudioFocusChangeListener {

    private val TAG = "AudioPlaybackService"
    private val binder = LocalBinder()
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    var ambientManager: AmbientSoundManager? = null
        private set

    inner class LocalBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        ambientManager = AmbientSoundManager(applicationContext)
        createNotificationChannel()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START_AMBIENT -> {
                val soundName = intent.getStringExtra(EXTRA_SOUND_NAME) ?: AmbientSound.NONE.name
                val sound = try { AmbientSound.valueOf(soundName) } catch (_: Exception) { AmbientSound.NONE }
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.4f)
                playAmbient(sound, volume)
            }
            ACTION_STOP_AMBIENT -> {
                stopAmbient()
            }
            ACTION_STOP_SERVICE -> {
                stopAmbient()
                stopForegroundService()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    fun playAmbient(sound: AmbientSound, volume: Float = 0.4f) {
        if (sound == AmbientSound.NONE) {
            stopAmbient()
            return
        }
        requestAudioFocus()
        startForegroundWithNotification("Paisaje sonoro: ${sound.name.lowercase().replaceFirstChar { it.uppercase() }}")
        ambientManager?.play(sound, volume)
    }

    fun stopAmbient() {
        ambientManager?.stop()
        abandonAudioFocus()
        stopForegroundService()
    }

    fun duckAmbient(enabled: Boolean) {
        ambientManager?.duck(enabled)
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                ambientManager?.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                ambientManager?.duck(true)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                ambientManager?.duck(false)
            }
        }
    }

    private fun startForegroundWithNotification(statusText: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification: Notification = builder
            .setContentTitle("BookSpread — Lectura Inmersiva")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BookSpread Audio Inmersivo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Reproducción de paisajes sonoros y lectura en voz alta"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        abandonAudioFocus()
        ambientManager?.release()
        stopForegroundService()
        instance = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "bookspread_audio_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_AMBIENT = "com.example.calibretv.action.START_AMBIENT"
        const val ACTION_STOP_AMBIENT = "com.example.calibretv.action.STOP_AMBIENT"
        const val ACTION_STOP_SERVICE = "com.example.calibretv.action.STOP_SERVICE"

        const val EXTRA_SOUND_NAME = "extra_sound_name"
        const val EXTRA_VOLUME = "extra_volume"

        @Volatile
        var instance: AudioPlaybackService? = null
            private set
    }
}
