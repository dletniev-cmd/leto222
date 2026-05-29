package com.wellness.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors
import kotlin.math.abs

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
) {
    val state = LocalAppState.current
    val b = calculateGoalProgress(state)

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        ScreenScaffold(topPadding = 0.dp) {
            SettingsHeader(title = "Прогресс целей", onBack = onBack)

            HeroCard(breakdown = b)

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
private fun HeroCard(breakdown: GoalBreakdown) {
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
                    Box(Modifier.height(4.dp))
                    Text(
                        "+8% за неделю",
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodySmall,
                    )
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
    val current = weight?.currentKg ?: state.weight
    val goal = weight?.goalKg ?: state.weightGoal
    // Points are the same demo series the old TrackersScreen used. Last
    // point reflects the live weight so the "↓ 0,5" mini-delta updates
    // when the user logs a new weigh-in.
    val basePoints = listOf(78.9f, 78.6f, 78.5f, 78.7f, 78.4f, 78.2f)
    val points = basePoints + current
    val miniDelta = points.last() - points.first()  // negative = lost weight
    val avg = points.average().toFloat()
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
                        "%.1f".format(current).replace('.', ','),
                        color = Wellness.colors.text,
                        style = Wellness.typography.displayMedium,
                    )
                    Text(
                        " кг",
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodyMedium,
                    )
                    Box(Modifier.width(8.dp))
                    val isLoss = miniDelta < 0f
                    Text(
                        text = "${if (isLoss) "↓" else "↑"} ${"%.1f".format(abs(miniDelta)).replace('.', ',')}",
                        color = if (isLoss) WellnessColors.Mint else WellnessColors.Pink,
                        style = Wellness.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddWeight)
        }
        Box(Modifier.height(10.dp))
        WeightChart(points = points, goalKg = goal)
        Box(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach {
                Text(it, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
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
                    "${"%.1f".format(avg).replace('.', ',')} кг",
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
                    "${"%.1f".format(remaining).replace('.', ',')} кг",
                    color = Wellness.colors.text,
                    style = Wellness.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Sleep tracker ─────────────────────────────────────────────────────

@Composable
private fun SleepTrackerCard(onAddSleep: () -> Unit) {
    val state = LocalAppState.current
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
                    "7ч 24м",
                    color = Wellness.colors.text,
                    style = Wellness.typography.displayMedium,
                )
            }
            Box(
                Modifier
                    .background(WellnessColors.Water.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text("отлично", color = WellnessColors.Water, style = Wellness.typography.labelMedium)
            }
            Box(Modifier.width(8.dp))
            IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddSleep)
        }
        Box(Modifier.height(14.dp))
        SleepBars()
        Box(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row {
                Text(
                    "Среднее ",
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodySmall,
                )
                Text(
                    "7,1 ч",
                    color = Wellness.colors.text,
                    style = Wellness.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text("цель 8 ч", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        }
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
        val stepX = w / (n - 1)
        // Dashed goal line.
        val goalY = h - ((goalKg - min) / (max - min)) * h
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
        // Build series path.
        val path = Path()
        points.forEachIndexed { i, v ->
            val nx = i * stepX
            val ny = h - ((v - min) / (max - min)) * h
            if (i == 0) path.moveTo(nx, ny) else path.lineTo(nx, ny)
        }
        val area = Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(area, color = color.copy(alpha = 0.10f))
        drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        points.forEachIndexed { i, v ->
            val nx = i * stepX
            val ny = h - ((v - min) / (max - min)) * h
            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(nx, ny))
            drawCircle(color = bg, radius = 1.5.dp.toPx(), center = Offset(nx, ny))
        }
    }
}

@Composable
private fun SleepBars() {
    val state = LocalAppState.current
    Row(
        Modifier.fillMaxWidth().height(110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        state.sleep.forEach { d ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(22.dp)
                        .height((20 + (d.height * 80)).dp)
                        .background(
                            if (d.highlighted) WellnessColors.Water else WellnessColors.Water.copy(alpha = 0.35f),
                            RoundedCornerShape(999.dp),
                        ),
                )
                Box(Modifier.height(6.dp))
                Text(d.label, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
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
