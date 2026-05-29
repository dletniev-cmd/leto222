package com.wellness.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.Tab
import com.wellness.app.ui.theme.Wellness

/**
 * One icon per tab — the SAME asset is used in both active and inactive
 * state. Active vs inactive is communicated purely through tint, never
 * through an icon swap, so there is no decode "flicker" when the user
 * jumps between tabs.
 *
 * For the Profile tab specifically, when the user has bound their
 * Telegram account and we have a real avatar URL, the navbar shows a
 * circular thumbnail of that photo INSTEAD of the user glyph — the
 * thumbnail is treated as the icon (same size slot, same active-state
 * scale animation). When no photo is available we fall back to the
 * user-bold-duotone glyph so the slot never goes empty.
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

// Geometry: the outer bar is a calm rounded rectangle (NOT a full pill) so
// the navbar belongs to the same visual family as the rest of the app's
// cards and tiles. The indicator under the active tab stays an iOS-style
// round pill, the way the user explicitly asked: it's a 44×44 square clipped
// with corner-radius 999, so it reads as a smooth circle even while the
// outer container is mildly rounded.
private val NavbarCornerRadius = 26.dp

@Composable
fun Navbar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val state = LocalAppState.current
    val itemSize = 44.dp
    val gap = 4.dp
    val padding = 6.dp
    val tabs = state.navbarOrder
    val idx = tabs.indexOf(current).coerceAtLeast(0)

    // Same gentle spring as the original — the indicator should feel like
    // it's being tossed under the finger and softly catching, NOT like it's
    // teleporting. dampingRatio 0.7 + StiffnessMediumLow matches the iOS
    // tab-bar pill behaviour the user asked for.
    val indicatorOffset by animateDpAsState(
        targetValue = padding + (itemSize + gap) * idx,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "indicator",
    )

    Box(
        modifier
            .padding(bottom = 18.dp)
            .background(
                color = Wellness.colors.container.copy(alpha = 0.94f),
                shape = RoundedCornerShape(NavbarCornerRadius)
            )
            .padding(padding)
    ) {
        // The indicator pill — round, iOS-style, sits under the active icon.
        Box(
            Modifier
                .offset(x = indicatorOffset - padding, y = 0.dp)
                .width(itemSize)
                .height(itemSize)
                .background(Wellness.colors.accentSoft, RoundedCornerShape(999.dp))
        )
        Row {
            tabs.forEachIndexed { i, tab ->
                if (i > 0) Box(Modifier.width(gap))
                NavItem(
                    tab = tab,
                    active = current == tab,
                    onClick = { onSelect(tab) },
                    size = itemSize,
                )
            }
        }
    }
}

@Composable
private fun NavItem(tab: Tab, active: Boolean, onClick: () -> Unit, size: Dp) {
    val state = LocalAppState.current
    val tint = if (active) Wellness.colors.accent else Wellness.colors.muted

    // Active-state lift: the icon (or avatar) grows ×1.12 with a soft
    // spring when the tab becomes active, then settles. Same physical
    // feel as the indicator pill so the two transitions land at the
    // same moment. Scale is applied via graphicsLayer to avoid any
    // layout reflow inside the row.
    val scale by animateFloatAsState(
        targetValue = if (active) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "navItemScale",
    )

    NoFeedbackButton(onClick = onClick, modifier = Modifier.size(size)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (tab == Tab.Profile) {
                ProfileNavSlot(
                    tint = tint,
                    scale = scale,
                )
            } else {
                Box(
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                ) {
                    SolarIcon(name = tabIcon(tab), tint = tint, size = 22.dp)
                }
            }
        }
    }
}

/**
 * Profile tab slot: shows a circular thumbnail of the user's Telegram
 * avatar when one is bound; otherwise falls back to the same Solar
 * user-bold-duotone glyph the other tabs use, so the row stays
 * visually balanced when no avatar is set.
 *
 * Important: the avatar is rendered slightly larger than the other
 * tab icons (28 dp vs 22 dp) — at 22 dp the photo reads as a generic
 * blob and is hard to recognise; 28 dp is the smallest size at which
 * the face is still identifiable, and it still sits comfortably
 * inside the 44 dp indicator pill (with the ×1.12 active scale taking
 * it to ~31 dp, well within the 44 dp slot).
 */
@Composable
private fun ProfileNavSlot(tint: androidx.compose.ui.graphics.Color, scale: Float) {
    val state = LocalAppState.current
    val photoUrl = state.telegramUser?.photoUrl
    if (photoUrl == null) {
        // No bound avatar — keep the original glyph so the slot reads
        // as "profile" rather than as an empty circle.
        Box(
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        ) {
            SolarIcon(name = tabIcon(Tab.Profile), tint = tint, size = 22.dp)
        }
        return
    }
    val context = LocalContext.current
    Box(
        Modifier
            .size(28.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            // Soft tinted backdrop behind the photo — matches the
            // active-state pill colour, and shows through as a faint
            // ring while the image is decoding so the slot never
            // flashes empty.
            .background(Wellness.colors.accentSoft, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photoUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
