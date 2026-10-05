package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DevilRed,
    onPrimary = Color.White,
    primaryContainer = DevilRedDark,
    onPrimaryContainer = Color.White,
    secondary = DevilCyan,
    onSecondary = ObsidianDark,
    secondaryContainer = DevilCyanDim,
    onSecondaryContainer = Color.White,
    tertiary = DevilAmber,
    background = ObsidianDark,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = DevilRedDark,
    onPrimary = Color.White,
    primaryContainer = DevilRedLight,
    onPrimaryContainer = Color.White,
    secondary = DevilCyanDim,
    onSecondary = Color.White,
    secondaryContainer = DevilCyan,
    onSecondaryContainer = ObsidianDark,
    tertiary = DevilAmber,
    background = CanvasLight,
    onBackground = TextPrimaryLight,
    surface = CardSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderOutlineLight
)

@Composable
fun DevilAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
