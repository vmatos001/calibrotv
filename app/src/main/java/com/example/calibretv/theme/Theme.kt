package com.example.calibretv.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.lightColorScheme

private val TvDarkColorScheme = darkColorScheme(
    primary = AmberWarm,
    onPrimary = Color.Black,
    primaryContainer = SurfaceContainerHigh,
    onPrimaryContainer = PrimaryGold,
    secondary = CyanElectric,
    onSecondary = Color.Black,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceBase,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = TextMuted,
)

private val TvLightColorScheme = lightColorScheme(
    primary = InkPrimary,
    onPrimary = Color.White,
    primaryContainer = CardBackgroundLight,
    onPrimaryContainer = InkPrimary,
    secondary = AccentLime,
    onSecondary = InkPrimary,
    background = CanvasBackgroundLight,
    onBackground = InkPrimary,
    surface = CardBackgroundLight,
    onSurface = InkPrimary,
    surfaceVariant = CardBackgroundLightSoft,
    onSurfaceVariant = InkSecondary,
)

@Composable
fun CalibreTVTheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) TvDarkColorScheme else TvLightColorScheme,
        typography = Typography,
        content = content
    )
}
