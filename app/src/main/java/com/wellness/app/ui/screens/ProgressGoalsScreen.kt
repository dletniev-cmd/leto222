package com.wellness.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wellness.app.ui.components.IconButtonRound
import com.wellness.app.ui.components.ScreenScaffold
import com.wellness.app.ui.components.SettingsHeader
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.GoalBreakdown
import com.wellness.app.ui.state.HabitsProgress
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.TasksProgress
import com.wellness.app.ui.state.WeightProgress
import com.wellness.app.ui.state.calculateGoalProgress
import com.wellness.app.ui.state.SleepEntry
import com.wellness.app.ui.state.WeightEntry
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors
import java.time.LocalDate
import kotlin.math.abs

// ── Shared helpers for the real trackers ───────────────────────────────

private val SHORT_DOW = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

private fun shortDow(dateKey: String): String =
    runCatching { SHORT_DOW[LocalDate.parse(dateKey).dayOfWeek.value - 1] }.getOrDefault("")

private fun formatHm(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}ч" else "${h}ч ${m}м"
}

private fun sleepQualityLabel(q: Int): String =
    listOf("плохо", "так себе", "норм", "отлично").getOrElse(q) { "норм" }

private fun oneDecimalComma(v: Float): String = "%.1f".format(v).replace('.', ',')

/**
 * Detail screen reached by tapping the goal progress bar on the
 * profile screen. r41 redesign — Variant B:
 *  - Hero card with a big overall %, weekly delta and three compact
 *    sub-goal chips (Похудение / Привычки / Задачи).
 *  - "Трекеры" section housing the Weight and Sleep trackers that
 *    previously lived behind the now-removed Trackers tab. Tapping the
 *    "+" buttons opens the existing add-weight / add-sleep overlays.
 *  - A single compact "Как считается" plate at the bottom.
 */
@Composable
fun ProgressGoalsScreen(
    onBack: () -> Unit,
    onAddWeight: () -> Unit = {},
    onAddSleep: () -> Unit = {},
    scrollState: ScrollState? = null,
) {
    val state = LocalAppState.current
    val b = calculateGoalProgress(state)

    // Real weekly weight change, derived from logged weigh-ins. Shown as the
    // sub-line under the big % (replaces the old hardcoded "+8% за неделю").
    // Hidden when there aren't at least two weigh-ins to compare.
    val weeklyNote = run {
        val sorted = state.weightLog.sortedBy { it.dateKey }
        val last = sorted.lastOrNull()
        if (last == null || sorted.size < 2) {
            null
        } else {
            val weekAgo = LocalDate.parse(last.dateKey).minusDays(7)
            val ref = sorted.lastOrNull { LocalDate.parse(it.dateKey) <= weekAgo }
                ?: sorted.first()
            val d = last.kg - ref.kg
            if (abs(d) < 0.05f) "стабильно за неделю"
            else "${if (d < 0) "↓" else "↑"} ${oneDecimalComma(abs(d))} кг за неделю"
        }
    }

    // The scroll position is HOISTED by the caller (WellnessApp) and the
    // SAME ScrollState instance is handed to both the "top overlay" and the
    // "underlay" rendering of this screen. Without that, opening the weight /
    // sleep adder moved this screen from the top slot to the underlay slot,
    // which created a fresh scroll state and snapped the page back to the top.
    val scroll = scrollState ?: rememberScrollState()

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        ScreenScaffold(
            scrollState = scroll,
            topPadding = 0.dp,
            pinnedHeader = {
                SettingsHeader(title = "Прогресс целей", onBack = onBack)
            },
            // SettingsHeader is a fixed-height bar (44dp back button + 4/6
            // vertical padding = 54dp). Passing it lets the scaffold skip
            // the SubcomposeLayout and compose this (chart-heavy) screen on a
            // lighter first frame, so the slide-in no longer stutters.
            pinnedHeaderHeight = 54.dp,
        ) {
            HeroCard(breakdown = b, weeklyNote = weeklyNote)

            Box(Modifier.height(20.dp))
            TrackersHeader()
            Box(Modifier.height(10.dp))

            WeightTrackerCard(weight = b.weight, onAddWeight = onAddWeight)
            Box(Modifier.height(12.dp))
            SleepTrackerCard(onAddSleep = onAddSleep)

            Box(Modifier.height(20.dp))
            FormulaPlate()
            Box(Modifier.height(24.dp))
        }
    }
}

// ── Hero (Big % + chips) ───────────────────────────────────────────────

@Composable
private fun HeroCard(breakdown: GoalBreakdown, weeklyNote: String?) {
    val percent = (breakdown.overall * 100f).toInt()
    val gradient = Brush.linearGradient(
        colors = listOf(
            WellnessColors.Purple.copy(alpha = 0.22f),
            WellnessColors.Water.copy(alpha = 0.10f),
            Color.Transparent,
        ),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(24.dp))
            .background(Wellness.colors.container, RoundedCornerShape(24.dp))
            .background(gradient, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Column {
            // Top row: big % + mini ring.
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Общая цель",
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodySmall,
                    )
                    Box(Modifier.height(4.dp))
                    Text(
                        "$percent%",
                        color = Wellness.colors.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.sp,
                        lineHeight = 56.sp,
                    )
                    if (weeklyNote != null) {
                        Box(Modifier.height(4.dp))
                        Text(
                            weeklyNote,
                            color = Wellness.colors.muted,
                            style = Wellness.typography.bodySmall,
                        )
                    }
                }
                MiniRing(progress = breakdown.overall)
            }
            Box(Modifier.height(14.dp))
            // Three compact stat chips.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(
                    label = "ПОХУДЕНИЕ",
                    value = breakdown.weight?.let { "${(it.progress * 100).toInt()}%" } ?: "—",
                    delta = breakdown.weight?.let { weightDelta(it) },
                    deltaColor = breakdown.weight?.let { weightDeltaColor(it) }
                        ?: Wellness.colors.muted,
                    modifier = Modifier.weight(1f),
                )
                StatChip(
                    label = "ПРИВЫЧКИ",
                    value = breakdown.habits?.let { "${(it.progress * 100).toInt()}%" } ?: "—",
                    delta = breakdown.habits?.let { habitsDelta(it) },
                    deltaColor = WellnessColors.Mint,
                    modifier = Modifier.weight(1f),
                )
                StatChip(
                    label = "ЗАДАЧИ",
                    value = breakdown.tasks?.let { "${(it.progress * 100).toInt()}%" } ?: "—",
                    delta = breakdown.tasks?.let { "${it.done} / ${it.total}" },
                    deltaColor = Wellness.colors.muted,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MiniRing(progress: Float) {
    val accent = Wellness.colors.accent
    val track = Wellness.colors.track
    Canvas(Modifier.size(52.dp)) {
        val stroke = 6.dp.toPx()
        val inset = stroke / 2f
        val rect = androidx.compose.ui.geometry.Rect(
            left = inset,
            top = inset,
            right = size.width - inset,
            bottom = size.height - inset,
        )
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = accent,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    delta: String?,
    deltaColor: Color,
    modifier: Modifier = Modifier,
) {
    // Chip fill is theme-aware: on dark a translucent black sinks the
    // chip into the gradient; on light a black 32% box would read as an
    // ugly grey slab over the white card, so we use a much softer scrim.
    val chipBg = if (Wellness.colors.isDark) {
        Color.Black.copy(alpha = 0.32f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(chipBg, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            color = Wellness.colors.muted,
            fontSize = 10.5.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.6.sp,
        )
        Box(Modifier.height(4.dp))
        Text(
            value,
            color = Wellness.colors.text,
            style = Wellness.typography.titleSmall,
        )
        if (delta != null) {
            Box(Modifier.height(2.dp))
            Text(
                delta,
                color = deltaColor,
                fontSize = 11.sp,
                lineHeight = 13.sp,
            )
        }
    }
}

private fun weightDelta(p: WeightProgress): String {
    val d = p.deltaKg
    val sign = if (d >= 0f) "↓" else "↑"
    return "$sign ${"%.1f".format(abs(d)).replace('.', ',')} кг"
}

private fun weightDeltaColor(p: WeightProgress): Color =
    if (p.deltaKg >= 0f) WellnessColors.Mint else WellnessColors.Pink

private fun habitsDelta(p: HabitsProgress): String = "${p.done} / ${p.total}"

// ── Section header ────────────────────────────────────────────────────

@Composable
private fun TrackersHeader() {
    Row(
        Modifier.fillMaxWidth().screenHPad(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            "Трекеры",
            color = Wellness.colors.text,
            style = Wellness.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            "неделя",
            color = Wellness.colors.muted,
            style = Wellness.typography.bodySmall,
        )
    }
}

// ── Weight tracker ────────────────────────────────────────────────────

@Composable
private fun WeightTrackerCard(weight: WeightProgress?, onAddWeight: () -> Unit) {
    val state = LocalAppState.current
    val goal = weight?.goalKg ?: state.weightGoal

    // Real weigh-ins, chronological, last 7. Everything below is derived
    // from what the user actually logged — no demo series.
    val entries: List<WeightEntry> = state.weightLog
        .sortedBy { it.dateKey }
        .takeLast(7)
    val points = entries.map { it.kg }
    val hasData = points.isNotEmpty()
    val current = points.lastOrNull() ?: weight?.currentKg ?: state.weight
    val miniDelta = if (points.size >= 2) points.last() - points.first() else 0f
    val avg = if (hasData) points.average().toFloat() else 0f
    val remaining = (current - goal).coerceAtLeast(0f)

    Column(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(WellnessColors.Purple.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { SolarIcon(name = "scale-bold-duotone", tint = WellnessColors.Purple, size = 22.dp) }
            Box(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Вес сейчас",
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodySmall,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (hasData) oneDecimalComma(current) else "—",
                        color = Wellness.colors.text,
                        style = Wellness.typography.displayMedium,
                    )
                    Text(
                        " кг",
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodyMedium,
                    )
                    if (points.size >= 2) {
                        Box(Modifier.width(8.dp))
                        val isLoss = miniDelta < 0f
                        Text(
                            text = "${if (isLoss) "↓" else "↑"} ${oneDecimalComma(abs(miniDelta))}",
                            color = if (isLoss) WellnessColors.Mint else WellnessColors.Pink,
                            style = Wellness.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddWeight)
        }
        Box(Modifier.height(10.dp))
        if (hasData) {
            WeightChart(points = points, goalKg = goal)
            Box(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                entries.forEach {
                    Text(
                        shortDow(it.dateKey),
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodySmall,
                    )
                }
            }
            Box(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    Text(
                        "Среднее ",
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodySmall,
                    )
                    Text(
                        "${oneDecimalComma(avg)} кг",
                        color = Wellness.colors.text,
                        style = Wellness.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row {
                    Text(
                        "осталось ",
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodySmall,
                    )
                    Text(
                        "${oneDecimalComma(remaining)} кг",
                        color = Wellness.colors.text,
                        style = Wellness.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        } else {
            TrackerEmptyState("Запишите вес, чтобы увидеть динамику")
        }
    }
}

// ── Sleep tracker ─────────────────────────────────────────────────────

@Composable
private fun SleepTrackerCard(onAddSleep: () -> Unit) {
    val state = LocalAppState.current

    // Real sleep history, chronological, last 7.
    val entries: List<SleepEntry> = state.sleepLog
        .sortedBy { it.dateKey }
        .takeLast(7)
    val hasData = entries.isNotEmpty()
    val last = entries.lastOrNull()
    val avgMin = if (hasData) entries.map { it.durationMinutes }.average().toInt() else 0
    val goalMin = state.sleepGoalMinutes
    val goalHoursText = if (goalMin % 60 == 0) "${goalMin / 60} ч" else formatHm(goalMin)

    Column(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(WellnessColors.Water.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { SolarIcon(name = "moon-sleep-bold-duotone", tint = WellnessColors.Water, size = 22.dp) }
            Box(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Прошлой ночью",
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodySmall,
                )
                Text(
                    last?.let { formatHm(it.durationMinutes) } ?: "—",
                    color = Wellness.colors.text,
                    style = Wellness.typography.displayMedium,
                )
            }
            if (last != null) {
                Box(
                    Modifier
                        .background(WellnessColors.Water.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        sleepQualityLabel(last.quality),
                        color = WellnessColors.Water,
                        style = Wellness.typography.labelMedium,
                    )
                }
                Box(Modifier.width(8.dp))
            }
            IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddSleep)
        }
        Box(Modifier.height(14.dp))
        if (hasData) {
            SleepBars(entries = entries, goalMin = goalMin)
            Box(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    Text(
                        "Среднее ",
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodySmall,
                    )
                    Text(
                        oneDecimalComma(avgMin / 60f) + " ч",
                        color = Wellness.colors.text,
                        style = Wellness.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "цель $goalHoursText",
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodySmall,
                )
            }
        } else {
            TrackerEmptyState("Запишите сон, чтобы увидеть статистику")
        }
    }
}

@Composable
private fun TrackerEmptyState(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(110.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
    }
}

// ── Charts ────────────────────────────────────────────────────────────

@Composable
private fun WeightChart(points: List<Float>, goalKg: Float) {
    val color = WellnessColors.Purple
    val bg = Wellness.colors.container
    val muted = Wellness.colors.muted
    // Range includes the goal line so the dashed marker always sits on
    // the canvas — otherwise an aggressive goal would clip below.
    val rawMin = minOf(points.min(), goalKg) - 0.6f
    val rawMax = maxOf(points.max(), goalKg) + 0.6f
    val min = rawMin
    val max = rawMax
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val w = size.width
        val h = size.height
        val n = points.size
        if (n == 0) return@Canvas
        // With a single weigh-in there's no line — anchor the lone point in
        // the middle of the canvas so it still reads sensibly.
        fun xAt(i: Int): Float = if (n > 1) i * (w / (n - 1)) else w / 2f
        fun yAt(v: Float): Float = h - ((v - min) / (max - min)) * h
        // Dashed goal line.
        val goalY = yAt(goalKg)
        drawLine(
            color = muted.copy(alpha = 0.35f),
            start = Offset(0f, goalY),
            end = Offset(w, goalY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                0f,
            ),
        )
        if (n > 1) {
            // Build series path + filled area.
            val path = Path()
            points.forEachIndexed { i, v ->
                if (i == 0) path.moveTo(xAt(i), yAt(v)) else path.lineTo(xAt(i), yAt(v))
            }
            val area = Path().apply {
                addPath(path)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(area, color = color.copy(alpha = 0.10f))
            drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        points.forEachIndexed { i, v ->
            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(xAt(i), yAt(v)))
            drawCircle(color = bg, radius = 1.5.dp.toPx(), center = Offset(xAt(i), yAt(v)))
        }
    }
}

@Composable
private fun SleepBars(entries: List<SleepEntry>, goalMin: Int) {
    // Scale bars against the larger of the goal and the longest night so a
    // night that beats the goal still fits inside the 0..1 fraction.
    val maxMin = (entries.maxOfOrNull { it.durationMinutes } ?: goalMin)
        .coerceAtLeast(goalMin)
        .coerceAtLeast(1)
    val lastIndex = entries.lastIndex
    Row(
        Modifier.fillMaxWidth().height(110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        entries.forEachIndexed { i, e ->
            val fraction = (e.durationMinutes.toFloat() / maxMin).coerceIn(0f, 1f)
            val highlighted = i == lastIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(22.dp)
                        .height((20 + (fraction * 80)).dp)
                        .background(
                            if (highlighted) WellnessColors.Water else WellnessColors.Water.copy(alpha = 0.35f),
                            RoundedCornerShape(999.dp),
                        ),
                )
                Box(Modifier.height(6.dp))
                Text(
                    shortDow(e.dateKey),
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodySmall,
                )
            }
        }
    }
}

// ── Formula plate ─────────────────────────────────────────────────────

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
        SolarIcon(
            name = "info-circle-bold-duotone",
            tint = Wellness.colors.accent,
            size = 20.dp,
        )
        Box(Modifier.width(10.dp))
        Text(
            "Как считается прогресс",
            color = Wellness.colors.text,
            style = Wellness.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        SolarIcon(
            name = "alt-arrow-right-outline",
            tint = Wellness.colors.muted,
            size = 18.dp,
        )
    }
}
