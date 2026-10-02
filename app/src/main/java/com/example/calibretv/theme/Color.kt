package com.example.calibretv.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// 🎨 CalibroTV v3.0 — Editorial Luxury Design System: "Noble Ink & Gold"
// As specified in DESIGN.md for OLED-Safe 10-Foot TV UI
// ============================================================================

// Superficies y Fondos (OLED Safe)
val SurfaceBase = Color(0xFF0C0A09)       // Obsidian Ink: Fondo base absoluto
val SurfaceCard = Color(0xFF181513)       // Binding Board: Superficie de tarjeta elevada
val SurfaceRaised = Color(0xFF181513)     // Alias retrocompatible para tarjetas elevadas
val SurfaceFocused = Color(0xFF221E1B)    // Pressed Linen: Superficie con foco D-Pad activa
val SurfaceContainer = Color(0xFF2D2925)  // Elevated Dock: Acrílico translúcido del menú HUD
val SurfaceContainerHigh = Color(0xFF38332E)
val SurfaceContainerHighest = Color(0xFF453F39)
val BackgroundDark = SurfaceBase          // Fondo base unificado

// Acentos Nobles (Pan de Oro y Cuero)
val AccentGold = Color(0xFFC5A059)        // Gold Foil: Foco D-Pad principal y botones de acción
val BrightGold = Color(0xFFD4AF37)        // Bright Gold: Estrellas de valoración y sellos
val StarGold = BrightGold
val DeepGoldTint = Color(0xFF423419)      // Fondo atenuado de selección
val MoroccanLeather = Color(0xFF4A1E22)   // Cuero granate para insignias especiales

// Compatibilidad con código existente: mapear al oro noble
val PrimaryGold = AccentGold
val AmberWarm = AccentGold
val CyanElectric = Color(0xFF38BDF8)

// Tipografía Editorial (Contraste WCAG AAA)
val AntiqueIvory = Color(0xFFF7F4EE)      // Antique Ivory: Títulos y texto de lectura
val TextPrimary = AntiqueIvory
val TextSecondary = Color(0xFFD4CFC6)     // Soft Parchment: Subtítulos y nombres de autor
val TextMuted = Color(0xFFA8A29E)         // Warm Linen: Sinopsis y metadata
val TextVariant = TextSecondary

// Paletas del Modo de Lectura (Dual-Spread 16:9)
// 1. Pergamino Clásico (Día / Pared)
val ParchmentBackground = Color(0xFFF4EFE6)
val ParchmentText = Color(0xFF2B2623)

// 2. Sepia Cine (Atmósfera cinematográfica)
val SepiaBackground = Color(0xFF1F1914)
val SepiaText = Color(0xFFE0D2C1)

// 3. OLED Ink (Noche / Dormitorio - Cero luz parásita)
val OledBackground = Color(0xFF000000)
val OledText = Color(0xFFE5E0D8)
val NightBackground = Color(0xFF0C0A09)
val NightText = Color(0xFFC5A059)
