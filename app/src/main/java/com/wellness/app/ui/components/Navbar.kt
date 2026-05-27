package com.wellness.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.Tab
import com.wellness.app.ui.theme.Wellness

/**
 * One icon per tab — the SAME asset is used in both active and inactive
 * state. Active vs inactive is communicated purely through tint, never
 * through an icon swap, so there is no decode “flicker” when the user
 * jumps between tabs.
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
// the navbar belongs to the same visual family as the rest of the app’s
// cards and tiles. The indicator under the active tab stays an iOS-style
// round pill, the way the user explicitly asked: it’s a 44×44 square clipped
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
    // it’s being tossed under the finger and softly catching, NOT like it’s
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
private fun NavItem(tab: Tab, active: Boolean, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp) {
    val tint = if (active) Wellness.colors.accent else Wellness.colors.muted
    // SAME icon asset in both states — only the tint differs. This avoids
    // the SVG re-decode flicker the user reported when active/inactive
    // were two different glyphs.
    NoFeedbackButton(onClick = onClick, modifier = Modifier.size(size)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SolarIcon(name = tabIcon(tab), tint = tint, size = 22.dp)
        }
    }
}
