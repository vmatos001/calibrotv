package com.example.calibretv.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 🎨 CalibroTV — Paleta Clásica y de Alto Contraste (OLED Safe & 10-Foot UI)
// Conserva el querido Ámbar Cálido (#FFA000) y Negros Profundos OLED de CalibroTV
// ============================================================================

// Superficies y Fondos (OLED Safe)
val SurfaceBase = Color(0xFF0A0A0A)       // Negro Profundo OLED
val SurfaceCard = Color(0xFF161616)       // Tarjeta oscura neutra
val SurfaceRaised = Color(0xFF1E1E1E)     // Superficie elevada
val SurfaceFocused = Color(0xFF282828)    // Foco D-Pad activo
val SurfaceContainer = Color(0xFF1E1E1E)  // Menú y dock translúcido
val SurfaceContainerHigh = Color(0xFF2D2D2D)
val SurfaceContainerHighest = Color(0xFF383838)
val BackgroundDark = SurfaceBase          // Fondo base unificado

// Acentos Clásicos de CalibroTV (Ámbar Cálido y Oro Vibrante)
val AmberWarm = Color(0xFFFFA000)        // Ámbar icónico de CalibroTV
val AccentGold = AmberWarm               // Halo D-Pad y botones principales
val BrightGold = Color(0xFFFFB300)       // Estrellas y sellos dorados
val StarGold = BrightGold
val DeepGoldTint = Color(0xFF3E2800)     // Fondo atenuado de selección
val MoroccanLeather = Color(0xFF5A1E22)  // Cuero granate para insignias

// Compatibilidad
val PrimaryGold = AccentGold
val CyanElectric = Color(0xFF00E5FF)     // Acento secundario cian de alto contraste

// Tipografía Editorial (Contraste WCAG AAA)
val AntiqueIvory = Color(0xFFFBFBFB)     // Blanco cálido suave
val TextPrimary = AntiqueIvory
val TextSecondary = Color(0xFFD0D0D0)    // Gris claro legible a 3 metros
val TextMuted = Color(0xFFA0A0A0)        // Metadatos y sinopsis
val TextVariant = TextSecondary

// Paletas del Modo de Lectura (Dual-Spread 16:9)
// 1. Pergamino Clásico (Día / Pared)
val ParchmentBackground = Color(0xFFF5EFE6)
val ParchmentText = Color(0xFF231F1C)

// 2. Sepia Cine (Atmósfera cinematográfica)
val SepiaBackground = Color(0xFF1C1712)
val SepiaText = Color(0xFFE8DBC9)

// 3. OLED Ink (Noche / Dormitorio - Cero luz parásita)
val OledBackground = Color(0xFF000000)
val OledText = Color(0xFFE5E0D8)
val NightBackground = Color(0xFF0A0A0A)
val NightText = Color(0xFFFFA000)

// ============================================================================
// 🎨 Light Theme Edition (Design.md — Bento Grid & Editorial Day Mode)
// ============================================================================
val CanvasBackgroundLight = Color(0xFFEAEFEA)      // Fondo suave papel mate antirreflejo TV
val CardBackgroundLight = Color(0xFFFFFFFF)        // Blanco puro para tarjetas Bento
val CardBackgroundLightSoft = Color(0xFFF6F8F6)    // Blanco secundario
val SidebarBackgroundLight = Color(0xFF141619)     // Rail carbón oscuro (en Light mode)
val SidebarBackgroundDarkInv = Color(0xFFFFFFFF)   // Rail blanco puro (en Dark mode invertido)

val InkPrimary = Color(0xFF111317)            // Texto principal / botones oscuros
val InkSecondary = Color(0xFF636670)          // Textos secundarios
val InkMuted = Color(0xFF9DA1AA)              // Bordes e iconos suaves

val AccentLime = Color(0xFFE6FA53)            // Acento dinámico Cyber Lime
val AccentLimeDark = Color(0xFFC7DC37)        // Hover/Pressed lima
val AccentLimeText = Color(0xFF111317)        // Texto sobre fondo lima
val PillInactiveLight = Color(0xFFE2E7E2)     // Cápsulas en reposo
val FocusBorderDark = Color(0xFF111317)       // Borde de foco oscuro

