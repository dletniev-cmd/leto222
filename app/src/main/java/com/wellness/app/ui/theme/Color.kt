package com.wellness.app.ui.theme

import androidx.compose.ui.graphics.Color

object WellnessColors {
    // Backgrounds per spec
    val DarkBg = Color(0xFF000000)
    val DarkContainer = Color(0xFF181818)
    val LightBg = Color(0xFFF1F1F3)
    val LightContainer = Color(0xFFFFFFFF)

    // Text
    val DarkText = Color(0xFFF4F4F5)
    val LightText = Color(0xFF18181B)
    val MutedDark = Color(0xFF8A8A92)
    // Darker muted in light theme — the previous #8A8A92 sat too low in
    // contrast on a white surface and made captions / chevrons look
    // washed out. #6B6B73 reads as a confident secondary tone while
    // staying clearly subordinate to the body text #18181B.
    val MutedLight = Color(0xFF6B6B73)

    // Tracks (ring background / progress track)
    val TrackDark = Color(0x14FFFFFF)
    val TrackLight = Color(0x14000000)

    // Semantic palette (theme-independent)
    val Water = Color(0xFF5AA7FF)
    val Cal = Color(0xFFFF8C5A)
    val Protein = Color(0xFFFF6B9D)
    val Fat = Color(0xFFFFD166)
    val Carb = Color(0xFFB084F5)
    val Purple = Color(0xFFB084F5)
    val Orange = Color(0xFFFF8C5A)
    val Pink = Color(0xFFFF6B9D)
    val Mint = Color(0xFF7CD992)
    val Yellow = Color(0xFFFFD166)

    // Telegram-style settings tile colours — saturated, theme-independent
    // squares with a white icon centred inside. Match the screenshot the
    // user shared.
    val TileBlue = Color(0xFF3FA8F5)
    val TileOrange = Color(0xFFFFA63D)
    val TileGreen = Color(0xFF4ECB71)
    val TileRed = Color(0xFFF26A5C)
    val TileCyan = Color(0xFF45C8E8)
    val TileSky = Color(0xFF5BB7FF)
    val TileViolet = Color(0xFFB286FF)
    val TileTeal = Color(0xFF3DBFA8)
    val TilePink = Color(0xFFFF6E97)
    val TileLemon = Color(0xFFEEC53A)

    // Brand blue used as the Telegram tile fill — close to the gradient
    // mid-tone of the official Telegram round-logo so the binding entry
    // reads as "Telegram" at a glance.
    val TelegramBlue = Color(0xFF2AABEE)
    val TelegramBlueDeep = Color(0xFF229ED9)
}

/**
 * 18 accent colours arranged in a smooth hue circle (violet → blue → green
 * → yellow → red → pink). Six per row, three rows. Designed to read
 * pleasantly on both light and dark backgrounds.
 */
val AccentPalette: List<Color> = listOf(
    // Row 1 — vivid warm / sun / earth
    Color(0xFFFF453A), // ruby red
    Color(0xFFFF6F61), // coral
    Color(0xFFFF9F0A), // tangerine
    Color(0xFFFFC940), // honey
    Color(0xFFEFD075), // sand
    Color(0xFFA9805A), // mocha
    // Row 2 — fresh green / cool teal / sky
    Color(0xFFB7DD5C), // lime
    Color(0xFF34C759), // grass green
    Color(0xFF30D1B0), // mint
    Color(0xFF5AC8FA), // sky
    Color(0xFF0A84FF), // ocean blue
    Color(0xFF5E5CE6), // indigo
    // Row 3 — purple / pink / berry / graphite
    Color(0xFFAF52DE), // violet
    Color(0xFFD583FF), // lavender
    Color(0xFFFF6FAA), // bubblegum
    Color(0xFFFF2D55), // rose
    Color(0xFF8E8E93), // graphite
    Color(0xFFB8BCC4), // mist grey
)

/**
 * Accent picker shown on the *Appearance → Theme* screen. This is the
 * one swatch that tints the entire app (buttons, switches, ring "fill"
 * colour), so we keep the list short and very saturated — vibrant iOS
 * system tints. Habit / task colours get their own broader palette
 * above so the picker on AddHabit stays rich while the theme picker
 * stays focused.
 */
val ThemePalette: List<Color> = listOf(
    Color(0xFF9EE493), // signature lime (default)
    Color(0xFF34C759), // green
    Color(0xFF30D1B0), // mint
    Color(0xFF5AC8FA), // sky
    Color(0xFF0A84FF), // blue
    Color(0xFF5E5CE6), // indigo
    Color(0xFFAF52DE), // violet
    Color(0xFFFF2D55), // rose
    Color(0xFFFF9F0A), // orange
    Color(0xFFFFCC00), // sunny
)
