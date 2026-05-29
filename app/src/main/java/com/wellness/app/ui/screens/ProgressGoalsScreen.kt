package com.wellness.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wellness.app.ui.components.ElasticOverscroll
import com.wellness.app.ui.components.IconButtonRound
import com.wellness.app.ui.components.NoFeedbackButton
import com.wellness.app.ui.components.ProgressRing
import com.wellness.app.ui.components.SettingsHeader
import com.wellness.app.ui.components.rememberElasticOverscroll
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.GoalBreakdown
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.SleepEntry
import com.wellness.app.ui.state.WeightEntry
import com.wellness.app.ui.state.calculateGoalProgress
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.launch

// ── Shared helpers ─────────────────────────────────────────────────────

private val SHORT_DOW = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private val DAY_MONTH = DateTimeFormatter.ofPattern("dd.MM")

private fun parseDate(dateKey: String): LocalDate? =
    runCatching { LocalDate.parse(dateKey) }.getOrNull()

private fun shortDow(dateKey: String): String =
    parseDate(dateKey)?.let { SHORT_DOW[it.dayOfWeek.value - 1] } ?: ""

private fun formatHm(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}ч" else "${h}ч ${m}м"
}

private fun sleepQualityLabel(q: Int): String =
    listOf("плохо", "так себе", "норм", "отлично").getOrElse(q) { "норм" }

private fun oneDecimalComma(v: Float): String = "%.1f".format(v).replace('.', ',')

// Sections of the redesigned screen, in pager order.
private const val OVERVIEW = 0
private const val WEIGHT = 1
private const val SLEEP = 2
private const val STEPS = 3
private const val PAGE_COUNT = 4

private data class Tab(val title: String, val icon: String)

private val TABS = listOf(
    Tab("Общий", "widget-bold-duotone"),
    Tab("Вес", "scale-bold-duotone"),
    Tab("Сон", "moon-sleep-bold-duotone"),
    Tab("Шаги", "walking-bold-duotone"),
)

// Date windows for the period control, in days.
private enum class Period(val title: String, val days: Long) {
    WEEK("Неделя", 7),
    MONTH("Месяц", 31),
    YEAR("Год", 366),
}

// SettingsHeader (~54dp) + chips row (8+40+8). Constant height so the page
// content reserves the right top inset on frame 0.
private val HeaderHeight = 110.dp

/**
 * "Прогресс целей" — r50 redesign.
 *
 * The four sections (Общий / Вес / Сон / Шаги) are a [HorizontalPager], so
 * they can be swiped between. A floating header (back + title + icon chips)
 * sits OVER the pager with no background plate, so content scrolls visibly
 * beneath it. The chip pill slides smoothly with the pager offset, and the
 * whole pager + chips get the app's iOS-style elastic translation overscroll
 * (same feel as the Plan rings) instead of Android's stretch.
 *
 * Weight + Sleep entry are root-level bottom sheets driven by [onAddWeight] /
 * [onAddSleep] — they do NOT push onto the overlay stack, so this screen never
 * re-mounts and the active tab / scroll position / period never snap back.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProgressGoalsScreen(
    onBack: () -> Unit,
    onAddWeight: () -> Unit = {},
    onAddSleep: () -> Unit = {},
) {
    val state = LocalAppState.current
    val breakdown = calculateGoalProgress(state)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val pager = rememberPagerState(pageCount = { PAGE_COUNT })
    // One scroll state per page so each section keeps its own scroll position.
    val scrolls = List(PAGE_COUNT) { rememberScrollState() }

    // Shared elastic overscroll for the pager: horizontal translation when
    // swiping past the first/last page, vertical translation when a page is
    // pulled past its top/bottom — no system stretch deformation.
    val elastic = rememberElasticOverscroll(maxVertical = 56.dp, maxHorizontal = 44.dp)

    val topInset =
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp

    val goToPage: (Int) -> Unit = { p -> scope.launch { pager.animateScrollToPage(p) } }

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        // ── Scrolling content (pager) ──
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            Box(Modifier.fillMaxSize().nestedScroll(elastic.connection)) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = elastic.horizontalOverscroll.floatValue
                            translationY = elastic.verticalOverscroll.floatValue
                        },
                    beyondBoundsPageCount = 1,
                ) { page ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrolls[page])
                            .padding(top = topInset + HeaderHeight, bottom = 160.dp),
                    ) {
                        when (page) {
                            WEIGHT -> WeightSection(onAddWeight = onAddWeight)
                            SLEEP -> SleepSection(onAddSleep = onAddSleep)
                            STEPS -> StepsSection()
                            else -> OverviewSection(breakdown = breakdown, onSelectPage = goToPage)
                        }
                        Box(Modifier.height(24.dp))
                    }
                }
            }
        }

        // ── Floating header (back + title + chips) over the content ──
        Column(Modifier.fillMaxWidth().padding(top = topInset)) {
            SettingsHeader(title = "Прогресс целей", onBack = onBack)
            TabChips(
                // Deferred reads: the pill position is read in the DRAW phase
                // (graphicsLayer), and the active index in TabChips' own
                // composition — so swiping never recomposes this whole screen
                // (which would re-run calculateGoalProgress + every chart per frame).
                pageFraction = { pager.currentPage + pager.currentPageOffsetFraction },
                selectedPage = { pager.currentPage },
                overscrollX = { elastic.horizontalOverscroll.floatValue },
                onSelect = goToPage,
            )
        }
    }
}

// ── Floating top chips ─────────────────────────────────────────────────

/**
 * Four equal-width tab cells with a single accent pill that slides smoothly
 * with the pager. There is NO background strip — only the active pill is
 * painted, so the page content stays visible passing under the chips. The
 * whole row inherits the pager's horizontal elastic overscroll so it "pulls"
 * together with the content (the rubber-band feel, not Android's stretch).
 */
@Composable
private fun TabChips(
    pageFraction: () -> Float,
    selectedPage: () -> Int,
    overscrollX: () -> Float,
    onSelect: (Int) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .graphicsLayer { translationX = overscrollX() },
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(40.dp)) {
            val n = TABS.size
            val cell = maxWidth / n
            val inset = 4.dp

            // Sliding accent pill behind the active cell. Position is read in
            // the DRAW phase (graphicsLayer) from the pageFraction lambda, so
            // the pill follows the swipe smoothly with NO recomposition.
            Box(
                Modifier
                    .width(cell - inset * 2)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val frac = pageFraction().coerceIn(0f, (n - 1).toFloat())
                        translationX = cell.toPx() * frac + inset.toPx()
                    }
                    .clip(RoundedCornerShape(999.dp))
                    .background(Wellness.colors.accent, RoundedCornerShape(999.dp)),
            )

            val selected = selectedPage()
            Row(Modifier.fillMaxSize()) {
                TABS.forEachIndexed { i, tab ->
                    val active = i == selected
                    val fg = if (active) Color.White else Wellness.colors.muted
                    NoFeedbackButton(
                        onClick = { onSelect(i) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SolarIcon(name = tab.icon, tint = fg, size = 16.dp)
                            Box(Modifier.width(5.dp))
                            Text(
                                tab.title,
                                color = fg,
                                style = Wellness.typography.titleSmall,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Section: Общий ─────────────────────────────────────────────────────

@Composable
private fun OverviewSection(breakdown: GoalBreakdown, onSelectPage: (Int) -> Unit) {
    val state = LocalAppState.current
    val percent = (breakdown.overall * 100f).toInt()

    // Weekly weight delta from real weigh-ins.
    val weightSorted = state.weightLog.sortedBy { it.dateKey }
    val weeklyNote = run {
        val last = weightSorted.lastOrNull()
        if (last == null || weightSorted.size < 2) null
        else {
            val weekAgo = parseDate(last.dateKey)?.minusDays(7)
            val ref = weekAgo?.let { wa ->
                weightSorted.lastOrNull { e -> parseDate(e.dateKey)?.let { it <= wa } == true }
            } ?: weightSorted.first()
            val d = last.kg - ref.kg
            if (abs(d) < 0.05f) "стабильно" else "${if (d < 0) "↓" else "↑"} ${oneDecimalComma(abs(d))} кг"
        }
    }
    val weightDeltaColor =
        if ((weightSorted.lastOrNull()?.kg ?: 0f) <= (weightSorted.firstOrNull()?.kg ?: 0f))
            WellnessColors.Mint else WellnessColors.Pink

    val curWeight = weightSorted.lastOrNull()?.kg
    val lastSleep = state.sleepLog.sortedBy { it.dateKey }.lastOrNull()
    val habitsToday = state.habitsToday()
    val habitsDone = habitsToday.count { it.isDoneOn(com.wellness.app.ui.state.Dates.todayKey()) }

    HeroCard(percent = percent, weeklyNote = weeklyNote, weeklyColor = weightDeltaColor, progress = breakdown.overall)

    Box(Modifier.height(18.dp))
    SectionLabel("Разделы")
    Box(Modifier.height(10.dp))

    Column(Modifier.screenHPad(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OverviewTile(
                modifier = Modifier.weight(1f),
                icon = "scale-bold-duotone", color = Wellness.colors.accent, title = "Вес",
                value = curWeight?.let { "${oneDecimalComma(it)} кг" } ?: "—",
                sub = weeklyNote?.let { "$it за неделю" } ?: "нет записей",
                onClick = { onSelectPage(WEIGHT) },
            )
            OverviewTile(
                modifier = Modifier.weight(1f),
                icon = "moon-sleep-bold-duotone", color = WellnessColors.Water, title = "Сон",
                value = lastSleep?.let { formatHm(it.durationMinutes) } ?: "—",
                sub = lastSleep?.let { sleepQualityLabel(it.quality) } ?: "нет записей",
                onClick = { onSelectPage(SLEEP) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OverviewTile(
                modifier = Modifier.weight(1f),
                icon = "walking-bold-duotone", color = WellnessColors.Mint, title = "Шаги",
                value = "—", sub = "скоро",
                onClick = { onSelectPage(STEPS) },
            )
            OverviewTile(
                modifier = Modifier.weight(1f),
                icon = "fire-bold-duotone", color = WellnessColors.Orange, title = "Привычки",
                value = if (habitsToday.isEmpty()) "—" else "$habitsDone / ${habitsToday.size}",
                sub = "сегодня",
                onClick = null,
            )
        }
    }

    Box(Modifier.height(18.dp))
    SectionLabel("Эта неделя")
    Box(Modifier.height(10.dp))
    WeekSummaryCard(breakdown = breakdown, weeklyNote = weeklyNote, weeklyColor = weightDeltaColor)
}

@Composable
private fun HeroCard(percent: Int, weeklyNote: String?, weeklyColor: Color, progress: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = progress,
                color = Wellness.colors.accent,
                size = 92.dp,
                strokeWidth = 9.dp,
                discFillAlpha = 0.10f,
            )
            Text(
                "$percent%",
                color = Wellness.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        }
        Box(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Общий прогресс целей",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall,
            )
            Box(Modifier.height(2.dp))
            Text(
                "Так держать 💪",
                color = Wellness.colors.text,
                style = Wellness.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (weeklyNote != null) {
                Box(Modifier.height(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(weeklyColor.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        "$weeklyNote за неделю",
                        color = weeklyColor,
                        style = Wellness.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTile(
    icon: String,
    color: Color,
    title: String,
    value: String,
    sub: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Wellness.colors.container, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) { SolarIcon(name = icon, tint = color, size = 18.dp) }
                Box(Modifier.width(8.dp))
                Text(title, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
            }
            Box(Modifier.height(10.dp))
            Text(
                value,
                color = Wellness.colors.text,
                style = Wellness.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Box(Modifier.height(2.dp))
            Text(sub, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        }
    }
    if (onClick != null) {
        NoFeedbackButton(onClick = onClick, modifier = modifier) { content() }
    } else {
        Box(modifier) { content() }
    }
}

@Composable
private fun WeekSummaryCard(breakdown: GoalBreakdown, weeklyNote: String?, weeklyColor: Color) {
    val state = LocalAppState.current
    val sleep7 = state.sleepLog.sortedBy { it.dateKey }.takeLast(7)
    val avgSleep = if (sleep7.isEmpty()) null else sleep7.map { it.durationMinutes }.average().toInt()

    val rows = buildList {
        add(Triple("Средний сон", avgSleep?.let { oneDecimalComma(it / 60f) + " ч" } ?: "—", WellnessColors.Water))
        add(Triple("Изменение веса", weeklyNote ?: "—", weeklyColor))
        breakdown.habits?.let { add(Triple("Привычки сегодня", "${it.done} / ${it.total}", WellnessColors.Orange)) }
        breakdown.tasks?.let { add(Triple("Задачи", "${it.done} / ${it.total}", Wellness.colors.muted)) }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        rows.forEachIndexed { i, (label, value, color) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, color = Wellness.colors.muted, style = Wellness.typography.bodyMedium)
                Text(value, color = color, style = Wellness.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            if (i != rows.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Wellness.colors.track),
                )
            }
        }
    }
}

// ── Section: Вес ───────────────────────────────────────────────────────

@Composable
private fun WeightSection(onAddWeight: () -> Unit) {
    val state = LocalAppState.current
    var period by remember { mutableStateOf(Period.WEEK) }
    val goal = state.weightGoal

    val all = state.weightLog.sortedBy { it.dateKey }
    val cutoff = LocalDate.now().minusDays(period.days)
    val windowed = all.filter { parseDate(it.dateKey)?.let { d -> !d.isBefore(cutoff) } ?: false }
    val entries = windowed.ifEmpty { all.takeLast(1) }
    val points = entries.map { it.kg }
    val hasData = points.isNotEmpty()
    val current = points.lastOrNull() ?: state.weight
    val delta = if (points.size >= 2) points.last() - points.first() else 0f
    val avg = if (hasData) points.average().toFloat() else 0f
    val remaining = (current - goal).coerceAtLeast(0f)

    Column(Modifier.screenHPad().padding(start = 4.dp)) {
        Text("Текущий вес", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (hasData) oneDecimalComma(current) else "—",
                color = Wellness.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 44.sp,
            )
            Text(" кг", color = Wellness.colors.muted, style = Wellness.typography.titleMedium)
            if (points.size >= 2) {
                Box(Modifier.width(10.dp))
                val loss = delta < 0f
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            (if (loss) WellnessColors.Mint else WellnessColors.Pink).copy(alpha = 0.16f),
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${if (loss) "↓" else "↑"} ${oneDecimalComma(abs(delta))} кг",
                        color = if (loss) WellnessColors.Mint else WellnessColors.Pink,
                        style = Wellness.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    Box(Modifier.height(16.dp))
    PeriodControl(period = period, onSelect = { period = it })
    Box(Modifier.height(16.dp))

    if (hasData) {
        SwipeLineChart(entries = entries, goalKg = goal, period = period)
    } else {
        EmptyState("Запишите вес, чтобы увидеть динамику")
    }

    Box(Modifier.height(18.dp))
    Row(Modifier.screenHPad(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(Modifier.weight(1f), "Среднее", if (hasData) "${oneDecimalComma(avg)} кг" else "—")
        StatTile(Modifier.weight(1f), "Цель", "${oneDecimalComma(goal)} кг")
        StatTile(Modifier.weight(1f), "Осталось", "${oneDecimalComma(remaining)} кг")
    }

    Box(Modifier.height(16.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Последние записи",
                color = Wellness.colors.text,
                style = Wellness.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddWeight)
        }
        if (all.isEmpty()) {
            Box(Modifier.height(8.dp))
            Text("Пока нет записей", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        } else {
            val recent = all.reversed().take(6)
            recent.forEachIndexed { i, e ->
                if (i > 0) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Wellness.colors.track))
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        parseDate(e.dateKey)?.format(DAY_MONTH) ?: e.dateKey,
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodyMedium,
                    )
                    Text(
                        "${oneDecimalComma(e.kg)} кг",
                        color = Wellness.colors.text,
                        style = Wellness.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ── Section: Сон ───────────────────────────────────────────────────────

@Composable
private fun SleepSection(onAddSleep: () -> Unit) {
    val state = LocalAppState.current
    var period by remember { mutableStateOf(Period.WEEK) }

    val all = state.sleepLog.sortedBy { it.dateKey }
    val cutoff = LocalDate.now().minusDays(period.days)
    val windowed = all.filter { parseDate(it.dateKey)?.let { d -> !d.isBefore(cutoff) } ?: false }
    val entries = windowed.ifEmpty { all.takeLast(1) }
    val hasData = entries.isNotEmpty() && entries.any { it.durationMinutes > 0 }
    val last = all.lastOrNull()
    val avgMin = if (hasData) entries.map { it.durationMinutes }.average().toInt() else 0
    val bestMin = entries.maxOfOrNull { it.durationMinutes } ?: 0
    val goalMin = state.sleepGoalMinutes
    val goalText = if (goalMin % 60 == 0) "${goalMin / 60} ч" else formatHm(goalMin)

    Column(Modifier.screenHPad().padding(start = 4.dp)) {
        Text("Прошлой ночью", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                last?.let { formatHm(it.durationMinutes) } ?: "—",
                color = Wellness.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 44.sp,
            )
            if (last != null) {
                Box(Modifier.width(10.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(WellnessColors.Water.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        sleepQualityLabel(last.quality),
                        color = WellnessColors.Water,
                        style = Wellness.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    Box(Modifier.height(16.dp))
    PeriodControl(period = period, onSelect = { period = it })
    Box(Modifier.height(16.dp))

    if (hasData) {
        Row(
            Modifier.screenHPad().fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Длительность сна", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
            Text("цель $goalText", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        }
        Box(Modifier.height(10.dp))
        SleepBars(entries = entries, goalMin = goalMin)
    } else {
        EmptyState("Запишите сон, чтобы увидеть статистику")
    }

    Box(Modifier.height(18.dp))
    Row(Modifier.screenHPad(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(Modifier.weight(1f), "Среднее", if (hasData) oneDecimalComma(avgMin / 60f) + " ч" else "—")
        StatTile(Modifier.weight(1f), "Лучшая", if (bestMin > 0) oneDecimalComma(bestMin / 60f) + " ч" else "—")
        StatTile(Modifier.weight(1f), "Цель", goalText)
    }

    Box(Modifier.height(16.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Записать сон",
            color = Wellness.colors.text,
            style = Wellness.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddSleep)
    }
}

// ── Section: Шаги (coming soon) ────────────────────────────────────────

@Composable
private fun StepsSection() {
    Box(Modifier.height(8.dp))
    Column(
        Modifier.fillMaxWidth().screenHPad(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = 0f,
                color = WellnessColors.Mint,
                size = 132.dp,
                strokeWidth = 12.dp,
                discFillAlpha = 0.10f,
            )
            SolarIcon(name = "walking-bold-duotone", tint = WellnessColors.Mint, size = 44.dp)
        }
        Box(Modifier.height(18.dp))
        Text(
            "Шаги скоро здесь",
            color = Wellness.colors.text,
            style = Wellness.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Box(Modifier.height(6.dp))
        Text(
            "Здесь будут ваши шаги, дистанция и активность за день и за неделю.",
            color = Wellness.colors.muted,
            style = Wellness.typography.bodyMedium,
        )
    }

    Box(Modifier.height(20.dp))
    Row(Modifier.screenHPad(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(Modifier.weight(1f), "Дистанция", "—")
        StatTile(Modifier.weight(1f), "Калории", "—")
        StatTile(Modifier.weight(1f), "Активность", "—")
    }

    Box(Modifier.height(16.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(WellnessColors.Mint.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(WellnessColors.Mint.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { SolarIcon(name = "walking-bold-duotone", tint = WellnessColors.Mint, size = 22.dp) }
        Box(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Скоро: встроенный шагомер",
                color = Wellness.colors.text,
                style = Wellness.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Box(Modifier.height(2.dp))
            Text(
                "Шаги будут считаться автоматически с датчиков телефона.",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall,
            )
        }
    }
}

// ── Reusable bits ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Wellness.colors.text,
        style = Wellness.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.screenHPad().padding(start = 4.dp),
    )
}

/**
 * Segmented period control (Неделя / Месяц / Год) with a single pill that
 * slides smoothly between segments via [animateDpAsState] when the selection
 * changes.
 */
@Composable
private fun PeriodControl(period: Period, onSelect: (Period) -> Unit) {
    val items = Period.entries
    val selected = items.indexOf(period).coerceAtLeast(0)
    Box(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(14.dp))
            .background(Wellness.colors.track, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(36.dp)) {
            val n = items.size
            val cell = maxWidth / n
            val x by animateDpAsState(
                targetValue = cell * selected,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                label = "periodPill",
            )
            Box(
                Modifier
                    .offset(x = x)
                    .width(cell)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Wellness.colors.container, RoundedCornerShape(10.dp)),
            )
            Row(Modifier.fillMaxSize()) {
                items.forEach { p ->
                    val active = p == period
                    NoFeedbackButton(
                        onClick = { onSelect(p) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                p.title,
                                color = if (active) Wellness.colors.text else Wellness.colors.muted,
                                style = Wellness.typography.bodyMedium,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier, label: String, value: String) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Wellness.colors.container, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(label, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        Box(Modifier.height(4.dp))
        Text(value, color = Wellness.colors.text, style = Wellness.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        Modifier.fillMaxWidth().screenHPad().height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Wellness.colors.muted, style = Wellness.typography.bodyMedium)
    }
}

// ── Charts ─────────────────────────────────────────────────────────────

/**
 * Build a smooth (rounded) path through [pts] using a cubic bezier per
 * segment with horizontal control tangents. Gives the gentle S-curve look of
 * iOS-style line charts instead of hard polyline corners.
 */
private fun smoothPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
        val p0 = pts[i - 1]
        val p1 = pts[i]
        val cx = (p0.x + p1.x) / 2f
        path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
    }
    return path
}

/**
 * Full-bleed weight line chart with a draggable scrubber. Smooth cubic line,
 * vertical gradient fill, subtle baseline grid and a dashed goal line. Touch
 * anywhere and drag: the nearest weigh-in highlights and a floating label
 * shows its exact value + date. No card background — it sits on the page.
 */
@Composable
private fun SwipeLineChart(entries: List<WeightEntry>, goalKg: Float, period: Period) {
    val points = entries.map { it.kg }
    val color = Wellness.colors.accent
    val bg = Wellness.colors.bg
    val muted = Wellness.colors.muted
    val grid = Wellness.colors.track
    val n = points.size
    val density = LocalDensity.current

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var active by remember(entries, period) { mutableStateOf<Int?>(null) }

    val rawMin = minOf(points.min(), goalKg) - 0.6f
    val rawMax = maxOf(points.max(), goalKg) + 0.6f
    val hPadPx = with(density) { 12.dp.toPx() }

    fun xAt(i: Int, w: Float): Float {
        val usable = w - hPadPx * 2
        return if (n > 1) hPadPx + i * (usable / (n - 1)) else w / 2f
    }
    fun nearest(x: Float): Int {
        if (canvasSize.width == 0) return 0
        val w = canvasSize.width.toFloat()
        var best = 0
        var bd = Float.MAX_VALUE
        for (i in 0 until n) {
            val d = abs(xAt(i, w) - x)
            if (d < bd) { bd = d; best = i }
        }
        return best
    }

    Box(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(entries, period) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        active = nearest(down.position.x)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { ch ->
                                active = nearest(ch.position.x)
                                ch.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        active = null
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val vPad = 22.dp.toPx()
            fun yAt(v: Float): Float = vPad + (1f - (v - rawMin) / (rawMax - rawMin)) * (h - vPad * 2)

            // Subtle horizontal grid lines (top, middle, bottom of plot area).
            val plotTop = vPad
            val plotBottom = h - vPad
            listOf(0f, 0.5f, 1f).forEach { f ->
                val gy = plotTop + (plotBottom - plotTop) * f
                drawLine(
                    color = grid.copy(alpha = 0.6f),
                    start = Offset(hPadPx, gy),
                    end = Offset(w - hPadPx, gy),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // Dashed goal line.
            val goalY = yAt(goalKg)
            drawLine(
                color = muted.copy(alpha = 0.45f),
                start = Offset(hPadPx, goalY),
                end = Offset(w - hPadPx, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 7.dp.toPx()), 0f),
            )

            if (n > 1) {
                val pts = points.mapIndexed { i, v -> Offset(xAt(i, w), yAt(v)) }
                val line = smoothPath(pts)
                // Gradient area under the curve.
                val area = Path().apply {
                    addPath(line)
                    lineTo(pts.last().x, h)
                    lineTo(pts.first().x, h)
                    close()
                }
                drawPath(
                    area,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.02f)),
                        startY = plotTop,
                        endY = h,
                    ),
                )
                // Soft underlay stroke + crisp line on top.
                drawPath(line, color = color.copy(alpha = 0.18f), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
                drawPath(line, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }

            val act = active
            if (act != null && act in points.indices) {
                val ax = xAt(act, w)
                drawLine(
                    color = color.copy(alpha = 0.4f),
                    start = Offset(ax, plotTop),
                    end = Offset(ax, plotBottom),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            points.forEachIndexed { i, v ->
                val on = active == i
                val r = if (on) 6.dp.toPx() else 3.5.dp.toPx()
                if (on) {
                    drawCircle(color = color.copy(alpha = 0.18f), radius = 12.dp.toPx(), center = Offset(xAt(i, w), yAt(v)))
                }
                drawCircle(color = color, radius = r, center = Offset(xAt(i, w), yAt(v)))
                drawCircle(color = bg, radius = if (on) 2.5.dp.toPx() else 2.dp.toPx(), center = Offset(xAt(i, w), yAt(v)))
            }
        }

        // Floating value label
        val act = active
        if (act != null && act in entries.indices && canvasSize.width > 0) {
            val w = canvasSize.width.toFloat()
            val labelWidthDp = 116.dp
            val labelHalfPx = with(density) { (labelWidthDp / 2).toPx() }
            val ax = xAt(act, w)
            val clampedX = ax.coerceIn(labelHalfPx, w - labelHalfPx)
            val xDp = with(density) { (clampedX - labelHalfPx).toDp() }
            Box(
                Modifier
                    .offset(x = xDp, y = 0.dp)
                    .width(labelWidthDp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Wellness.colors.text, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "${oneDecimalComma(entries[act].kg)} кг",
                            color = Wellness.colors.bg,
                            style = Wellness.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(Modifier.height(2.dp))
                    Text(
                        parseDate(entries[act].dateKey)?.format(DAY_MONTH) ?: "",
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodySmall,
                    )
                }
            }
        }
    }

    // X-axis labels
    Box(Modifier.height(6.dp))
    if (period == Period.WEEK && n <= 7) {
        Row(
            Modifier.fillMaxWidth().screenHPad(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            entries.forEach {
                Text(shortDow(it.dateKey), color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().screenHPad(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                parseDate(entries.first().dateKey)?.format(DAY_MONTH) ?: "",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall,
            )
            Text(
                parseDate(entries.last().dateKey)?.format(DAY_MONTH) ?: "",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall,
            )
        }
    }
}

/**
 * Full-bleed sleep bars. Tap a bar to highlight it and show its duration.
 */
@Composable
private fun SleepBars(entries: List<SleepEntry>, goalMin: Int) {
    val maxMin = (entries.maxOfOrNull { it.durationMinutes } ?: goalMin)
        .coerceAtLeast(goalMin)
        .coerceAtLeast(1)
    var selected by remember(entries) { mutableStateOf(entries.lastIndex) }

    Column(Modifier.fillMaxWidth().screenHPad()) {
        if (selected in entries.indices) {
            val e = entries[selected]
            Text(
                "${shortDow(e.dateKey).ifEmpty { parseDate(e.dateKey)?.format(DAY_MONTH) ?: "" }} · ${formatHm(e.durationMinutes)}",
                color = Wellness.colors.text,
                style = Wellness.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.height(10.dp))
        }
        Row(
            Modifier.fillMaxWidth().height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            entries.forEachIndexed { i, e ->
                val fraction = (e.durationMinutes.toFloat() / maxMin).coerceIn(0f, 1f)
                val on = i == selected
                NoFeedbackButton(onClick = { selected = i }, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(24.dp)
                                .height((14 + fraction * 96).dp)
                                .background(
                                    if (on) WellnessColors.Water else WellnessColors.Water.copy(alpha = 0.35f),
                                    RoundedCornerShape(999.dp),
                                ),
                        )
                        Box(Modifier.height(6.dp))
                        Text(
                            shortDow(e.dateKey),
                            color = if (on) Wellness.colors.text else Wellness.colors.muted,
                            style = Wellness.typography.bodySmall,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
