package com.wellness.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.components.FieldLabel
import com.wellness.app.ui.components.IconCellPicker
import com.wellness.app.ui.components.NoFeedbackButton
import com.wellness.app.ui.components.OverlayScreen
import com.wellness.app.ui.components.TextInput
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.Habit
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.theme.AccentPalette
import com.wellness.app.ui.theme.Wellness

private val habitIcons = listOf(
    "bottle-bold-duotone", "book-bookmark-bold-duotone", "meditation-round-bold-duotone",
    "dumbbell-large-bold-duotone", "running-2-bold-duotone", "leaf-bold-duotone",
    "heart-pulse-bold-duotone", "smile-circle-outline",
)

private val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@Composable
fun AddHabitScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(habitIcons[0]) }
    var color by remember { mutableStateOf<Color>(AccentPalette[0]) }
    var target by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("раз") }
    val selectedDays = remember { mutableStateOf(setOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")) }

    OverlayScreen(
        title = "Новая привычка",
        onBack = onBack,
        primaryLabel = "Создать",
        primaryEnabled = name.isNotBlank(),
        onPrimary = {
            state.habits.add(
                Habit(
                    id = (state.habits.maxOfOrNull { it.id } ?: 0) + 1,
                    name = name,
                    icon = icon,
                    color = color,
                    target = target.toIntOrNull() ?: 1,
                    progress = 0,
                    unit = unit,
                    schedule = if (selectedDays.value.size == 7) "Ежедневно" else selectedDays.value.joinToString(" "),
                )
            )
            onBack()
        },
    ) {
        FieldLabel("Название")
        TextInput(value = name, placeholder = "Например: Прочитать 10 страниц", onValueChange = { name = it })

        FieldLabel("Иконка")
        IconCellPicker(icons = habitIcons, selected = icon, tint = color, onSelect = { icon = it })

        FieldLabel("Цвет")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentPalette.forEach { c ->
                val active = c.value == color.value
                NoFeedbackButton(onClick = { color = c }, modifier = Modifier.size(36.dp)) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(if (active) c.copy(alpha = 0.22f) else Color.Transparent, RoundedCornerShape(999.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(24.dp).background(c, RoundedCornerShape(999.dp)))
                    }
                }
            }
        }

        FieldLabel("Цель")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput(
                value = target,
                placeholder = "1",
                onValueChange = { target = it.filter { c -> c.isDigit() } },
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            TextInput(
                value = unit,
                placeholder = "раз / мин / страниц…",
                onValueChange = { unit = it },
                modifier = Modifier.weight(2f),
            )
        }

        FieldLabel("Дни недели")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            days.forEach { d ->
                val active = d in selectedDays.value
                NoFeedbackButton(onClick = {
                    selectedDays.value = if (active) selectedDays.value - d else selectedDays.value + d
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
    }
}
