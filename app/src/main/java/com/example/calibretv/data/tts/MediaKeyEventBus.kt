package com.example.calibretv.data.tts

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bus de eventos globales para capturar teclas multimedia físicas
 * (como Play/Pause, HeadsetHook) emitidas por controles remotos de Fire TV y Android TV.
 */
object MediaKeyEventBus {
    private val _playPauseEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val playPauseEvents: SharedFlow<Unit> = _playPauseEvents.asSharedFlow()

    fun triggerPlayPause() {
        _playPauseEvents.tryEmit(Unit)
    }
}
