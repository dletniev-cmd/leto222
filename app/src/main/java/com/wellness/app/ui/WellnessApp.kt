package com.wellness.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.components.Navbar
import com.wellness.app.ui.components.NoFeedbackButton
import com.wellness.app.ui.components.OverlayHost
import com.wellness.app.ui.components.RoundedSlideOverlay
import com.wellness.app.ui.components.rememberParallaxProgress
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.screens.AddHabitScreen
import com.wellness.app.ui.screens.AddNutritionScreen
import com.wellness.app.ui.screens.AddSleepScreen
import com.wellness.app.ui.screens.AddTaskScreen
import com.wellness.app.ui.screens.AddWeightScreen
import com.wellness.app.ui.screens.AppearanceScreen
import com.wellness.app.ui.screens.BindingsScreen
import com.wellness.app.ui.screens.EditProfileScreen
import com.wellness.app.ui.screens.GoalsScreen
import com.wellness.app.ui.screens.HomeScreen
import com.wellness.app.ui.screens.LogsScreen
import com.wellness.app.ui.screens.NotificationsScreen
import com.wellness.app.ui.screens.OtherScreen
import com.wellness.app.ui.screens.NutritionScreen
import com.wellness.app.ui.screens.PlanScreen
import com.wellness.app.ui.screens.ProfileScreen
import com.wellness.app.ui.screens.TrackersScreen
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.Tab
import com.wellness.app.ui.theme.Wellness

/**
 * Identifies which secondary screen is currently active on top of the tabs.
 * Most are full-screen "slide in from the right" overlays; weight is rendered
 * as a real BottomSheet because it's a single-value picker.
 */
sealed interface AddOverlay {
    data object Habit : AddOverlay
    data object Task : AddOverlay
    data object Nutrition : AddOverlay
    data object Weight : AddOverlay
    data object Sleep : AddOverlay
    data object EditProfile : AddOverlay
    data object Goals : AddOverlay
    data object Appearance : AddOverlay
    data object Notifications : AddOverlay
    data object Bindings : AddOverlay
    data object Tiwi : AddOverlay
    data object Other : AddOverlay
    data object Logs : AddOverlay
}

@Composable
fun WellnessApp() {
    val state = LocalAppState.current

    // Snap to whatever the user picked as their default landing tab once on
    // first composition of the app shell.
    //
    // Keying this on `state.defaultTab` (the previous behaviour) was a
    // landmine: opening Appearance → Навбар and picking a new default tab
    // would mutate `currentTab` while the user was still inside the nested
    // overlay, kicking off a tab-content transition behind the overlay
    // stack. Swiping back from Навбар then races against that transition,
    // which crashed the app on some devices. The setting is a "next launch"
    // preference, not a "switch right now" action, so we only honour it on
    // first entry. From then on `currentTab` is owned solely by direct nav
    // (tab bar tap / overlay nav handlers).
    LaunchedEffect(Unit) {
        state.currentTab = state.defaultTab
    }

    // Stack of overlays so a sub-screen can pop back to its parent
    // (e.g. Logs opened from Other slides back into Other, not all the
    // way to the home screen). Pushing the same overlay twice is fine
    // — `key(current)` below treats each push as a fresh entry.
    var overlayStack by remember { mutableStateOf<List<AddOverlay>>(emptyList()) }
    val overlay: AddOverlay? = overlayStack.lastOrNull()
    val push: (AddOverlay) -> Unit = { o -> overlayStack = overlayStack + o }
    val pop: () -> Unit = { overlayStack = overlayStack.dropLast(1) }
    val parallax = rememberParallaxProgress()

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        OverlayHost(parallaxProgress = parallax) {
            Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = state.currentTab,
                    transitionSpec = {
                        // Snappy iOS-style cross-fade between tabs — fast
                        // enough to feel responsive (no perceived lag) but
                        // long enough that the swap reads as a soft dissolve
                        // rather than a hard cut.
                        fadeIn(tween(180)).togetherWith(fadeOut(tween(120)))
                    },
                    label = "screens",
                    modifier = Modifier.fillMaxSize(),
                ) { tab ->
                    when (tab) {
                        Tab.Home -> HomeScreen(
                            onAddWeight = { push(AddOverlay.Weight) },
                        )
                        Tab.Nutrition -> NutritionScreen(
                            onAddMeal = { push(AddOverlay.Nutrition) },
                        )
                        Tab.Plan -> PlanScreen(
                            onAddHabit = { push(AddOverlay.Habit) },
                            onAddTask = { push(AddOverlay.Task) },
                        )
                        Tab.Trackers -> TrackersScreen(
                            onAddWeight = { push(AddOverlay.Weight) },
                            onAddSleep = { push(AddOverlay.Sleep) },
                        )
                        Tab.Profile -> ProfileScreen(
                            onEditProfile = { push(AddOverlay.EditProfile) },
                            onGoals = { push(AddOverlay.Goals) },
                            onAppearance = { push(AddOverlay.Appearance) },
                            onNotifications = { push(AddOverlay.Notifications) },
                            onBindings = { push(AddOverlay.Bindings) },
                            onTiwi = { push(AddOverlay.Tiwi) },
                            onOther = { push(AddOverlay.Other) },
                        )
                    }
                }
            }
        }

        // Navbar lives OUTSIDE the parallax host so it doesn't slide off to
        // the left along with the tab content when an overlay opens. Instead
        // it fades on the same progress value — at parallax = 1 (no overlay)
        // it's fully visible; at parallax = 0 (overlay fully covering the
        // screen) it's invisible. The fade is driven inside a graphicsLayer
        // block so reading the float doesn't trigger recomposition of the
        // navbar tree.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = parallax.floatValue },
        ) {
            Navbar(
                current = state.currentTab,
                onSelect = { state.currentTab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // Multi-field forms get the full-screen "slide in from the right"
        // overlay. Weight is a BottomSheet (rendered outside this branch).
        overlay?.let { current ->
            if (current == AddOverlay.Weight) {
                AddWeightScreen(onBack = { pop() })
            } else {
                key(current) {
                    RoundedSlideOverlay(
                        parallaxProgress = parallax,
                        onDismissed = { pop() },
                    ) { animatedBack ->
                        when (current) {
                            AddOverlay.Habit -> AddHabitScreen(onBack = animatedBack)
                            AddOverlay.Task -> AddTaskScreen(onBack = animatedBack)
                            AddOverlay.Nutrition -> AddNutritionScreen(onBack = animatedBack)
                            AddOverlay.Sleep -> AddSleepScreen(onBack = animatedBack)
                            AddOverlay.Weight -> {} // handled above
                            AddOverlay.EditProfile -> EditProfileScreen(onBack = animatedBack)
                            AddOverlay.Goals -> GoalsScreen(onBack = animatedBack)
                            AddOverlay.Appearance -> AppearanceScreen(onBack = animatedBack)
                            AddOverlay.Notifications -> NotificationsScreen(onBack = animatedBack)
                            AddOverlay.Bindings -> BindingsScreen(onBack = animatedBack)
                            AddOverlay.Tiwi -> TiwiPlaceholder(onBack = animatedBack)
                            AddOverlay.Other -> OtherScreen(
                                onBack = animatedBack,
                                onLogs = { push(AddOverlay.Logs) },
                            )
                            AddOverlay.Logs -> LogsScreen(onBack = animatedBack)
                        }
                    }
                }
            }
        }

        // Crash reports are written to disk by CrashReporter on uncaught
        // exception and surfaced passively via Profile → Другое → Логи.
        // The previous launch-time dialog interrupted the cold-start flow
        // after every crash and looked alarming — the new screen lets the
        // user copy logs on demand without blocking the UI.
    }
}

@Composable
private fun TiwiPlaceholder(onBack: () -> Unit) {
    // The user explicitly asked that "Тифи" not open any real content for
    // now — just a polite stub with a back arrow so the entry still feels
    // wired up.
    Box(
        Modifier
            .fillMaxSize()
            .background(Wellness.colors.bg),
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            com.wellness.app.ui.components.SettingsHeader(title = "Тифи", onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SolarIcon(
                        name = "smile-circle-outline",
                        tint = Wellness.colors.muted.copy(alpha = 0.6f),
                        size = 64.dp,
                    )
                    Box(Modifier.size(14.dp))
                    Text(
                        "Скоро",
                        color = Wellness.colors.text,
                        style = Wellness.typography.headlineMedium,
                    )
                    Box(Modifier.size(6.dp))
                    Text(
                        "Этот раздел пока в работе",
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
