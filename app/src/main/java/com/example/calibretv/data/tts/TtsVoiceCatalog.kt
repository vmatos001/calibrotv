package com.example.calibretv.data.tts

import java.util.Locale

/**
 * Catálogo Canónico de Parejas de Voces Neuronales de Microsoft Edge TTS para CalibroTV.
 *
 * Parejas de Voces por Idioma del Libro:
 * - Español (México):
 *     1. Dalia (Femenina) [es-MX-DaliaNeural] - Predeterminada
 *     2. Jorge (Masculino) [es-MX-JorgeNeural]
 * - Inglés (EE.UU.):
 *     1. Ava (Femenina) [en-US-AvaNeural] - Predeterminada (Estilo Jane: joven, cálida y amigable)
 *     2. Andrew (Masculino) [en-US-AndrewMultilingualNeural] - (Estilo Samuel: sincero, cálido y multilingüe)
 */
data class TtsVoice(
    val code: String,        // Identificador de Edge TTS (ej: "es-MX-DaliaNeural")
    val displayName: String, // Nombre para el HUD del TV (ej: "Dalia (Femenina)")
    val language: String,    // "es" o "en"
    val description: String, // Descripción del timbre
    val isFemale: Boolean,   // Femenina o Masculina
    val nativeLocale: Locale // Locale de fallback
)

object TtsVoiceCatalog {

    // --- PAREJA EN ESPAÑOL (México) ---
    val DALIA = TtsVoice(
        code = "es-MX-DaliaNeural",
        displayName = "Dalia (Femenina)",
        language = "es",
        description = "Femenina brillante, alegre y expresiva para narración (México)",
        isFemale = true,
        nativeLocale = Locale.forLanguageTag("es-MX")
    )

    val JORGE = TtsVoice(
        code = "es-MX-JorgeNeural",
        displayName = "Jorge (Masculino)",
        language = "es",
        description = "Masculina profunda, segura y con autoridad para narración (México)",
        isFemale = false,
        nativeLocale = Locale.forLanguageTag("es-MX")
    )

    // --- PAREJA EN INGLÉS (EE.UU.) ---
    val AVA = TtsVoice(
        code = "en-US-AvaNeural",
        displayName = "Ava (Femenina)",
        language = "en",
        description = "Femenina joven, cálida y amigable para audiolibros (EE.UU.)",
        isFemale = true,
        nativeLocale = Locale.US
    )

    val ANDREW = TtsVoice(
        code = "en-US-AndrewMultilingualNeural",
        displayName = "Andrew (Masculino)",
        language = "en",
        description = "Masculina sincera, cálida y narrativa (EE.UU. Multilingüe)",
        isFemale = false,
        nativeLocale = Locale.US
    )

    val SPANISH_VOICE = DALIA
    val ENGLISH_VOICE = AVA

    val SPANISH_VOICES = listOf(DALIA, JORGE)
    val ENGLISH_VOICES = listOf(AVA, ANDREW)
    val ALL_VOICES = SPANISH_VOICES + ENGLISH_VOICES

    fun getVoicesForLanguage(lang: String): List<TtsVoice> {
        return if (lang.equals("en", ignoreCase = true)) ENGLISH_VOICES else SPANISH_VOICES
    }

    fun findVoice(code: String, lang: String = "es"): TtsVoice {
        val list = getVoicesForLanguage(lang)
        return list.find { it.code.equals(code, ignoreCase = true) }
            ?: ALL_VOICES.find { it.code.equals(code, ignoreCase = true) }
            ?: if (lang.equals("en", ignoreCase = true)) ENGLISH_VOICE else SPANISH_VOICE
    }

    fun getVoiceDisplayName(code: String, lang: String): String {
        return findVoice(code, lang).displayName
    }

    fun getNextVoice(currentCode: String, lang: String): TtsVoice {
        val list = getVoicesForLanguage(lang)
        val currentIndex = list.indexOfFirst { it.code.equals(currentCode, ignoreCase = true) }
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % list.size else 0
        return list[nextIndex]
    }

    fun ensureValidVoiceCode(code: String, lang: String): String {
        val list = getVoicesForLanguage(lang)
        return list.firstOrNull { it.code.equals(code, ignoreCase = true) }?.code ?: list.first().code
    }
}
