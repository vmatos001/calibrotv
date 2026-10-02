package com.example.calibretv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String? = null,
    val epubUrl: String? = null,
    val summary: String = "",
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val shelves: List<String> = emptyList(),
    val progressPercent: Int = 0,
    val lastReadSpread: Int = 0
)

@Serializable
data class CalibreShelf(
    val id: String,
    val name: String,
    val bookIds: List<String> = emptyList(),
    val isCharacterShelf: Boolean = false,
    val hasImage: Boolean = false,
    val imageUrl: String? = null
)

@Serializable
data class OpdsCategory(
    val id: String,
    val title: String,
    val feedUrl: String,
    val count: Int = 0
)

@Serializable
enum class ReadingTheme {
    PERGAMINO,   // #F4F1EA fondo papel clásico, #2C2A29 texto editorial (Estilo Apple Books)
    OLED_PURE,   // #000000 negro absoluto, #E5E1E4 texto nítido
    SEPIA_CINE,  // #26201A fondo cálido cinematográfico, #E6DBCC texto
    NIGHT_AMBER, // #0D0D0D fondo noche, #FFC664 / #C29B38 texto ámbar (cero luz azul)
    PROYECTOR_BLANCO, // Fondo blanco puro para proyectores con fondo claro
    CINE_OSCURO       // Negro absoluto + texto ámbar muy tenue para sala oscura
}

@Serializable
enum class ReadingFont {
    SERIF_SYSTEM,     // Fuente serif del sistema (actual)
    OPEN_DYSLEXIC,    // Para personas con dislexia (requiere asset)
    GEORGIA_LIKE,     // Georgia/serif elegante
    MONOSPACE,        // Ancho fijo para código/poesía
}

@Serializable
enum class CurlSpeed {
    APPLE_BOOKS_SMOOTH, // 500ms física suave de hoja de papel
    FLUID               // 320ms paso fluido
}

@Serializable
enum class AmbientSound {
    NONE,       // Sin sonido
    RAIN,       // Lluvia suave
    FIREPLACE,  // Chimenea crepitando
    OCEAN,      // Olas del mar
    CAFE,       // Cafetería con murmullos
    FOREST      // Bosque / naturaleza
}

@Serializable
data class ReadingSettings(
    val verticalMirror: Boolean = false,
    val rotation180: Boolean = false,
    val ceilingMode: Boolean = false,
    val curlSpeed: CurlSpeed = CurlSpeed.APPLE_BOOKS_SMOOTH,
    val theme: ReadingTheme = ReadingTheme.PERGAMINO,
    val fontSizeSp: Int = 20,
    val overscanPercent: Int = 0,
    val sleepTimerMinutes: Int = 0, // 0 = desactivado, 15, 30, 45, 60
    val readerBrightness: Float = 1.0f, // 0.1f a 1.0f, control de brillo interno
    val readingFont: ReadingFont = ReadingFont.SERIF_SYSTEM,
    val spineDepth3D: Float = 0.5f, // 0.0 = plano, 1.0 = tomo grueso
    val ttsEnabled: Boolean = false,
    val ttsSpeedRate: Float = 1.0f, // 0.5 = lento, 1.0 = normal, 1.5 = rápido
    val ttsPitch: Float = 1.0f, // 0.8 = grave, 1.0 = normal, 1.2 = agudo
    val ttsVoiceLocale: String = "es-ES", // "es-ES", "es-MX", "es-US"
    val pageSoundEnabled: Boolean = true,
    val ambientSound: AmbientSound = AmbientSound.NONE,
    val ambientVolume: Float = 0.4f // 0.0f a 1.0f
)

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val avatarColorHex: String,
    val isKidsMode: Boolean = false,
    val parentalPin: String? = null,
    val starsCount: Int = 0,
    val whitelistBookIds: List<String> = emptyList()
)

@Serializable
data class ServerConfig(
    val serverUrl: String = "http://192.168.1.50:8083/opds",
    val username: String = "",
    val password: String = ""
)
