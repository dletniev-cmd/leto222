package com.wellness.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.components.GoalProgressBar
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

/**
 * Detail screen reached by tapping the goal progress card on the
 * profile screen. Shows the overall bar plus a breakdown card per
 * sub-goal (Вес / Привычки / Задачи). Cards for sub-goals that don't
 * apply (no weight movement, no scheduled habits, no scheduled tasks)
 * are omitted entirely — better than showing a flat 0 % which would
 * mis-imply failure on a category the user hasn't engaged with.
 */
@Composable
fun ProgressGoalsScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    val b = calculateGoalProgress(state)

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        ScreenScaffold(topPadding = 0.dp) {
            SettingsHeader(title = "Прогресс целей", onBack = onBack)

            // Overall card.
            OverallCard(breakdown = b)

            Box(Modifier.height(12.dp))

            // Sub-goal cards.
            b.weight?.let {
                WeightCard(it)
                Box(Modifier.height(12.dp))
            }
            b.habits?.let {
                HabitsCard(it)
                Box(Modifier.height(12.dp))
            }
            b.tasks?.let {
                TasksCard(it)
                Box(Modifier.height(12.dp))
            }

            FormulaCard()
            Box(Modifier.height(24.dp))
        }
    }
}

// ── Cards ──────────────────────────────────────────────────────────────

@Composable
private fun OverallCard(breakdown: GoalBreakdown) {
    val percent = (breakdown.overall * 100f).toInt()
    Box(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column {
            GoalProgressBar(progress = breakdown.overall, modifier = Modifier.fillMaxWidth())
            Box(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "Общая цель достигнута на ",
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodyMedium,
                )
                Text(
                    "$percent%",
                    color = Wellness.colors.text,
                    style = Wellness.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun WeightCard(p: WeightProgress) {
    GoalCard(
        icon = "scale-outline",
        tile = WellnessColors.TileBlue,
        title = "Похудение",
        subtitle = subtitleForWeight(p),
        progress = p.progress,
    )
}

@Composable
private fun HabitsCard(p: HabitsProgress) {
    GoalCard(
        icon = "check-circle-outline",
        tile = WellnessColors.TileViolet,
        title = "Привычки",
        subtitle = "Выполнено ${p.done} из ${p.total} за неделю",
        progress = p.progress,
    )
}

@Composable
private fun TasksCard(p: TasksProgress) {
    GoalCard(
        icon = "clipboard-check-outline",
        tile = WellnessColors.TileGreen,
        title = "Задачи",
        subtitle = "Завершено ${p.done} из ${p.total} за неделю",
        progress = p.progress,
    )
}

/**
 * Shared sub-goal card layout. Tile + title/subtitle on top, % on the
 * trailing edge, mini progress bar underneath. Thinner bar (12 dp) so
 * the card height stays compact relative to the overall card.
 */
@Composable
private fun GoalCard(
    icon: String,
    tile: Color,
    title: String,
    subtitle: String,
    progress: Float,
) {
    val percent = (progress * 100f).toInt()
    Box(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(tile, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SolarIcon(name = icon, tint = Color.White, size = 20.dp)
                }
                Box(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = Wellness.colors.text,
                        style = Wellness.typography.titleSmall,
                    )
                    Text(
                        subtitle,
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodySmall,
                    )
                }
                Text(
                    "$percent%",
                    color = Wellness.colors.text,
                    style = Wellness.typography.titleSmall,
                )
            }
            Box(Modifier.height(12.dp))
            GoalProgressBar(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                thickness = 12.dp,
            )
        }
    }
}

@Composable
private fun FormulaCard() {
    Box(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SolarIcon(
                    name = "info-circle-bold-duotone",
                    tint = Wellness.colors.accent,
                    size = 18.dp,
                )
                Box(Modifier.width(8.dp))
                Text(
                    "Как считается",
                    color = Wellness.colors.text,
                    style = Wellness.typography.titleSmall,
                )
            }
            Box(Modifier.height(6.dp))
            Text(
                "Похудение даёт 60% общего прогресса, привычки — 25%, задачи — 15%. " +
                    "Если что-то из подцелей сейчас не отслеживается — веса пере" +
                    "распределяются между оставшимися.",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall,
            )
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────

private fun subtitleForWeight(p: WeightProgress): String {
    val deltaTxt = "%.1f".format(p.deltaKg).replace('.', ',')
    val totalTxt = "%.1f".format(p.totalKg).replace('.', ',')
    val current = "%.1f".format(p.currentKg).replace('.', ',')
    val goal = "%.1f".format(p.goalKg).replace('.', ',')
    return "Сброшено $deltaTxt из $totalTxt кг · сейчас $current кг → $goal кг"
}
