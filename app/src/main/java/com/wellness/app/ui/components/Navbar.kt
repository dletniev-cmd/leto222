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

// === Geometry ===
// Adapted from the reference island-nav (52×44, padding 10/8). The
// reference targets a 3-button bar; our 5-button layout overflowed the
// screen visually, so the dimensions are pulled in slightly so the
// pills sit closer together without losing finger-target size: button
// 48×40 (still ≥44 dp tappable thanks to the row's 8 dp vertical
// chrome on top + bottom), padding 8/6, container radius 26, pill
// radius 18. Same motion physics as before — only sizes shrank.
private val ButtonWidth = 48.dp
private val ButtonHeight = 40.dp
private val NavGap = 4.dp
private val NavPadH = 8.dp
private val NavPadV = 6.dp
private val NavCornerRadius = 26.dp
private val PillCornerRadius = 18.dp

// Same easing curve as the reference island-nav for the pill motion
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

    val isDark = Wellness.colors.isDark
    // Tint sitting on top of the blurred snapshot. We want the bar to
    // read as glass — semi-transparent, takes its mood from whatever is
    // behind it — but still legible. Alpha ~.55 dark / ~.7 light is the
    // sweet spot (any lower and the icons stop reading on a bright
    // screen-behind, any higher and the blur stops being visible).
    val tint = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.55f)
               else Color.White.copy(alpha = 0.70f)
    val hazeStyle = HazeStyle(
        tint = tint,
        blurRadius = 24.dp,
        noiseFactor = HazeDefaults.noiseFactor,
    )
    // Fallback when haze isn't wired (e.g. previews) — keep the bar
    // legible but obviously not blurred. 95% alpha matches the
    // pre-blur version of the navbar so screenshots still look right.
    val solidBg = if (isDark) Wellness.colors.container.copy(alpha = 0.95f)
                  else Color.White.copy(alpha = 0.95f)

    Box(
        modifier
            // Bottom safe-area gap — pulled in from the reference's
            // 20 dp to 14 dp so the bar sits a touch closer to the
            // edge of the screen (user feedback: it felt floating
            // too high above the gesture bar).
            .padding(bottom = 14.dp)
            // Drop shadow that survives the glass effect. Modifier
            // order matters here — shadow has to be applied BEFORE
            // the haze child so it draws under the blurred area.
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(NavCornerRadius),
                ambientColor = Color.Black,
                spotColor = Color.Black,
            )
            .clip(RoundedCornerShape(NavCornerRadius))
            .let { base ->
                if (hazeState != null) {
                    // Real frosted-glass blur: haze captures the
                    // content drawn into `hazeState.haze(...)` (the
                    // tab area sitting under the navbar) and renders
                    // it through a RenderEffect blur, clipped to the
                    // navbar's rounded-rect shape. On API < 31 the
                    // library falls back to a flat translucent tint
                    // automatically — no crashes, no missing visuals.
                    base.hazeChild(
                        state = hazeState,
                        shape = RoundedCornerShape(NavCornerRadius),
                        style = hazeStyle,
                    )
                } else {
                    base.background(solidBg, RoundedCornerShape(NavCornerRadius))
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
                        Wellness.colors.accent.copy(alpha = 0.22f),
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
 *
 * No active-state scale — matches the reference, where the avatar
 * stays the same size whether selected or not.
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
