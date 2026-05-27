package com.wellness.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.components.FieldLabel
import com.wellness.app.ui.components.NoFeedbackButton
import com.wellness.app.ui.components.OverlayScreen
import com.wellness.app.ui.components.TextInput
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.TaskItem
import com.wellness.app.ui.state.TaskStatus
import com.wellness.app.ui.theme.Wellness

private val daysShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@Composable
fun AddTaskScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    var name by remember { mutableStateOf("") }
    var fromH by remember { mutableStateOf(9) }
    var fromM by remember { mutableStateOf(0) }
    var toH by remember { mutableStateOf(10) }
    var toM by remember { mutableStateOf(0) }
    val days = remember { mutableStateOf(setOf("Пн", "Вт", "Ср", "Чт", "Пт")) }

    OverlayScreen(
        title = "Новая задача",
        onBack = onBack,
        primaryLabel = "Добавить",
        primaryEnabled = name.isNotBlank(),
        onPrimary = {
            val duration = ((toH * 60 + toM) - (fromH * 60 + fromM)).coerceAtLeast(0)
            val durLabel = when {
                duration < 60 -> "${duration}м"
                duration % 60 == 0 -> "${duration / 60}ч"
                else -> "${duration / 60}ч ${duration % 60}м"
            }
            state.tasks.add(
                TaskItem(
                    id = (state.tasks.maxOfOrNull { it.id } ?: 0) + 1,
                    name = name,
                    time = "%02d:%02d".format(fromH, fromM),
                    duration = durLabel,
                    status = TaskStatus.Upcoming,
                    statusText = "Запланировано",
                )
            )
            onBack()
        },
    ) {
        FieldLabel("Название")
        TextInput(value = name, placeholder = "Например: Тренировка", onValueChange = { name = it })

        FieldLabel("Дни")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            daysShort.forEach { d ->
                val active = d in days.value
                NoFeedbackButton(onClick = {
                    days.value = if (active) days.value - d else days.value + d
                }, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .background(if (active) Wellness.colors.accentSoft else Wellness.colors.track, RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(d, color = if (active) Wellness.colors.accent else Wellness.colors.text, style = Wellness.typography.labelMedium)
                    }
                }
            }
        }

        FieldLabel("С")
        TimeWheel(hour = fromH, minute = fromM, onChange = { h, m -> fromH = h; fromM = m })

        FieldLabel("По")
        TimeWheel(hour = toH, minute = toM, onChange = { h, m -> toH = h; toM = m })
    }
}

@Composable
private fun TimeWheel(hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Wellness.colors.track, RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stepper(hour, 0, 23) { onChange(it, minute) }
        Text(":", color = Wellness.colors.text, style = Wellness.typography.displayMedium, modifier = Modifier.padding(horizontal = 6.dp))
        Stepper(minute, 0, 59, step = 5) { onChange(hour, it) }
    }
}

@Composable
private fun Stepper(value: Int, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperBtn(icon = "alt-arrow-down-outline") {
            val v = if (value - step < min) max - ((max - min + 1 - step) % (max - min + 1)) else value - step
            onChange(v.coerceIn(min, max))
        }
        Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
            Text("%02d".format(value), color = Wellness.colors.text, style = Wellness.typography.displayMedium)
        }
        StepperBtn(icon = "alt-arrow-up-outline") {
            val v = if (value + step > max) min else value + step
            onChange(v.coerceIn(min, max))
        }
    }
}

@Composable
private fun StepperBtn(icon: String, onClick: () -> Unit) {
    NoFeedbackButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Box(
            Modifier.size(36.dp).background(Wellness.colors.container, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = icon, tint = Wellness.colors.text, size = 18.dp)
        }
    }
}
