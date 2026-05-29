package com.wellness.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.wellness.app.ui.screens.ProgressGoalsScreen
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.Tab
import com.wellness.app.ui.theme.Wellness
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

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
    data object ProgressGoals : AddOverlay
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
    // way to the home screen).
    //
    // Rendering model: the *top* of the stack is the active overlay —
    // it gets a RoundedSlideOverlay that drives the host parallax (so
    // the home tab content slides/zooms behind it), is interactive, and
    // can be dismissed by swipe-back. If the stack has a level below
    // (e.g. [Other, Logs]) we render that **second-from-top** overlay
    // statically as an "underlay" behind the active one, with no swipe
    // gestures and a no-op back. That guarantees:
    //   - When the user taps Логи from Другое, the slide-in of Logs
    //     reveals Other behind it — not the home tab (Profile). The
    //     previous code unmounted Other on the key change and the user
    //     briefly saw Profile through the gap.
    //   - When the user swipes Logs back, Logs slides out to the right
    //     revealing the still-rendered Other below. After onDismissed
    //     fires we re-mount Other as the new active top, but it was
    //     already at its final on-screen position so there's no second
    //     slide-in animation (see `animateIn` below).
    //
    // `lastAction` tells the active RoundedSlideOverlay whether the
    // user got here via PUSH (animate in from the right) or POP (it
    // was already on screen as an underlay — no entry animation).
    var overlayStack by remember { mutableStateOf<List<AddOverlay>>(emptyList()) }
    var lastAction by remember { mutableStateOf("init") }
    val overlay: AddOverlay? = overlayStack.lastOrNull()
    val underlay: AddOverlay? = if (overlayStack.size >= 2) overlayStack[overlayStack.size - 2] else null
    val push: (AddOverlay) -> Unit = { o -> overlayStack = overlayStack + o; lastAction = "push" }
    val pop: () -> Unit = { overlayStack = overlayStack.dropLast(1); lastAction = "pop" }
    // Hoisted scroll position for the Progress-Goals screen. The SAME
    // instance is fed to both its "top overlay" and "underlay" rendering, so
    // opening the weight/sleep adder (which moves this screen from the top
    // slot to the underlay slot) no longer creates a fresh scroll state that
    // snaps the page back to the top.
    val progressScroll = rememberScrollState()
    val parallax = rememberParallaxProgress()
    // Sink for nested overlays. When the stack is >= 2 levels deep the
    // active top RoundedSlideOverlay must NOT drive the host parallax,
    // because the underlay (Other under Logs) is rendered as a fully
    // opaque sibling above the home tab. If the new Logs RSO mirrored
    // its dismissProgress=1 starting value into the real parallax, the
    // navbar (whose alpha = parallax) would flash to 1 for one frame
    // every time the user opens a nested screen — that's the wrong
    // entry animation the user reported for Logs from Другое. The
    // dummy sink absorbs the mirror writes so nested entry/exit reads
    // exactly like a normal slide-in over the parent.
    // The top overlay at depth >= 2 drives THIS state instead of the
    // host parallax. We also read it from the underlay Box so the
    // screen underneath (e.g. Other under Logs) slides slightly to
    // the left as the top overlay slides in from the right — the
    // iOS-style parallax the rest of the app already does for the
    // home pager via OverlayHost. Initial value is 1f (no shift) so
    // there's no flicker before the first RSO write.
    val underlayParallax = remember { mutableFloatStateOf(1f) }

    // Frosted-glass source — captures the tab content drawn under
    // the navbar so the bar can render a real RenderEffect-blurred
    // snapshot through it. The source is on a SIBLING wrapper, not
    // on the root that also holds the navbar, so the navbar's own
    // pixels don't end up in the source (avoids the empty-navbar
    // feedback we saw in r33).
    val hazeState = remember { HazeState() }

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        Box(Modifier.fillMaxSize().haze(hazeState)) {
        OverlayHost(parallaxProgress = parallax) {
            Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = state.currentTab,
                    transitionSpec = {
                        // Horizontal slide-swap: the outgoing screen leaves
                        // toward one edge and the incoming screen enters from
                        // the opposite edge at the same time. Direction is
                        // driven by the actual order of tabs in the navbar
                        // (so reordering in настройки behaves correctly).
                        val order = state.navbarOrder
                        val fromIdx = order.indexOf(initialState).let { if (it < 0) 0 else it }
                        val toIdx = order.indexOf(targetState).let { if (it < 0) 0 else it }
                        val forward = toIdx >= fromIdx
                        val dur = 260
                        val ease = FastOutSlowInEasing
                        if (forward) {
                            (slideInHorizontally(tween(dur, easing = ease)) { it } +
                                fadeIn(tween(dur)))
                                .togetherWith(
                                    slideOutHorizontally(tween(dur, easing = ease)) { -it } +
                                        fadeOut(tween(dur))
                                )
                        } else {
                            (slideInHorizontally(tween(dur, easing = ease)) { -it } +
                                fadeIn(tween(dur)))
                                .togetherWith(
                                    slideOutHorizontally(tween(dur, easing = ease)) { it } +
                                        fadeOut(tween(dur))
                                )
                        }
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
                        Tab.Profile -> ProfileScreen(
                            onEditProfile = { push(AddOverlay.EditProfile) },
                            onGoals = { push(AddOverlay.Goals) },
                            onAppearance = { push(AddOverlay.Appearance) },
                            onNotifications = { push(AddOverlay.Notifications) },
                            onBindings = { push(AddOverlay.Bindings) },
                            onTiwi = { push(AddOverlay.Tiwi) },
                            onOther = { push(AddOverlay.Other) },
                            onProgressDetail = { push(AddOverlay.ProgressGoals) },
                            onQuickScan = { push(AddOverlay.Tiwi) },
                            onQuickWeight = { push(AddOverlay.Weight) },
                            onQuickAchievements = { push(AddOverlay.Tiwi) },
                        )
                    }
                }
            }
        }
        } // end haze source wrapper — the navbar's backdrop blur reads
          // ONLY the live tab content here. Overlays are rendered AFTER
          // the navbar (below) so they layer ON TOP of it: the bar never
          // moves — a screen / bottom-sheet simply covers it, exactly
          // like the old versions. Keeping the navbar OUT of the haze
          // source is also what stops the blur snapshot eating its icons.

        // Navbar — always mounted on the root tabs, fixed in place. It is
        // a sibling of (not inside) the haze source, and is drawn BEFORE
        // the overlays so any open screen / sheet sits ON TOP of it
        // rather than the bar floating over the screen or sliding away.
        Box(modifier = Modifier.fillMaxSize()) {
            Navbar(
                current = state.currentTab,
                onSelect = { state.currentTab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
                hazeState = hazeState,
            )
        }

        // Underlay — only the second-from-top overlay, rendered
        // statically full-screen behind the active one (no slide, no
        // swipe-back, no-op onBack). Keeps the parent visible during
        // child push/pop so the home tab never flashes through.
        underlay?.let { u ->
            if (u != AddOverlay.Weight) {
                // Slide the underlay slightly left as the top overlay
                // covers it (parallax matches the home-pager behaviour
                // — 28% of width at fully-covered). underlayParallax is
                // 1f when nothing is covering (no shift), 0f when the
                // top overlay fully covers the screen (-28% shift).
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = underlayParallax.floatValue
                            translationX = -(1f - p) * size.width * 0.28f
                        }
                        .background(Wellness.colors.bg)
                ) {
                    OverlayContent(
                        current = u,
                        animatedBack = {},
                        onPushLogs = {},
                        progressScrollState = progressScroll,
                    )
                }
            }
        }

        // Multi-field forms get the full-screen "slide in from the right"
        // overlay. Weight is a BottomSheet (rendered outside this branch).
        overlay?.let { current ->
            if (current == AddOverlay.Weight) {
                AddWeightScreen(onBack = { pop() })
            } else {
                // Only animate-in for PUSH actions. If we got here via
                // POP (the user dismissed a child overlay), this view
                // was already on screen as an underlay — re-mounting it
                // and re-running the slide-in would look like the new
                // top "appears" from the right one extra time.
                val animateInTop = lastAction != "pop"
                val topParallax = if (overlayStack.size >= 2) underlayParallax else parallax
                key(current, animateInTop) {
                    RoundedSlideOverlay(
                        parallaxProgress = topParallax,
                        onDismissed = { pop() },
                        animateIn = animateInTop,
                    ) { animatedBack ->
                        OverlayContent(
                            current = current,
                            animatedBack = animatedBack,
                            onPushLogs = { push(AddOverlay.Logs) },
                            onPushWeight = { push(AddOverlay.Weight) },
                            onPushSleep = { push(AddOverlay.Sleep) },
                            progressScrollState = progressScroll,
                        )
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

/**
 * Renders the body of a single overlay level. Extracted so both the
 * static underlay (no animation, no back) and the interactive top
 * (slide+swipe via RoundedSlideOverlay) share one switch and stay in
 * sync when new overlay types are added.
 */
@Composable
private fun OverlayContent(
    current: AddOverlay,
    animatedBack: () -> Unit,
    onPushLogs: () -> Unit,
    onPushWeight: () -> Unit = {},
    onPushSleep: () -> Unit = {},
    progressScrollState: ScrollState? = null,
) {
    when (current) {
        AddOverlay.Habit -> AddHabitScreen(onBack = animatedBack)
        AddOverlay.Task -> AddTaskScreen(onBack = animatedBack)
        AddOverlay.Nutrition -> AddNutritionScreen(onBack = animatedBack)
        AddOverlay.Sleep -> AddSleepScreen(onBack = animatedBack)
        AddOverlay.Weight -> {} // weight is a bottom-sheet, handled elsewhere
        AddOverlay.EditProfile -> EditProfileScreen(onBack = animatedBack)
        AddOverlay.Goals -> GoalsScreen(onBack = animatedBack)
        AddOverlay.Appearance -> AppearanceScreen(onBack = animatedBack)
        AddOverlay.Notifications -> NotificationsScreen(onBack = animatedBack)
        AddOverlay.Bindings -> BindingsScreen(onBack = animatedBack)
        AddOverlay.Tiwi -> TiwiPlaceholder(onBack = animatedBack)
        AddOverlay.Other -> OtherScreen(onBack = animatedBack, onLogs = onPushLogs)
        AddOverlay.Logs -> LogsScreen(onBack = animatedBack)
        AddOverlay.ProgressGoals -> ProgressGoalsScreen(
            onBack = animatedBack,
            onAddWeight = onPushWeight,
            onAddSleep = onPushSleep,
            scrollState = progressScrollState,
        )
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
