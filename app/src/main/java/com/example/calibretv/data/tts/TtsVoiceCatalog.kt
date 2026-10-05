package com.example.calibretv.data.tts

/**
 * Catálogo de voces para Text-To-Speech (TTS) en CalibroTV.
 * Soporta 8 voces oficiales: 4 en español y 4 en inglés.
 * Se activan y filtran dinámicamente según el idioma del libro que se esté leyendo.
 */
data class TtsVoice(
    val code: String,        // Código de configuración (ej. "es-ES", "en-US")
    val displayName: String, // Nombre para la UI (ej. "España", "EE.UU.")
    val language: String     // Idioma ("es" o "en")
)

object TtsVoiceCatalog {

    val SPANISH_VOICES = listOf(
        TtsVoice("es-ES", "España", "es"),
        TtsVoice("es-MX", "México", "es"),
        TtsVoice("es-US", "Latino", "es"),
        TtsVoice("es-AR", "Argentina", "es")
    )

    val ENGLISH_VOICES = listOf(
        TtsVoice("en-US", "EE.UU.", "en"),
        TtsVoice("en-GB", "Reino Unido", "en"),
        TtsVoice("en-AU", "Australia", "en"),
        TtsVoice("en-CA", "Canadá", "en")
    )

    val ALL_VOICES = SPANISH_VOICES + ENGLISH_VOICES

    fun getVoicesForLanguage(lang: String): List<TtsVoice> {
        return if (lang.equals("en", ignoreCase = true)) ENGLISH_VOICES else SPANISH_VOICES
    }

    fun findVoice(code: String): TtsVoice? {
        return ALL_VOICES.find { it.code.equals(code, ignoreCase = true) }
    }

    fun getVoiceDisplayName(code: String, lang: String): String {
        val found = findVoice(code)
        if (found != null && found.language.equals(lang, ignoreCase = true)) {
            return found.displayName
        }
        val fallbackList = getVoicesForLanguage(lang)
        return fallbackList.first().displayName
    }

    fun getNextVoice(currentCode: String, lang: String): TtsVoice {
        val list = getVoicesForLanguage(lang)
        val idx = list.indexOfFirst { it.code.equals(currentCode, ignoreCase = true) }
        return if (idx < 0) list.first() else list[(idx + 1) % list.size]
    }

    fun ensureValidVoiceCode(code: String, lang: String): String {
        val found = findVoice(code)
        if (found != null && found.language.equals(lang, ignoreCase = true)) {
            return found.code
        }
        return getVoicesForLanguage(lang).first().code
    }
}
