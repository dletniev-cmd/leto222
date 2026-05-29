package com.wellness.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wellness.app.ui.components.IconButtonRound
import com.wellness.app.ui.components.NoFeedbackButton
import com.wellness.app.ui.components.ProgressRing
import com.wellness.app.ui.components.ScreenScaffold
import com.wellness.app.ui.components.SettingsHeader
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

// Sections of the redesigned screen.
private const val OVERVIEW = "overview"
private const val WEIGHT = "weight"
private const val SLEEP = "sleep"
private const val STEPS = "steps"

private data class Tab(val key: String, val title: String, val icon: String)

private val TABS = listOf(
    Tab(OVERVIEW, "Общий", "widget-bold-duotone"),
    Tab(WEIGHT, "Вес", "scale-bold-duotone"),
    Tab(SLEEP, "Сон", "moon-sleep-bold-duotone"),
    Tab(STEPS, "Шаги", "walking-bold-duotone"),
)

// Date windows for the period control, in days.
private enum class Period(val key: String, val title: String, val days: Long) {
    WEEK("week", "Неделя", 7),
    MONTH("month", "Месяц", 31),
    YEAR("year", "Год", 366),
}

/**
 * "Прогресс целей" — r49 redesign.
 *
 * Top floating icon-chips (Общий / Вес / Сон / Шаги) sit OVER the scrolling
 * content with no background plate, so content is visible passing under them
 * (the [ScreenScaffold] pinned-header path renders the header transparent and
 * scrolls the body beneath it). Each section is its own composable:
 *  - Общий: overall ring + tappable section tiles + weekly summary.
 *  - Вес: big current weight (no container), period control, a full-bleed
 *    swipeable line chart (drag to scrub exact values), stat tiles, log.
 *  - Сон: big last-night value, period control, full-bleed bar chart, stats.
 *  - Шаги: "coming soon" — no pedometer data source yet, so we show an honest
 *    empty state instead of fabricated step counts.
 */
@Composable
fun ProgressGoalsScreen(
    onBack: () -> Unit,
    onAddWeight: () -> Unit = {},
    onAddSleep: () -> Unit = {},
    scrollState: ScrollState? = null,
    // Hoisted by the host so the active tab survives the screen moving between
    // its top and underlay slots (opening the sleep adder). Falls back to a
    // local state when rendered standalone.
    sectionState: androidx.compose.runtime.MutableState<String>? = null,
) {
    val state = LocalAppState.current
    val breakdown = calculateGoalProgress(state)
    val sectionHolder = sectionState ?: remember { mutableStateOf(OVERVIEW) }
    var section by sectionHolder

    // One scroll state per section so switching tabs starts each at the top
    // rather than inheriting a deep scroll from another, taller section.
    val scrollOverview = rememberScrollState()
    val scrollWeight = rememberScrollState()
    val scrollSleep = rememberScrollState()
    val scrollSteps = rememberScrollState()
    val activeScroll = when (section) {
        WEIGHT -> scrollWeight
        SLEEP -> scrollSleep
        STEPS -> scrollSteps
        else -> scrollState ?: scrollOverview
    }

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        ScreenScaffold(
            scrollState = activeScroll,
            topPadding = 0.dp,
            pinnedHeader = {
                Column {
                    SettingsHeader(title = "Прогресс целей", onBack = onBack)
                    TabChips(selected = section, onSelect = { section = it })
                }
            },
            // SettingsHeader (~54dp) + chips row (~56dp incl. spacing). Constant
            // height keeps the jank-free fast path in the scaffold.
            pinnedHeaderHeight = 110.dp,
        ) {
            when (section) {
                WEIGHT -> WeightSection(onAddWeight = onAddWeight)
                SLEEP -> SleepSection(onAddSleep = onAddSleep)
                STEPS -> StepsSection()
                else -> OverviewSection(breakdown = breakdown, onSelectSection = { section = it })
            }
            Box(Modifier.height(24.dp))
        }
    }
}

// ── Floating top chips ─────────────────────────────────────────────────

@Composable
private fun TabChips(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TABS.forEach { tab ->
            val active = tab.key == selected
            // Active = solid accent pill with white content. Inactive = soft
            // track pill. No strip behind the row, so content scrolls under.
            val bg = if (active) Wellness.colors.accent else Wellness.colors.track
            val fg = if (active) Color.White else Wellness.colors.muted
            NoFeedbackButton(onClick = { onSelect(tab.key) }) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(bg, RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SolarIcon(name = tab.icon, tint = fg, size = 17.dp)
                    Box(Modifier.width(6.dp))
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

// ── Section: Общий ─────────────────────────────────────────────────────

@Composable
private fun OverviewSection(breakdown: GoalBreakdown, onSelectSection: (String) -> Unit) {
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

    // Tile values from real data.
    val curWeight = weightSorted.lastOrNull()?.kg
    val lastSleep = state.sleepLog.sortedBy { it.dateKey }.lastOrNull()
    val habitsToday = state.habitsToday()
    val habitsDone = habitsToday.count { it.isDoneOn(com.wellness.app.ui.state.Dates.todayKey()) }

    // Hero
    HeroCard(percent = percent, weeklyNote = weeklyNote, weeklyColor = weightDeltaColor, progress = breakdown.overall)

    Box(Modifier.height(18.dp))
    SectionLabel("Разделы")
    Box(Modifier.height(10.dp))

    // 2×2 tiles
    Column(Modifier.screenHPad(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OverviewTile(
                modifier = Modifier.weight(1f),
                icon = "scale-bold-duotone", color = Wellness.colors.accent, title = "Вес",
                value = curWeight?.let { "${oneDecimalComma(it)} кг" } ?: "—",
                sub = weeklyNote?.let { "$it за неделю" } ?: "нет записей",
                onClick = { onSelectSection(WEIGHT) },
            )
            OverviewTile(
                modifier = Modifier.weight(1f),
                icon = "moon-sleep-bold-duotone", color = WellnessColors.Water, title = "Сон",
                value = lastSleep?.let { formatHm(it.durationMinutes) } ?: "—",
                sub = lastSleep?.let { sleepQualityLabel(it.quality) } ?: "нет записей",
                onClick = { onSelectSection(SLEEP) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OverviewTile(
                modifier = Modifier.weight(1f),
                icon = "walking-bold-duotone", color = WellnessColors.Mint, title = "Шаги",
                value = "—", sub = "скоро",
                onClick = { onSelectSection(STEPS) },
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

    Box(Modifier.height(18.dp))
    FormulaPlate()
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

    // Big number, NOT in a container.
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
        // Full-bleed swipeable chart — NOT inside a card.
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
    // Recent weigh-ins.
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

@Composable
private fun PeriodControl(period: Period, onSelect: (Period) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(14.dp))
            .background(Wellness.colors.track, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Period.entries.forEach { p ->
            val active = p == period
            NoFeedbackButton(onClick = { onSelect(p) }, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (active) Wellness.colors.container else Color.Transparent,
                            RoundedCornerShape(10.dp),
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
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

@Composable
private fun FormulaPlate() {
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(20.dp))
            .background(Wellness.colors.container, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SolarIcon(name = "info-circle-bold-duotone", tint = Wellness.colors.accent, size = 20.dp)
        Box(Modifier.width(10.dp))
        Text(
            "Как считается прогресс",
            color = Wellness.colors.text,
            style = Wellness.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        SolarIcon(name = "alt-arrow-right-outline", tint = Wellness.colors.muted, size = 18.dp)
    }
}

// ── Charts ─────────────────────────────────────────────────────────────

/**
 * Full-bleed weight line chart with a draggable scrubber. Touch anywhere and
 * drag: the nearest weigh-in highlights and a floating label shows its exact
 * value + date. No card background — it sits directly on the page.
 */
@Composable
private fun SwipeLineChart(entries: List<WeightEntry>, goalKg: Float, period: Period) {
    val points = entries.map { it.kg }
    val color = Wellness.colors.accent
    val bg = Wellness.colors.bg
    val muted = Wellness.colors.muted
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
                .height(160.dp)
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
            val vPad = 18.dp.toPx()
            fun yAt(v: Float): Float = vPad + (1f - (v - rawMin) / (rawMax - rawMin)) * (h - vPad * 2)

            // Goal line
            val goalY = yAt(goalKg)
            drawLine(
                color = muted.copy(alpha = 0.35f),
                start = Offset(hPadPx, goalY),
                end = Offset(w - hPadPx, goalY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()), 0f),
            )

            if (n > 1) {
                val path = Path()
                points.forEachIndexed { i, v ->
                    if (i == 0) path.moveTo(xAt(i, w), yAt(v)) else path.lineTo(xAt(i, w), yAt(v))
                }
                val area = Path().apply {
                    addPath(path)
                    lineTo(xAt(n - 1, w), h)
                    lineTo(xAt(0, w), h)
                    close()
                }
                drawPath(area, color = color.copy(alpha = 0.12f))
                drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }

            val act = active
            if (act != null && act in points.indices) {
                val ax = xAt(act, w)
                drawLine(
                    color = color.copy(alpha = 0.4f),
                    start = Offset(ax, vPad),
                    end = Offset(ax, h - vPad),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            points.forEachIndexed { i, v ->
                val r = if (active == i) 6.dp.toPx() else 3.5.dp.toPx()
                drawCircle(color = color, radius = r, center = Offset(xAt(i, w), yAt(v)))
                drawCircle(color = bg, radius = 2.dp.toPx(), center = Offset(xAt(i, w), yAt(v)))
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
