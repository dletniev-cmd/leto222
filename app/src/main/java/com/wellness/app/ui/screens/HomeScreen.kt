package com.wellness.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.components.NoFeedbackButton
import com.wellness.app.ui.components.ProgressRing
import com.wellness.app.ui.components.ScreenHeader
import com.wellness.app.ui.components.ScreenScaffold
import com.wellness.app.ui.components.SectionTitle
import com.wellness.app.ui.components.WCard
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.Dates
import com.wellness.app.ui.state.Habit
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.TaskItem
import com.wellness.app.ui.state.TaskStatus
import com.wellness.app.ui.theme.Wellness
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable
fun HomeScreen(@Suppress("UNUSED_PARAMETER") onAddWeight: () -> Unit = {}) {
    val state = LocalAppState.current

    // Tick once a minute so "Идёт сейчас" / "Через 1ч 35м" stays current
    // without forcing the user to navigate away and back.
    var nowMin by remember { mutableIntStateOf(LocalTime.now().toSecondOfDay() / 60) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMin = LocalTime.now().toSecondOfDay() / 60
        }
    }
    val dateKey = Dates.todayKey()
    val today = state.habitsToday()
    val tasksToday = state.tasksToday()

    ScreenScaffold {
        ScreenHeader(
            title = "Летифай",
            trailingIcon = "bell-outline",
            trailingGhost = true,
            onTrailingClick = {},
        )

        HeroCard()
        Box(Modifier.height(12.dp))

        SectionTitle("Сейчас")
        NowTaskCard(tasksToday = tasksToday, nowMin = nowMin, dateKey = dateKey)
        Box(Modifier.height(8.dp))

        val done = today.count { it.isDoneOn(dateKey) }
        SectionTitle("Привычки") {
            Text(
                if (today.isEmpty()) "—" else "$done из ${today.size}",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall
            )
        }
        HabitsList(today = today, dateKey = dateKey)
    }
}

@Composable
private fun HeroCard() {
    val state = LocalAppState.current
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = state.overallProgress(),
                color = Wellness.colors.accent,
                size = 140.dp,
                strokeWidth = 12.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(state.overallProgress() * 100).toInt()}%",
                    color = Wellness.colors.text,
                    style = Wellness.typography.displayLarge,
                )
                Text("цель дня", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
            }
        }
        Box(Modifier.width(14.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Stat(
                icon = "bottle-bold-duotone",
                color = com.wellness.app.ui.theme.WellnessColors.Water,
                value = state.waterMl.toString(),
                suffix = " / ${state.waterTarget} мл",
                label = "Вода",
                progress = state.waterMl.toFloat() / state.waterTarget,
            )
            Stat(
                icon = "donut-bitten-outline",
                color = com.wellness.app.ui.theme.WellnessColors.Cal,
                value = state.kcal.toString(),
                suffix = " / ${state.kcalTarget} ккал",
                label = "Питание",
                progress = state.kcal.toFloat() / state.kcalTarget,
            )
        }
    }
}

@Composable
private fun Stat(icon: String, color: Color, value: String, suffix: String, label: String, progress: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = progress,
                color = color,
                size = 44.dp,
                strokeWidth = 5.dp,
            )
            SolarIcon(name = icon, tint = color, size = 18.dp)
        }
        Column(Modifier.padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Wellness.colors.text, style = Wellness.typography.titleMedium)
                Text(suffix, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
            }
            Text(label, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        }
    }
}

/**
 * The "right now" hero card on the home screen. Prefer a live task, then
 * the next upcoming task, then fall back to a friendly "ничего не
 * запланировано" placeholder when the day is fully clear. The ring shows
 * progress through the current task's time window (0% at start → 100% at
 * end) so the user can glance at it and know how much of the slot is left.
 */
@Composable
private fun NowTaskCard(tasksToday: List<TaskItem>, nowMin: Int, dateKey: String) {
    val live = tasksToday.firstOrNull { it.statusAt(nowMin, dateKey) == TaskStatus.Live }
    val next = live ?: tasksToday
        .filter { it.statusAt(nowMin, dateKey) == TaskStatus.Upcoming }
        .minByOrNull { it.startMinutes }

    WCard(
        modifier = Modifier.screenHPad(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (next == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(Wellness.colors.track, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SolarIcon(name = "moon-stars-bold-duotone", tint = Wellness.colors.muted, size = 22.dp)
                }
                Box(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Свободно", color = Wellness.colors.text, style = Wellness.typography.titleMedium)
                    Text("На сегодня задач нет", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
                }
            }
            return@WCard
        }

        val isLive = next === live
        val progress = if (isLive) {
            ((nowMin - next.startMinutes).toFloat() /
                (next.endMinutes - next.startMinutes).coerceAtLeast(1)).coerceIn(0f, 1f)
        } else 0f
        val ringLabel = if (isLive) "${(next.endMinutes - nowMin).coerceAtLeast(0)}м"
                        else next.startTime

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (isLive) Wellness.colors.accentSoft else Wellness.colors.track,
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    if (isLive) {
                        LiveDot()
                        Box(Modifier.width(6.dp))
                    }
                    Text(
                        if (isLive) "Идёт сейчас" else "Следующее",
                        color = if (isLive) Wellness.colors.accent else Wellness.colors.muted,
                        style = Wellness.typography.labelMedium,
                    )
                }
                Box(Modifier.height(6.dp))
                Text(next.name, color = Wellness.colors.text, style = Wellness.typography.titleLarge)
                Box(Modifier.height(2.dp))
                Text(
                    next.statusTextAt(nowMin, dateKey),
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodySmall,
                )
            }
            Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = progress,
                    color = if (isLive) Wellness.colors.accent else next.color,
                    size = 54.dp,
                    strokeWidth = 5.dp,
                )
                Text(ringLabel, color = Wellness.colors.text, style = Wellness.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun LiveDot() {
    Box(
        Modifier
            .size(6.dp)
            .background(Wellness.colors.accent, RoundedCornerShape(999.dp))
    )
}

@Composable
private fun HabitsList(today: List<Habit>, dateKey: String) {
    val state = LocalAppState.current
    WCard(
        modifier = Modifier.screenHPad(),
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp),
    ) {
        if (today.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Сегодня свободно — добавь первую привычку в Плане",
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodySmall,
                )
            }
            return@WCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            today.forEach { h ->
                HabitRow(h, dateKey) { state.tapHabit(h.id) }
            }
        }
    }
}

@Composable
private fun HabitRow(h: Habit, dateKey: String, onTap: () -> Unit) {
    val progress = h.progressOn(dateKey)
    val done = h.isDoneOn(dateKey)
    val ringFraction = if (h.target <= 0) 0f else (progress.toFloat() / h.target).coerceIn(0f, 1f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NoFeedbackButton(onClick = onTap, modifier = Modifier.size(28.dp)) {
            // Ring + fill — fills as progress reaches target, becomes a solid
            // tick when done. For target=1 it's effectively a binary checkbox.
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = if (done) 1f else ringFraction,
                    color = if (done) h.color else h.color.copy(alpha = 0.85f),
                    size = 28.dp,
                    strokeWidth = 3.dp,
                )
                if (done) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .background(h.color, RoundedCornerShape(999.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        SolarIcon(name = "check-read-bold", tint = Color(0xFF0C1F12), size = 10.dp)
                    }
                }
            }
        }
        Box(Modifier.width(12.dp))
        Box(
            Modifier
                .size(34.dp)
                .background(h.color.copy(alpha = 0.16f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = h.icon, tint = h.color, size = 18.dp)
        }
        Box(Modifier.width(12.dp))
        Text(
            h.name,
            color = if (done) Wellness.colors.muted else Wellness.colors.text,
            style = Wellness.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            ),
            modifier = Modifier.weight(1f),
        )
        if (h.target > 1) {
            Text(
                "$progress/${h.target}",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall,
            )
        }
    }
}
