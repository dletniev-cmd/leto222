package com.wellness.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.components.IconButtonRound
import com.wellness.app.ui.components.ScreenHeader
import com.wellness.app.ui.components.ScreenScaffold
import com.wellness.app.ui.components.SectionTitle
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors

@Composable
fun TrackersScreen(
    onAddWeight: () -> Unit = {},
    onAddSleep: () -> Unit = {},
) {
    val state = LocalAppState.current
    ScreenScaffold {
        ScreenHeader(title = "Трекеры")

        // Weight block — the metric header AND the chart live inside one
        // rounded container so the chart visually "belongs" to its data
        // block instead of floating on the bare page.
        Column(
            Modifier
                .fillMaxWidth()
                .screenHPad()
                .background(Wellness.colors.container, RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).background(WellnessColors.Purple.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) { SolarIcon(name = "scale-outline", tint = WellnessColors.Purple, size = 22.dp) }
                Box(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Вес", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%.1f".format(state.weight), color = Wellness.colors.text, style = Wellness.typography.displayMedium)
                        Text(" кг", color = Wellness.colors.muted, style = Wellness.typography.bodyMedium)
                    }
                }
                IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddWeight)
            }
            Box(Modifier.height(10.dp))
            WeightChart()
            Box(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("16", "17", "18", "19", "20", "21", "22").forEach {
                    Text(it, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
                }
            }
            Box(Modifier.height(6.dp))
            Text(
                "Цель ${"%.1f".format(state.weightGoal)} кг · осталось ${"%.1f".format(state.weight - state.weightGoal)} кг",
                color = Wellness.colors.muted, style = Wellness.typography.bodySmall,
            )
        }

        // Sleep block — same unified-container treatment.
        SectionTitle("Сон") {
            IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddSleep)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .screenHPad()
                .background(Wellness.colors.container, RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).background(WellnessColors.Water.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) { SolarIcon(name = "moon-sleep-bold-duotone", tint = WellnessColors.Water, size = 22.dp) }
                Box(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Прошлой ночью", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("7ч 24м", color = Wellness.colors.text, style = Wellness.typography.displayMedium)
                    }
                }
                Box(
                    Modifier
                        .background(WellnessColors.Water.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("отлично", color = WellnessColors.Water, style = Wellness.typography.labelMedium)
                }
            }
            Box(Modifier.height(14.dp))
            SleepBars()
        }
    }
}

@Composable
private fun WeightChart() {
    val color = WellnessColors.Purple
    // The chart now sits inside the weight tracker's container, so the
    // marker's inner dot is punched out with the container colour (not the
    // page background) so it stays a clean hole on both dark and light.
    val bg = Wellness.colors.container
    val points = listOf(78.9f, 78.6f, 78.5f, 78.7f, 78.4f, 78.2f, 78.4f)
    val min = points.min() - 0.6f
    val max = points.max() + 0.6f
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val w = size.width
        val h = size.height
        val n = points.size
        val stepX = w / (n - 1)
        // Build path
        val path = Path()
        points.forEachIndexed { i, v ->
            val nx = i * stepX
            val ny = h - ((v - min) / (max - min)) * h
            if (i == 0) path.moveTo(nx, ny) else path.lineTo(nx, ny)
        }
        // Filled area
        val area = Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(area, color = color.copy(alpha = 0.10f))
        drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        // Points
        points.forEachIndexed { i, v ->
            val nx = i * stepX
            val ny = h - ((v - min) / (max - min)) * h
            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(nx, ny))
            // Inner dot uses the page bg so the marker reads correctly on both
            // light and dark themes (was hard-coded white).
            drawCircle(color = bg, radius = 1.5.dp.toPx(), center = Offset(nx, ny))
        }
    }
}

@Composable
private fun SleepBars() {
    val state = LocalAppState.current
    Row(
        Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        state.sleep.forEach { d ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(18.dp)
                        .height((20 + (d.height * 90)).dp)
                        .background(
                            if (d.highlighted) WellnessColors.Water else WellnessColors.Water.copy(alpha = 0.35f),
                            RoundedCornerShape(999.dp),
                        )
                )
                Box(Modifier.height(6.dp))
                Text(d.label, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
            }
        }
    }
}
