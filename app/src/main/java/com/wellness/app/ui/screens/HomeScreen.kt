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
import com.wellness.app.ui.state.Habit
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.theme.Wellness

@Composable
fun HomeScreen(@Suppress("UNUSED_PARAMETER") onAddWeight: () -> Unit = {}) {
    val state = LocalAppState.current
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
        NowTaskCard()
        Box(Modifier.height(8.dp))

        val done = state.todayHabits.count { it.done }
        SectionTitle("Привычки") {
            Text(
                "$done из ${state.todayHabits.size}",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall
            )
        }
        HabitsList()
    }
}

@Composable
private fun HeroCard() {
    val state = LocalAppState.current
    // Big ring lives directly on the background — no container plate. Stats
    // sit in a separate, gentler card to the right so the ring can breathe.
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

@Composable
private fun NowTaskCard() {
    val state = LocalAppState.current
    val live = state.tasks.firstOrNull { it.status == com.wellness.app.ui.state.TaskStatus.Live } ?: state.tasks.first()
    WCard(
        modifier = Modifier.screenHPad(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Wellness.colors.accentSoft, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    LiveDot()
                    Box(Modifier.width(6.dp))
                    Text("Идёт сейчас", color = Wellness.colors.accent, style = Wellness.typography.labelMedium)
                }
                Box(Modifier.height(6.dp))
                Text(live.name, color = Wellness.colors.text, style = Wellness.typography.titleLarge)
                Box(Modifier.height(2.dp))
                Text("До конца 23 мин", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
            }
            Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = 0.55f,
                    color = Wellness.colors.accent,
                    size = 54.dp,
                    strokeWidth = 5.dp,
                )
                Text("23м", color = Wellness.colors.text, style = Wellness.typography.labelLarge)
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
private fun HabitsList() {
    val state = LocalAppState.current
    WCard(
        modifier = Modifier.screenHPad(),
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            state.todayHabits.forEachIndexed { idx, h ->
                HabitRow(h) {
                    state.todayHabits[idx] = h.copy(done = !h.done)
                }
            }
        }
    }
}

@Composable
private fun HabitRow(h: Habit, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NoFeedbackButton(onClick = onToggle, modifier = Modifier.size(22.dp)) {
            Box(
                Modifier
                    .size(22.dp)
                    .background(
                        if (h.done) Wellness.colors.accent else Wellness.colors.track,
                        RoundedCornerShape(999.dp)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (h.done) SolarIcon(name = "check-read-bold", tint = Color(0xFF0C1F12), size = 14.dp)
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
            color = if (h.done) Wellness.colors.muted else Wellness.colors.text,
            style = Wellness.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (h.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            "${h.progress}/${h.target}",
            color = Wellness.colors.muted,
            style = Wellness.typography.bodySmall,
        )
    }
}
