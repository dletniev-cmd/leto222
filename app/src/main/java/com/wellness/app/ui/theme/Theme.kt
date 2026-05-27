package com.wellness.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class WellnessColorScheme(
    val bg: Color,
    val container: Color,
    val text: Color,
    val muted: Color,
    val track: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val isDark: Boolean,
)

val LocalWellnessColors = staticCompositionLocalOf<WellnessColorScheme> {
    error("WellnessColors not provided")
}

object Wellness {
    val colors: WellnessColorScheme
        @Composable
        get() = LocalWellnessColors.current
    val typography
        @Composable
        get() = MaterialTheme.typography
    val shapes = WellnessShapes
}

private fun darkScheme(accent: Color) = WellnessColorScheme(
    bg = WellnessColors.DarkBg,
    container = WellnessColors.DarkContainer,
    text = WellnessColors.DarkText,
    muted = WellnessColors.MutedDark,
    track = WellnessColors.TrackDark,
    accent = accent,
    accentSoft = accent.copy(alpha = 0.16f),
    onAccent = Color(0xFF0C1F12),
    isDark = true,
)

private fun lightScheme(accent: Color) = WellnessColorScheme(
    bg = WellnessColors.LightBg,
    container = WellnessColors.LightContainer,
    text = WellnessColors.LightText,
    muted = WellnessColors.MutedLight,
    track = WellnessColors.TrackLight,
    accent = accent,
    accentSoft = accent.copy(alpha = 0.16f),
    onAccent = Color(0xFF0C1F12),
    isDark = false,
)

enum class ThemeMode { Light, Dark, System }

@Composable
fun WellnessTheme(
    mode: ThemeMode = ThemeMode.Dark,
    accent: Color = WellnessColors.Mint,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> systemDark
    }
    val wColors = if (dark) darkScheme(accent) else lightScheme(accent)

    val material = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF0C1F12),
            background = wColors.bg,
            surface = wColors.container,
            onBackground = wColors.text,
            onSurface = wColors.text,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color(0xFF0C1F12),
            background = wColors.bg,
            surface = wColors.container,
            onBackground = wColors.text,
            onSurface = wColors.text,
        )
    }

    CompositionLocalProvider(LocalWellnessColors provides wColors) {
        MaterialTheme(
            colorScheme = material,
            typography = WellnessTypography,
            content = content,
        )
    }
}
