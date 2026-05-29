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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

/**
 * One icon per tab — the SAME asset is used in both active and inactive
 * state; only the tint changes (with a soft cross-fade).
 *
 * For the Profile tab we don't render a glyph at all: the slot shows a
 * 26 dp circular avatar (Telegram photo when bound, otherwise a gradient
 * disc with the first letter of the user's name as fallback).
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

// === Geometry ===
// Adapted from the reference island-nav (52×44, padding 10/8). Pulled
// in for the 5-button row so the pills sit closer together: button
// 48×40, padding 8/6, container radius 26, pill radius 18. Bottom
// safe-area gap 14 dp (down from 20 dp) — bar sits a touch closer
// to the gesture edge.
private val ButtonWidth = 48.dp
private val ButtonHeight = 40.dp
private val NavGap = 4.dp
private val NavPadH = 8.dp
private val NavPadV = 6.dp
private val NavCornerRadius = 26.dp
private val PillCornerRadius = 18.dp

// Same easing curve as the reference island-nav for the pill motion
// and squash recovery — gentle ease-out, no overshoot, no bounce.
// r36: bumped 320 → 420 ms — pill glides a touch slower so the warm
// glow has time to register; still feels snappy on tap.
private val NavEasing = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)
private const val NavMoveMs = 420

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

    // Pill X — tween + soft ease-out, no overshoot, no bounce.
    val indicatorOffset by animateDpAsState(
        targetValue = (ButtonWidth + NavGap) * idx,
        animationSpec = tween(durationMillis = NavMoveMs, easing = NavEasing),
        label = "indicator",
    )

    // Pill squash on tab change: parabolic scaleX 1.0 → 1.12 → 1.0
    // over 320 ms. Reset to 0 on every change so a fast double-tap
    // still produces a fresh squash.
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

    val isDark = Wellness.colors.isDark
    // Frosted glass tint — kept INTENTIONALLY low so icons stay
    // crisply visible regardless of what's blurred behind. The
    // glass effect comes from the real RenderEffect blur (Android
    // 12+); on older devices we fall through to this tint as a
    // soft translucent overlay.
    //   dark theme  → black @ 0.18
    //   light theme → white @ 0.40
    val bgColor = if (isDark) Color.Black.copy(alpha = 0.18f)
                  else Color.White.copy(alpha = 0.40f)
    val hazeStyle = HazeStyle(
        tint = bgColor,
        blurRadius = 24.dp,
        noiseFactor = HazeDefaults.noiseFactor,
    )

    Box(
        modifier
            .padding(bottom = 14.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(NavCornerRadius),
                ambientColor = Color.Black,
                spotColor = Color.Black,
            )
            .clip(RoundedCornerShape(NavCornerRadius))
            // Background: when a haze source is wired, render the
            // blurred snapshot of whatever is drawn into that source
            // (the tab content under the navbar). The bar itself sits
            // outside the source wrapper so it doesn't appear in its
            // own blur. Without haze, fall back to the bare tint —
            // looks like a translucent surface, no crash.
            .let { base ->
                if (hazeState != null) {
                    base.hazeChild(
                        state = hazeState,
                        shape = RoundedCornerShape(NavCornerRadius),
                        style = hazeStyle,
                    )
                } else {
                    base.background(bgColor, RoundedCornerShape(NavCornerRadius))
                }
            }
            .padding(horizontal = NavPadH, vertical = NavPadV)
    ) {
        Box(
            Modifier
                .width(ButtonWidth * tabs.size + NavGap * (tabs.size - 1))
                .height(ButtonHeight)
        ) {
            // The indicator pill (r36: Island Pill v2).
            //  - Fill is a 160°-ish linear gradient accent → pink so the
            //    active slot reads warm and slightly two-tone, not flat.
            //  - A warm outer glow (shadow w/ accent spotColor) gives the
            //    pill a soft halo on dark theme — matches the prototype's
            //    `box-shadow: 0 0 24px rgba(255,140,90,.35)`.
            //  - A 1 dp white-alpha top sheen sells the "glass" highlight
            //    (inset 0 1px 0 rgba(255,255,255,.10)).
            //  - Squash-on-tap stays scaleX-only (parabola), so icons
            //    never get squished.
            val pillShape = RoundedCornerShape(PillCornerRadius)
            val accent = Wellness.colors.accent
            val pillBrush = Brush.linearGradient(
                colors = listOf(
                    accent.copy(alpha = 0.30f),
                    WellnessColors.TilePink.copy(alpha = 0.22f),
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
            )
            Box(
                Modifier
                    .offset(x = indicatorOffset, y = 0.dp)
                    .size(width = ButtonWidth, height = ButtonHeight)
                    .graphicsLayer {
                        scaleX = pillScaleX
                        scaleY = 1f
                    }
                    // Warm halo. spotColor + ambientColor are accent-tinted,
                    // so on dark theme this paints a soft orange cast around
                    // the pill (API 28+). Elevation kept low so it doesn't
                    // bleed onto neighbouring tabs.
                    .shadow(
                        elevation = 8.dp,
                        shape = pillShape,
                        ambientColor = accent,
                        spotColor = accent,
                    )
                    .background(pillBrush, pillShape)
                    .clip(pillShape)
            ) {
                // Top sheen — 1 dp tall, fades from white-alpha to transparent.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.10f),
                                0.08f to Color.Transparent,
                            ),
                            pillShape,
                        )
                )
            }

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
    // Icon tint cross-fades between accent and muted over 200 ms.
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
                SolarIcon(name = tabIcon(tab), tint = tint, size = 22.dp)
            }
        }
    }
}

/**
 * Profile slot: 26 dp circular avatar.
 *  - Gradient backdrop (accent → pink) + first letter of `userName`
 *    is painted first so the slot never flashes empty.
 *  - When a Telegram photo is bound, an `AsyncImage` crossfades on
 *    top of the backdrop.
 */
@Composable
private fun ProfileNavSlot() {
    val state = LocalAppState.current
    val context = LocalContext.current
    val photoUrl = state.telegramUser?.photoUrl
    val letter = state.userName.firstOrNull()?.uppercase() ?: "?"

    Box(
        Modifier
            .size(26.dp)
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
