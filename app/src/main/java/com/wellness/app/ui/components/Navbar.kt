package com.wellness.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.Tab
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors

/**
 * One icon per tab — the SAME asset is used in both active and inactive
 * state; only the tint changes (with a soft cross-fade).
 *
 * For the Profile tab we don't render a glyph at all: the slot shows a
 * 28 dp circular avatar (Telegram photo when bound, otherwise a gradient
 * disc with the first letter of the user's name as fallback) — same
 * pattern as the reference island-nav.
 */
fun tabIcon(tab: Tab): String = when (tab) {
    Tab.Home -> "home-2-bold-duotone"
    Tab.Nutrition -> "fire-bold-duotone"
    Tab.Plan -> "calendar-bold-duotone"
    Tab.Trackers -> "chart-2-bold-duotone"
    Tab.Profile -> "user-bold-duotone"
}

/** Human-readable name for every supported tab. */
fun tabTitle(tab: Tab): String = when (tab) {
    Tab.Home -> "Главная"
    Tab.Nutrition -> "Питание"
    Tab.Plan -> "План"
    Tab.Trackers -> "Трекеры"
    Tab.Profile -> "Профиль"
}

// === Geometry copied 1:1 from the reference island-nav ===
// Buttons: 52 × 44, rounded 20. Gap 4. Container padding 10/8, radius 28.
// Pill move: 320 ms with Cubic(.32, .72, .00, 1).
// Pill squash on tap: parabolic scaleX 1.0 → 1.12 → 1.0 over 320 ms
// (Y stays 1 — icons themselves do NOT move/scale, per user feedback).
private val ButtonWidth = 52.dp
private val ButtonHeight = 44.dp
private val NavGap = 4.dp
private val NavPadH = 10.dp
private val NavPadV = 8.dp
private val NavCornerRadius = 28.dp
private val PillCornerRadius = 20.dp

// Same easing curve as the reference island-nav for both pill motion
// and squash recovery — gentle ease-out, no overshoot, no bounce.
private val NavEasing = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)
private const val NavMoveMs = 320

@Composable
fun Navbar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val state = LocalAppState.current
    val tabs = state.navbarOrder
    val idx = tabs.indexOf(current).coerceAtLeast(0)

    // Pill X — animates with the same tween + curve as the reference.
    // Spring is intentionally NOT used here: the user said the springy
    // motion "moves horribly"; the reference glides linearly with a
    // soft ease-out and no overshoot.
    val indicatorOffset by animateDpAsState(
        targetValue = (ButtonWidth + NavGap) * idx,
        animationSpec = tween(durationMillis = NavMoveMs, easing = NavEasing),
        label = "indicator",
    )

    // Pill squash on tab change: parabolic scaleX 1.0 → 1.12 → 1.0 over
    // 320 ms. Reset to 0 on every change so a fast double-tap still
    // produces a fresh squash.
    val squash = remember { Animatable(0f) }
    LaunchedEffect(idx) {
        squash.snapTo(0f)
        squash.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = NavMoveMs, easing = LinearEasing),
        )
    }
    // 0 → 0.5 → 1 maps to scaleX 1.0 → 1.12 → 1.0 (parabola peaked at .5)
    val pillScaleX = 1f + 0.12f * (1f - kotlin.math.abs(2f * squash.value - 1f))

    // Same hue as r32 (dark = container, light = white), alpha lowered
    // so the frosted-glass blur underneath can read through. No theme
    // colour change, no shadow — per user spec.
    val bg = if (Wellness.colors.isDark) {
        Wellness.colors.container.copy(alpha = 0.62f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
    // Light, smooth blur: 16 dp RenderEffect, no noise, static tint —
    // no animated parameter on the blur itself so nothing can "jitter".
    val hazeStyle = HazeStyle(
        tint = bg,
        blurRadius = 16.dp,
        noiseFactor = 0f,
    )

    Box(
        modifier
            // Bottom safe-area gap — matches the 20 px the reference
            // shell leaves between the island and the bottom of the
            // device. Keeps the nav floating, not glued to the edge.
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(NavCornerRadius))
            // Real frosted-glass blur when a haze source is wired
            // (Android 12+ uses RenderEffect, GPU-continuous, no
            // per-frame snapshotting → smooth). Without haze, fall
            // through to a plain translucent tint — same hue, no crash.
            .let { base ->
                if (hazeState != null) {
                    base.hazeChild(
                        state = hazeState,
                        shape = RoundedCornerShape(NavCornerRadius),
                        style = hazeStyle,
                    )
                } else {
                    base.background(bg, RoundedCornerShape(NavCornerRadius))
                }
            }
            .padding(horizontal = NavPadH, vertical = NavPadV)
    ) {
        Box(
            Modifier
                .width(ButtonWidth * tabs.size + NavGap * (tabs.size - 1))
                .height(ButtonHeight)
        ) {
            // The indicator pill — squashes on tap, then settles to a
            // calm rounded rect under the active slot.
            Box(
                Modifier
                    .offset(x = indicatorOffset, y = 0.dp)
                    .size(width = ButtonWidth, height = ButtonHeight)
                    .graphicsLayer {
                        scaleX = pillScaleX
                        // Reference squashes ONLY scaleX. The icons
                        // sitting on top don't move at all.
                        scaleY = 1f
                    }
                    .background(
                        Wellness.colors.accent.copy(alpha = 0.18f),
                        RoundedCornerShape(PillCornerRadius),
                    )
            )

            Row(Modifier.fillMaxSize()) {
                tabs.forEachIndexed { i, tab ->
                    if (i > 0) Spacer(Modifier.width(NavGap))
                    NavItem(
                        tab = tab,
                        active = current == tab,
                        onClick = { onSelect(tab) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(tab: Tab, active: Boolean, onClick: () -> Unit) {
    // Icon tint cross-fades between accent and muted over 200 ms —
    // same as the reference's AnimatedSwitcher between two const
    // colored icons, but cheaper: a single tint animation, no swap.
    val tint by animateColorAsState(
        targetValue = if (active) Wellness.colors.accent else Wellness.colors.muted,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "navTint",
    )

    NoFeedbackButton(
        onClick = onClick,
        modifier = Modifier.size(width = ButtonWidth, height = ButtonHeight),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (tab == Tab.Profile) {
                ProfileNavSlot()
            } else {
                // No scale, no lift — icons stay perfectly still
                // (only the pill underneath moves and squashes).
                SolarIcon(name = tabIcon(tab), tint = tint, size = 24.dp)
            }
        }
    }
}

/**
 * Profile slot: 28 dp circular avatar.
 *  - Gradient backdrop (accent → pink) + first letter of `userName`
 *    is painted first so the slot never flashes empty.
 *  - When a Telegram photo is bound, an `AsyncImage` crossfades on
 *    top of the backdrop.
 *
 * No active-state scale — matches the reference, where the avatar
 * stays the same size whether selected or not (the pill behind it
 * is the only thing that changes).
 */
@Composable
private fun ProfileNavSlot() {
    val state = LocalAppState.current
    val context = LocalContext.current
    val photoUrl = state.telegramUser?.photoUrl
    val letter = state.userName.firstOrNull()?.uppercase() ?: "?"

    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Wellness.colors.accent, WellnessColors.TilePink),
                ),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        if (photoUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoUrl)
                    .crossfade(260)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
