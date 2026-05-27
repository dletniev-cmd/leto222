package com.wellness.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.wellness.app.ui.components.AccentSwitch
import com.wellness.app.ui.components.ColorPickerGrid
import com.wellness.app.ui.components.NoFeedbackButton
import com.wellness.app.ui.components.OverlayHost
import com.wellness.app.ui.components.RoundedSlideOverlay
import com.wellness.app.ui.components.SettingsCard
import com.wellness.app.ui.components.SettingsHeader
import com.wellness.app.ui.components.SettingsRow
import com.wellness.app.ui.components.SettingsRowDivider
import com.wellness.app.ui.components.rememberParallaxProgress
import com.wellness.app.ui.components.noFeedbackClick
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.components.tabIcon
import com.wellness.app.ui.components.tabTitle
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.Tab
import com.wellness.app.ui.theme.AccentPalette
import com.wellness.app.ui.theme.ThemeMode
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors

private enum class AppearanceRoute { Root, Navbar }

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    var route by remember { mutableStateOf(AppearanceRoute.Root) }
    // No external BackHandler needed: RoundedSlideOverlay registers its
    // own PredictiveBackHandler, which intercepts the system back gesture
    // and plays the slide-out (then calls onDismissed → route = Root).
    // Adding an extra BackHandler here would bypass the animation and
    // pop the overlay instantly.
    //
    // Same transition mechanic as the OUTER settings overlays —
    // RoundedSlideOverlay + OverlayHost give us the iOS-style stack push:
    //   - the Appearance Root parallax-translates left under the cover
    //   - the Navbar settings slide in from the right with the device’s
    //     rounded-corner clip while it crosses the edge
    //   - swipe-back-from-the-left dismisses, same as everywhere else
    val parallax = rememberParallaxProgress()
    Box(Modifier.fillMaxSize()) {
        OverlayHost(parallaxProgress = parallax) {
            AppearanceRoot(
                onBack = onBack,
                onNavbar = { route = AppearanceRoute.Navbar },
            )
        }
        if (route == AppearanceRoute.Navbar) {
            key(route) {
                RoundedSlideOverlay(
                    parallaxProgress = parallax,
                    onDismissed = { route = AppearanceRoute.Root },
                ) { animatedBack ->
                    NavbarSettingsScreen(onBack = animatedBack)
                }
            }
        }
    }
}

@Composable
private fun AppearanceRoot(onBack: () -> Unit, onNavbar: () -> Unit) {
    val state = LocalAppState.current
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 6.dp, bottom = 60.dp),
        ) {
            SettingsHeader(title = "Оформление", onBack = onBack)

            // Theme toggle
            Text(
                "ТЕМА",
                color = Wellness.colors.muted,
                style = Wellness.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 14.dp, bottom = 8.dp),
            )
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "moon-outline",
                    iconTile = WellnessColors.TileViolet,
                    title = "Тёмная тема",
                    value = if (state.themeMode == ThemeMode.Dark) "Включена" else "Выключена",
                    showChevron = false,
                    trailing = {
                        AccentSwitch(
                            checked = state.themeMode == ThemeMode.Dark,
                            onCheckedChange = { dark ->
                                state.themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light
                            },
                        )
                    },
                )
            }

            // Accent color picker
            Text(
                "АКЦЕНТНЫЙ ЦВЕТ",
                color = Wellness.colors.muted,
                style = Wellness.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 22.dp, bottom = 8.dp),
            )
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            ) {
                ColorPickerGrid(
                    colors = AccentPalette,
                    selected = state.accent,
                    onSelect = { state.accent = it },
                )
            }

            // Navbar settings entry
            Text(
                "НАВИГАЦИЯ",
                color = Wellness.colors.muted,
                style = Wellness.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 22.dp, bottom = 8.dp),
            )
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "settings-outline",
                    iconTile = WellnessColors.TileBlue,
                    title = "Навбар",
                    value = tabTitle(state.defaultTab),
                    onClick = onNavbar,
                )
            }
        }
    }
}

@Composable
private fun NavbarSettingsScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 6.dp, bottom = 60.dp),
        ) {
            SettingsHeader(title = "Навбар", onBack = onBack)

            // ---- Live navbar preview with long-press drag reorder ------------
            // The user asked for the actual navbar to be shown on the editor
            // ("сделай там чтобы отображался сам навбар и пункты можно
            // перетаскивать внутри него удержанием"). Drag-to-reorder gives the
            // editor the same shape as the real navbar at the bottom of the app.
            Text(
                "ПОРЯДОК",
                color = Wellness.colors.muted,
                style = Wellness.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 10.dp),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                DraggableNavbarPreview()
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 4.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Нажми и удержи иконку, чтобы перетащить",
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodySmall,
                )
            }

            // ---- Default landing tab -----------------------------------------
            Text(
                "СТАРТОВЫЙ ЭКРАН",
                color = Wellness.colors.muted,
                style = Wellness.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 22.dp, bottom = 8.dp),
            )
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                state.navbarOrder.forEachIndexed { i, tab ->
                    val active = tab == state.defaultTab
                    SettingsRow(
                        icon = tabIcon(tab),
                        iconTile = tileForTab(tab),
                        title = tabTitle(tab),
                        showChevron = false,
                        trailing = {
                            if (active) {
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .background(Wellness.colors.accent, RoundedCornerShape(999.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SolarIcon(
                                        name = "check-bold",
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        size = 14.dp,
                                    )
                                }
                            } else {
                                Box(Modifier.size(22.dp))
                            }
                        },
                        onClick = { state.defaultTab = tab },
                    )
                    if (i < state.navbarOrder.size - 1) SettingsRowDivider()
                }
            }
        }
    }
}

// A 1:1 replica of the actual bottom navbar that lets the user long-press
// any icon and drag it to a new slot. Layout is identical to the real
// navbar, just inverted: it sits inside a settings page rather than at
// the bottom of the screen.
@Composable
private fun DraggableNavbarPreview() {
    val state = LocalAppState.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val itemSize = 56.dp
    val gap = 6.dp
    val padding = 8.dp
    val cornerRadius = 28.dp

    val itemSizePx = with(density) { itemSize.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val slotPitchPx = itemSizePx + gapPx

    // Which slot is currently being dragged (-1 = none) and how far it
    // has been pulled from its slot center.
    var draggedIndex by remember { mutableIntStateOf(-1) }
    val dragOffsetPx = remember { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .background(
                color = Wellness.colors.container.copy(alpha = 0.94f),
                shape = RoundedCornerShape(cornerRadius),
            )
            .padding(padding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            state.navbarOrder.forEachIndexed { i, tab ->
                if (i > 0) Box(Modifier.width(gap))
                val isDragged = i == draggedIndex
                // Slots that aren't being dragged shift gently to make space
                // when the user drags an icon past their slot center.
                val sideShift = if (draggedIndex < 0) 0f else {
                    val from = draggedIndex
                    val targetFloat = (from + dragOffsetPx.floatValue / slotPitchPx)
                        .coerceIn(0f, state.navbarOrder.lastIndex.toFloat())
                    val to = kotlin.math.round(targetFloat).toInt()
                    when {
                        i == from -> 0f
                        from < to && i in (from + 1)..to -> -slotPitchPx
                        from > to && i in to..(from - 1) -> slotPitchPx
                        else -> 0f
                    }
                }
                val animShift by animateFloatAsState(
                    targetValue = sideShift,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "shift",
                )
                val scale by animateFloatAsState(
                    targetValue = if (isDragged) 1.12f else 1f,
                    animationSpec = spring(dampingRatio = 0.7f),
                    label = "scale",
                )
                Box(
                    Modifier
                        .size(itemSize)
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationX = if (isDragged) dragOffsetPx.floatValue else animShift
                            scaleX = scale
                            scaleY = scale
                        }
                        .pointerInput(state.navbarOrder.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedIndex = i
                                    dragOffsetPx.floatValue = 0f
                                    haptics.performHapticFeedback(
                                        HapticFeedbackType.LongPress,
                                    )
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val maxLeft = -i * slotPitchPx
                                    val maxRight = (state.navbarOrder.lastIndex - i) * slotPitchPx
                                    dragOffsetPx.floatValue =
                                        (dragOffsetPx.floatValue + drag.x)
                                            .coerceIn(maxLeft, maxRight)
                                },
                                onDragEnd = {
                                    val from = draggedIndex
                                    if (from >= 0) {
                                        val targetFloat = (from + dragOffsetPx.floatValue / slotPitchPx)
                                            .coerceIn(0f, state.navbarOrder.lastIndex.toFloat())
                                        val to = kotlin.math.round(targetFloat).toInt()
                                        if (to != from) {
                                            val moved = state.navbarOrder.removeAt(from)
                                            state.navbarOrder.add(to, moved)
                                            haptics.performHapticFeedback(
                                                HapticFeedbackType.TextHandleMove,
                                            )
                                        }
                                    }
                                    draggedIndex = -1
                                    dragOffsetPx.floatValue = 0f
                                },
                                onDragCancel = {
                                    draggedIndex = -1
                                    dragOffsetPx.floatValue = 0f
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(
                                color = if (isDragged) Wellness.colors.accentSoft
                                else androidx.compose.ui.graphics.Color.Transparent,
                                shape = RoundedCornerShape(999.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        SolarIcon(
                            name = tabIcon(tab),
                            tint = if (isDragged) Wellness.colors.accent else Wellness.colors.text,
                            size = 26.dp,
                        )
                    }
                }
            }
        }
    }
}

private fun tileForTab(tab: Tab): androidx.compose.ui.graphics.Color = when (tab) {
    Tab.Home -> WellnessColors.TileBlue
    Tab.Nutrition -> WellnessColors.TileOrange
    Tab.Plan -> WellnessColors.TileViolet
    Tab.Trackers -> WellnessColors.TileGreen
    Tab.Profile -> WellnessColors.TileTeal
}

