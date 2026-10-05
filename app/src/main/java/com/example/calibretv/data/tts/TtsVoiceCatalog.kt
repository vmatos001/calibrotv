package com.example.calibretv.data.tts

import java.util.Locale

/**
 * Catálogo Exclusivo de 8 Voces Neuronales Piper TTS para CalibroTV.
 * 4 en Español y 4 en Inglés, optimizadas para lectura continua de audiolibros.
 */
data class TtsVoice(
    val code: String,        // Identificador oficial del modelo Piper en el servidor (ej: "es_MX-claude-high")
    val displayName: String, // Nombre para el HUD del TV (ej: "Claude (Neutro)")
    val language: String,    // "es" o "en"
    val description: String, // Descripción del arquetipo vocal
    val nativeLocale: Locale // Locale de fallback para el motor nativo de Android
)

object TtsVoiceCatalog {

    val SPANISH_VOICES = listOf(
        TtsVoice(
            code = "es_MX-claude-high",
            displayName = "Claude (Neutro)",
            language = "es",
            description = "Tono neutro alta fidelidad (Voz predeterminada)",
            nativeLocale = Locale("es", "MX")
        ),
        TtsVoice(
            code = "es_MX-ald-medium",
            displayName = "Aldo (México)",
            language = "es",
            description = "Masculina (México / Neutro)",
            nativeLocale = Locale("es", "MX")
        ),
        TtsVoice(
            code = "es_ES-carlfm-x_low",
            displayName = "Carl (España)",
            language = "es",
            description = "Voz ágil y ligera (España)",
            nativeLocale = Locale("es", "ES")
        ),
        TtsVoice(
            code = "es_ES-mls_10246-low",
            displayName = "Clásica (España)",
            language = "es",
            description = "Narrativa de audiolibro clásica (España)",
            nativeLocale = Locale("es", "ES")
        )
    )

    val ENGLISH_VOICES = listOf(
        TtsVoice(
            code = "en_US-joe-medium",
            displayName = "Joe (EE.UU.)",
            language = "en",
            description = "Masculina (Estados Unidos)",
            nativeLocale = Locale.US
        ),
        TtsVoice(
            code = "en_US-kristin-medium",
            displayName = "Kristin (EE.UU.)",
            language = "en",
            description = "Femenina (Estados Unidos)",
            nativeLocale = Locale.US
        ),
        TtsVoice(
            code = "en_GB-northern_english_male-medium",
            displayName = "Norteño (Reino Unido)",
            language = "en",
            description = "Masculina británica norteña",
            nativeLocale = Locale.UK
        ),
        TtsVoice(
            code = "en_GB-alba-medium",
            displayName = "Alba (Reino Unido)",
            language = "en",
            description = "Femenina británica / escocesa",
            nativeLocale = Locale.UK
        )
    )

    val ALL_VOICES = SPANISH_VOICES + ENGLISH_VOICES

    fun getVoicesForLanguage(lang: String): List<TtsVoice> {
        return if (lang.equals("en", ignoreCase = true)) ENGLISH_VOICES else SPANISH_VOICES
    }

    fun findVoice(code: String): TtsVoice? {
        val direct = ALL_VOICES.find { it.code.equals(code, ignoreCase = true) }
        if (direct != null) return direct

        // Mapeo inteligente para configuraciones previas (ej: "es-ES", "es-MX", "en-US")
        return when {
            code.contains("claude", true) || code.equals("es-MX", true) || code.equals("es-US", true) || code.equals("Latino", true) -> SPANISH_VOICES[0]
            code.contains("ald", true) || code.equals("es-AR", true) || code.equals("Argentina", true) -> SPANISH_VOICES[1]
            code.contains("carlfm", true) || code.equals("es-ES", true) || code.equals("España", true) -> SPANISH_VOICES[2]
            code.contains("mls", true) -> SPANISH_VOICES[3]
            code.contains("joe", true) || code.equals("en-US", true) || code.equals("EE.UU.", true) -> ENGLISH_VOICES[0]
            code.contains("kristin", true) -> ENGLISH_VOICES[1]
            code.contains("northern", true) || code.equals("en-GB", true) || code.equals("Reino Unido", true) -> ENGLISH_VOICES[2]
            code.contains("alba", true) || code.equals("en-AU", true) || code.equals("en-CA", true) || code.equals("Australia", true) || code.equals("Canadá", true) -> ENGLISH_VOICES[3]
            code.startsWith("en", true) -> ENGLISH_VOICES[0]
            else -> SPANISH_VOICES[0]
        }
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
        val currentVoice = findVoice(currentCode)
        val idx = list.indexOfFirst { it.code.equals(currentVoice?.code, ignoreCase = true) }
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
