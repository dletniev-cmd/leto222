package com.wellness.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import com.wellness.app.ui.components.noFeedbackClick
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.components.IconButtonRound
import com.wellness.app.ui.components.ProgressRing
import com.wellness.app.ui.components.ScreenHorizontalPadding
import com.wellness.app.ui.components.SectionTitle
import com.wellness.app.ui.components.rememberElasticOverscroll
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.Dates
import com.wellness.app.ui.state.Habit
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.TaskItem
import com.wellness.app.ui.state.TaskStatus
import com.wellness.app.ui.theme.Wellness
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.LocalTime

// -------- Tuning constants for the collapsing-header animation -----------------

private val RingNatural = 64.dp     // ring diameter when fully expanded
private val RingTarget  = 32.dp     // ring diameter when collapsed into the title row
private val TargetSpacing = 19.dp   // center-to-center distance in the collapsed cluster
private const val MiniRingCount = 3 // miniature only shows first N rings, centred

// Horizontal strip cell geometry — each habit cell is FIXED width so the row
// scrolls horizontally when there are more habits than fit on screen. Cell
// gap is small so rings sit close together (a wider gap made the strip read
// as four loose icons rather than a single cluster).
private val StripCellWidth = 72.dp
private val StripCellGap = 4.dp

private val TitleHeight       = 56.dp  // header row height (title's row)
private val SectionTitleHeight = 40.dp // "Привычки" row reserved height
private val StripHeight       = 110.dp // habit strip height
private val ScrollTopPadding  = 6.dp   // space above the title

// Total scroll distance over which the collapse animation happens.
private val CollapseScrollDistance = SectionTitleHeight + StripHeight + 10.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlanScreen(
    onAddHabit: () -> Unit = {},
    onAddTask: () -> Unit = {},
) {
    val state = LocalAppState.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val hScrollState = rememberScrollState()

    // Shared iOS-style elastic translation overscroll. Vertical translation
    // is applied to the scrolling Column, horizontal translation to the
    // habit strip's Row. The collapsing mini-cluster gets a per-strip
    // counter-translation (-overscrollY * t) so that once fully collapsed it
    // stays nailed to the title row, even while the rest of the column
    // elastically pulls.
    val elastic = rememberElasticOverscroll()
    val verticalOverscroll = elastic.verticalOverscroll
    val horizontalOverscroll = elastic.horizontalOverscroll

    val collapsePx = with(density) { CollapseScrollDistance.toPx() }
    // derivedStateOf so the alpha/scale chain only recomposes when the clamped
    // progress actually changes (i.e. ~25 times across the whole collapse,
    // not every pixel of scroll).
    val collapseProgressState = remember(scrollState, collapsePx) {
        derivedStateOf { (scrollState.value / collapsePx).coerceIn(0f, 1f) }
    }

    // On Plan tab entry: hard-reset both the vertical scroll and the strip's
    // horizontal scroll. rememberSaveable preserves scroll state across tab
    // switches, so without this the user can come back to Plan and see the
    // rings frozen at a partial-collapse position — which is exactly what
    // QA flagged (n8847). Always starting fresh is also what the user asked
    // for: "when you collapse the rings then re-expand, they should start
    // from the beginning" — so we also reset hScrollState alongside.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        scrollState.scrollTo(0)
        hScrollState.scrollTo(0)
    }

    // Once the strip is fully docked (rings collapsed into the title row),
    // the horizontal scroll is invisible — reset it silently so that when
    // the user re-expands, the strip is back to its starting position.
    androidx.compose.runtime.LaunchedEffect(scrollState, collapsePx) {
        androidx.compose.runtime.snapshotFlow { scrollState.value >= collapsePx - 0.5f }
            .collect { fullyDocked ->
                if (fullyDocked && hScrollState.value != 0) {
                    hScrollState.scrollTo(0)
                }
            }
    }

    // Safety net: if the screen settles inside the collapse zone with the
    // fling behaviour not having had a chance to run (e.g. the user lifted
    // mid-drag with negligible velocity), snap it to the nearest rest
    // position. The snap fling only fires on flings, not idle landings.
    androidx.compose.runtime.LaunchedEffect(scrollState, collapsePx) {
        androidx.compose.runtime.snapshotFlow { scrollState.isScrollInProgress to scrollState.value }
            .collect { (inProgress, value) ->
                if (!inProgress) {
                    val v = value.toFloat()
                    if (v > 0.5f && v < collapsePx - 0.5f) {
                        val target = if (v > collapsePx * 0.5f) collapsePx.toInt() else 0
                        scrollState.animateScrollTo(
                            target,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                }
            }
    }

    // Smooth snap-to-rest after finger lift. The previous attempt put the
    // snap on a NestedScrollConnection and launched a separate coroutine
    // for the animation — which raced against the inner scrollable’s own
    // fling block over the scroll mutex and lost roughly half the time,
    // leaving the rings frozen mid-collapse.
    //
    // Doing it as a custom [FlingBehavior] solves this cleanly: performFling
    // runs INSIDE the scrollable’s scroll block (i.e. it already owns the
    // mutex), so the snap animation is never preempted. We delegate to the
    // default decay fling only when we’re outside the collapse zone — i.e.
    // when the user is genuinely flinging the schedule list below.
    //
    // Direction: lift velocity wins; if the user released perfectly still
    // (v≈0), we honour the position as a tiebreaker, but with a strong
    // bias toward “continue collapse” once the user has clearly started
    // collapsing (value > 1/4 collapsePx) — this is what the user asked
    // for: a slow drag toward hidden must finish in hidden, not bounce
    // back to the initial expanded state.
    val defaultFling = ScrollableDefaults.flingBehavior()
    val snapFling = remember(scrollState, collapsePx, defaultFling) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val value = scrollState.value.toFloat()
                if (value <= 0.5f || value >= collapsePx - 0.5f) {
                    // Outside the collapse zone — normal fling for the
                    // schedule list below.
                    return with(defaultFling) { performFling(initialVelocity) }
                }
                val direction = when {
                    initialVelocity > 10f -> 1f
                    initialVelocity < -10f -> -1f
                    else -> if (value > collapsePx * 0.25f) 1f else -1f
                }
                val target = if (direction > 0f) collapsePx else 0f
                var prev = value
                animate(
                    initialValue = value,
                    targetValue = target,
                    initialVelocity = initialVelocity,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { latest, _ ->
                    scrollBy(latest - prev)
                    prev = latest
                }
                return 0f
            }
        }
    }

    // Kills the system-level stretch overscroll on every scroll container
    // inside this screen (the verticalScroll Column AND the strip's
    // horizontalScroll). At the edges the scroll now just stops — no
    // visible deformation of the habit rings, labels or task cards. Wrap
    // the BoxWithConstraints so both scroll modifiers see the null config.
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .nestedScroll(elastic.connection),
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val statusBarPx = WindowInsets.statusBars.getTop(density)

        // Pinned title position.
        val titleTopPx = statusBarPx + with(density) { ScrollTopPadding.toPx() }
        val titleHeightPx = with(density) { TitleHeight.toPx() }
        val titleCenterY = titleTopPx + titleHeightPx / 2f

        // Strip ring center when verticalScroll == 0 (constant per layout).
        val stripTopAtZeroScrollPx =
            titleTopPx + titleHeightPx + with(density) { SectionTitleHeight.toPx() }
        val stripRingCenterAtZeroScroll =
            stripTopAtZeroScrollPx + with(density) { RingNatural.toPx() } / 2f

        // Per-ring horizontal centers in expanded layout (fixed-width scrollable cells).
        val hPadPx = with(density) { ScreenHorizontalPadding.toPx() }
        val cellWidthPx = with(density) { StripCellWidth.toPx() }
        val cellGapPx = with(density) { StripCellGap.toPx() }

        // Per-ring horizontal centers in collapsed mini cluster.
        //
        // Final state docks the cluster against the right edge of the
        // screen at the same Y as the (centred, stationary) title:
        //     [        "План"        ][ ○ ○ ○ ]
        // Slot 0 is the LEFT-most ring in the collapsed cluster, slot
        // N−1 the right-most — same ordering as the expanded strip. This
        // is important: with consistent left-to-right ordering across
        // both states, rings never have to cross each other in transit
        // (no "смешиваются" visual chaos), they just contract
        // rightward as a group.
        val targetSpacingPx = with(density) { TargetSpacing.toPx() }
        val visibleMiniCount = minOf(MiniRingCount, state.habits.size)
        val ringTargetPx = with(density) { RingTarget.toPx() }
        val rightEdgePadPx = with(density) { 18.dp.toPx() }
        // Right-most slot sits just inside the screen's right padding;
        // slot 0 is (N−1) * spacing to its left.
        val clusterStartX = screenWidthPx - rightEdgePadPx -
            ringTargetPx / 2f - (visibleMiniCount - 1) * targetSpacingPx

        // Measure the pinned title (used for vertical anchoring only —
        // the title doesn't translate horizontally any more).
        val titleMeasurer = rememberTextMeasurer()
        val titleStyle = Wellness.typography.displayMedium
        val titleLayout = remember(titleMeasurer, titleStyle, density) {
            titleMeasurer.measure(text = AnnotatedString("План"), style = titleStyle)
        }
        @Suppress("UNUSED_VARIABLE") val titleWidthPx = titleLayout.size.width.toFloat()
        val ringTargetScale = RingTarget.value / RingNatural.value

        // Pre-compute the minimum column height in pixels that guarantees
        // the user can scroll at least [CollapseScrollDistance] further
        // than the viewport (so the rings can always fully collapse, no
        // matter how short the schedule list is). The chrome we have to
        // account for is: status bar inset + top padding (title + spacer
        // above it) + bottom padding (navbar clearance).
        val viewportPx = with(density) { maxHeight.toPx() }.toInt()
        val verticalChromePx = with(density) {
            (ScrollTopPadding + TitleHeight + 110.dp).toPx()
        }.toInt() + statusBarPx
        val collapsePxInt = collapsePx.toInt()
        val minColumnHeightPx =
            (viewportPx + collapsePxInt - verticalChromePx).coerceAtLeast(0)

        // ------------- LAYER 1 : scrolling content ----------------------------
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Elastic translation: shifts the whole scrolling column by
                // the current vertical overscroll. Runs in the draw phase via
                // graphicsLayer so layout isn't disturbed.
                .graphicsLayer { translationY = verticalOverscroll.floatValue }
                // The custom [snapFling] runs INSIDE the scroll mutex on
                // finger lift — see definition above. It commits the
                // partial collapse to whichever direction the user was
                // dragging and delegates to the normal decay fling once
                // we’re past the collapse zone (so the schedule list below
                // still flings naturally).
                .verticalScroll(scrollState, flingBehavior = snapFling)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    top = ScrollTopPadding + TitleHeight,
                    bottom = 110.dp,
                )
                // Floor the column's reported height at [minColumnHeightPx]
                // so the scroll range always exceeds [CollapseScrollDistance]
                // — but only when the natural content is actually shorter
                // than that. Long schedule lists report their real height
                // unmodified, so there's no wasted empty space at the
                // bottom of the screen.
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val height = maxOf(placeable.height, minColumnHeightPx)
                    layout(placeable.width, height) {
                        placeable.place(0, 0)
                    }
                },
        ) {
            // Section title fades out as we collapse. graphicsLayer keeps the
            // alpha read inside the draw phase so we don't recompose Text on
            // every scroll tick.
            Box(
                modifier = Modifier
                    .height(SectionTitleHeight)
                    .graphicsLayer {
                        alpha = (1f - collapseProgressState.value * 1.8f).coerceIn(0f, 1f)
                    },
            ) {
                SectionTitle("Привычки", topPadding = 0.dp) {
                    IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddHabit)
                }
            }

            // Habit strip — single-ring architecture. The whole strip lifts
            // vertically via graphicsLayer (just enough that the ring lands
            // at the title row's Y when fully collapsed) and each ring also
            // morphs in X+scale into its mini-cluster slot. The rings stay
            // INSIDE the horizontal scroll row, so Android's overscroll-
            // stretch effect tugs them sideways together with the labels.
            // No overlay layer, no crossfade — there is only ever one ring
            // per habit on screen.
            HabitStrip(
                habits = state.habits,
                hScrollState = hScrollState,
                collapseProgress = { collapseProgressState.value },
                stripLiftPx = {
                    // Lifts the strip so its ring lands exactly at titleCenter
                    // when t == 1 AND stays pinned there for any further
                    // scroll past collapsePx. Decomposes into three terms:
                    //   translationY = vScroll − yTravel * t − overscrollY * t
                    //
                    // • +vScroll                cancels the column's own vertical
                    //                          scroll once we're fully collapsed,
                    //                          so the mini-cluster doesn't drift
                    //                          up with the rest of the page.
                    // • −yTravel * t           pulls the ring from its natural Y
                    //                          (stripCenter) down to the title row
                    //                          (titleCenter) linearly with t.
                    // • −overscrollY * t       cancels the COLUMN-level elastic
                    //                          overscroll translation by the same
                    //                          amount that has been applied above,
                    //                          but only proportionally to t.
                    //                          At t=0 (expanded) the strip moves
                    //                          with the elastic content; at t=1
                    //                          (mini-cluster pinned to title) it
                    //                          ignores the overscroll entirely.
                    val t = collapseProgressState.value
                    val yTravelPx = stripRingCenterAtZeroScroll - titleCenterY
                    scrollState.value.toFloat() -
                        yTravelPx * t -
                        verticalOverscroll.floatValue * t
                },
                hScrollOffset = { hScrollState.value.toFloat() },
                hPadPx = hPadPx,
                cellWidthPx = cellWidthPx,
                cellGapPx = cellGapPx,
                clusterStartX = clusterStartX,
                targetSpacingPx = targetSpacingPx,
                ringTargetScale = ringTargetScale,
                visibleMiniCount = visibleMiniCount,
                horizontalOverscrollPx = { horizontalOverscroll.floatValue * (1f - collapseProgressState.value) },
            )

            SectionTitle("Расписание на сегодня") {
                IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddTask)
            }
            ScheduleList(state.tasksToday())
        }

        // ------------- LAYER 3 : pinned title ---------------------------------
        // (Previous LAYER 2 darkening overlay removed — pinned title now floats
        // directly over the scrolled content with no fade-to-background, the
        // same way the system Gallery / Telegram-style large titles work.)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = ScrollTopPadding)
                .height(TitleHeight)
                .screenHPad(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "План",
                color = Wellness.colors.text,
                style = titleStyle,
                // Title is anchored dead-centre at all times. The mini-
                // cluster slots into the right margin alongside it; the
                // title itself never translates.
            )
        }

    }
    }
}

// ---------- Habit strip (inline rings, only visible while expanded) ------------

@Composable
private fun HabitStrip(
    habits: List<Habit>,
    hScrollState: androidx.compose.foundation.ScrollState,
    collapseProgress: () -> Float,
    stripLiftPx: () -> Float,
    hScrollOffset: () -> Float,
    hPadPx: Float,
    cellWidthPx: Float,
    cellGapPx: Float,
    clusterStartX: Float,
    targetSpacingPx: Float,
    ringTargetScale: Float,
    visibleMiniCount: Int,
    horizontalOverscrollPx: () -> Float,
) {
    Row(
        modifier = Modifier
            // zIndex above sibling children of the parent Column so the rings
            // stay ABOVE the schedule list as the strip slides up into the
            // title row — otherwise the task cards (rendered later in the
            // Column) draw over the mini-cluster.
            .zIndex(1f)
            // Two translations baked into the same draw-phase layer:
            //   • translationY: lifts the strip so its ring lands exactly at
            //     titleCenter when fully collapsed and stays pinned beyond.
            //   • translationX: elastic horizontal overscroll for the strip
            //     (pulled past its left/right edge).
            .graphicsLayer {
                translationY = stripLiftPx()
                translationX = horizontalOverscrollPx()
            }
            .fillMaxWidth()
            .horizontalScroll(hScrollState)
            .height(StripHeight),
        verticalAlignment = Alignment.Top,
    ) {
        // NB: no Arrangement.spacedBy — explicit Spacers so the per-ring
        // X-morph math matches:
        //   center(i) = hPad + cellWidth/2 + i*(cellWidth + cellGap)
        Spacer(Modifier.width(ScreenHorizontalPadding))
        habits.forEachIndexed { idx, h ->
            if (idx > 0) Spacer(Modifier.width(StripCellGap))
            HabitCell(
                habit = h,
                index = idx,
                collapseProgress = collapseProgress,
                hScrollOffset = hScrollOffset,
                hPadPx = hPadPx,
                cellWidthPx = cellWidthPx,
                cellGapPx = cellGapPx,
                clusterStartX = clusterStartX,
                targetSpacingPx = targetSpacingPx,
                ringTargetScale = ringTargetScale,
                visibleMiniCount = visibleMiniCount,
            )
        }
        Spacer(Modifier.width(ScreenHorizontalPadding))
    }
}

@Composable
private fun HabitCell(
    habit: Habit,
    index: Int,
    collapseProgress: () -> Float,
    hScrollOffset: () -> Float,
    hPadPx: Float,
    cellWidthPx: Float,
    cellGapPx: Float,
    clusterStartX: Float,
    targetSpacingPx: Float,
    ringTargetScale: Float,
    visibleMiniCount: Int,
) {
    val state = LocalAppState.current
    val todayKey = Dates.todayKey()
    val progressCount = habit.progressOn(todayKey)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(StripCellWidth),
    ) {
        // The one and only ring per habit. Lives INSIDE the horizontal
        // scroll Row, so overscroll-stretch tugs it sideways together with
        // the labels. As the user scrolls vertically, the parent strip
        // translates up (carrying ring + labels) and each ring independently
        // morphs in X + scale into its mini-cluster slot via this layer.
        //
        // Tapping the ring increments today's progress (mirrors the same
        // affordance on HomeScreen — previously this surface was inert,
        // which was the "ничо не происходит" bug from the user report).
        Box(
            modifier = Modifier
                .size(RingNatural)
                .noFeedbackClick { state.tapHabit(habit.id) }
                .graphicsLayer {
                    val t = collapseProgress()
                    // Short-circuit when the strip is fully expanded. This
                    // is CRITICAL for performance: by NOT reading
                    // hScrollOffset() in this branch we don't subscribe to
                    // it, so dragging the habit strip horizontally doesn't
                    // re-invalidate every ring's layer every frame. With
                    // many habits that was the cause of jerky scroll.
                    if (t <= 0f) {
                        translationX = 0f
                        scaleX = 1f
                        scaleY = 1f
                        alpha = 1f
                        return@graphicsLayer
                    }
                    val slot = index.coerceAtMost(visibleMiniCount - 1)
                    val layoutCenterX =
                        hPadPx + cellWidthPx / 2f + index * (cellWidthPx + cellGapPx)
                    val targetCenterX = clusterStartX + slot * targetSpacingPx
                    translationX = (targetCenterX - layoutCenterX + hScrollOffset()) * t
                    val scale = 1f + (ringTargetScale - 1f) * t
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin.Center
                    alpha = if (index < visibleMiniCount) {
                        1f
                    } else {
                        (1f - t * 1.4f).coerceIn(0f, 1f)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val progress = if (habit.target == 0) 0f else progressCount.toFloat() / habit.target
            ProgressRing(
                progress = progress,
                color = habit.color,
                size = RingNatural,
                strokeWidth = 4.dp,
            )
            SolarIcon(name = habit.icon, tint = habit.color, size = 28.dp)
        }

        Spacer(Modifier.height(10.dp))

        // Two-line fixed-height label block. Fades out over the first ~42%
        // of the collapse — by the time labels are invisible the ring has
        // arrived at the title row's Y level and is about to morph X/scale
        // into the cluster slot.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .graphicsLayer {
                    alpha = (1f - collapseProgress() * 2.4f).coerceIn(0f, 1f)
                },
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = habit.name,
                color = Wellness.colors.text,
                style = Wellness.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (habit.unit.isEmpty()) {
                    "$progressCount/${habit.target}"
                } else {
                    "$progressCount/${habit.target} ${habit.unit}"
                },
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------- Schedule list -----------------------------------------------------

@Composable
private fun ScheduleList(tasks: List<TaskItem>) {
    val state = LocalAppState.current
    val dateKey = Dates.todayKey()

    // Tick once a minute so live/upcoming/past partitioning stays current.
    var nowMin by remember { mutableIntStateOf(LocalTime.now().toSecondOfDay() / 60) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMin = LocalTime.now().toSecondOfDay() / 60
        }
    }
    val sorted = tasks.sortedBy { it.startMinutes }
    val past = sorted.filter { it.statusAt(nowMin, dateKey) == TaskStatus.Done }
    val live = sorted.firstOrNull { it.statusAt(nowMin, dateKey) == TaskStatus.Live }
    val upcoming = sorted.filter { it.statusAt(nowMin, dateKey) == TaskStatus.Upcoming }

    Column(
        modifier = Modifier.screenHPad(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (sorted.isEmpty()) {
            Text(
                "Расписания на сегодня нет — добавь первую задачу",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        live?.let { TaskCard(it, nowMin, dateKey) { state.toggleTaskDone(it.id) } }
        upcoming.forEach { t -> TaskCard(t, nowMin, dateKey) { state.toggleTaskDone(t.id) } }
        if (past.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Прошедшие",
                color = Wellness.colors.muted,
                style = Wellness.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            past.forEach { t -> TaskCard(t, nowMin, dateKey) { state.toggleTaskDone(t.id) } }
        }
    }
}

@Composable
private fun TaskCard(task: TaskItem, nowMin: Int, dateKey: String, onToggle: () -> Unit) {
    val status = task.statusAt(nowMin, dateKey)
    val isPast = status == TaskStatus.Done
    val isLive = status == TaskStatus.Live

    val accentColor = when (status) {
        TaskStatus.Live -> Wellness.colors.accent
        else -> Wellness.colors.text
    }
    val timeColor = if (isPast) Wellness.colors.muted else Wellness.colors.text
    val nameColor = if (isPast) Wellness.colors.muted else Wellness.colors.text
    val containerAlpha = if (isPast) 0.55f else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = containerAlpha }
            .background(Wellness.colors.container, RoundedCornerShape(28.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Tappable status bullet — single tap toggles done state for today.
            com.wellness.app.ui.components.NoFeedbackButton(
                onClick = onToggle,
                modifier = Modifier.size(28.dp),
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isPast -> Box(
                            Modifier
                                .size(20.dp)
                                .background(task.color.copy(alpha = 0.85f), RoundedCornerShape(999.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            SolarIcon(name = "check-read-bold", tint = Wellness.colors.bg, size = 12.dp)
                        }
                        isLive -> Box(
                            Modifier
                                .size(14.dp)
                                .background(Wellness.colors.accent, RoundedCornerShape(999.dp))
                        )
                        else -> Box(
                            Modifier
                                .size(20.dp)
                                .background(task.color.copy(alpha = 0.18f), RoundedCornerShape(999.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            SolarIcon(name = task.icon, tint = task.color, size = 12.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.startTime, color = timeColor, style = Wellness.typography.titleSmall)
                    Spacer(Modifier.width(8.dp))
                    Text(task.durationLabel, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
                }
                Spacer(Modifier.height(2.dp))
                Text(task.name, color = nameColor, style = Wellness.typography.titleMedium)
                val statusText = task.statusTextAt(nowMin, dateKey)
                if (statusText.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        statusText,
                        color = if (isLive) accentColor else Wellness.colors.muted,
                        style = Wellness.typography.bodySmall,
                    )
                }
            }
        }
    }
}

