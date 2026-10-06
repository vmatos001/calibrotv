package com.example.calibretv.ui.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Resuelve la Activity asociada a un Context desenrollando posibles ContextWrappers.
 */
fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Activa o desactiva la bandera FLAG_KEEP_SCREEN_ON en la ventana de la Activity.
 * Evita que el protector de pantalla / Daydream de Fire OS o Android TV se active
 * mientras el usuario está leyendo o escuchando TTS.
 */
fun Activity.setKeepScreenOn(keepOn: Boolean) {
    try {
        if (keepOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    } catch (_: Throwable) {}
}

/**
 * Efecto de Compose que mantiene encendida la pantalla de la TV (Keep Screen On)
 * mientras la pantalla de lectura está activa, y restaura el comportamiento normal
 * al salir o desmontar la pantalla.
 */
@Composable
fun KeepScreenOnEffect() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val activity = context.findActivity()
        activity?.setKeepScreenOn(true)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                activity?.setKeepScreenOn(true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.setKeepScreenOn(false)
        }
    }
}
