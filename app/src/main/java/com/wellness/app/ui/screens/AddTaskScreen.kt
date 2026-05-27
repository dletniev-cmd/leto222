package com.wellness.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.components.FieldLabel
import com.wellness.app.ui.components.TextInput
import com.wellness.app.ui.components.TimePickerInline
import com.wellness.app.ui.components.ToggleRow
import com.wellness.app.ui.components.WeekdaysPicker
import com.wellness.app.ui.components.WizardIconCatalog
import com.wellness.app.ui.components.WizardIconGrid
import com.wellness.app.ui.components.WizardPreviewTile
import com.wellness.app.ui.components.WizardScaffold
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.TaskItem
import com.wellness.app.ui.state.scheduleTextFor
import com.wellness.app.ui.theme.AccentPalette
import com.wellness.app.ui.theme.Wellness

/**
 * Three-step "create task" wizard. Mirrors [AddHabitScreen]'s structure but
 * step 1 collects a time range and step 3 collects days + optional advance
 * reminder.
 *
 * Step 1 — Что и когда: name + time range (start / end)
 * Step 2 — Стиль:       icon + colour + live preview
 * Step 3 — Дни:         days of week + optional "напомнить за N минут"
 */
@Composable
fun AddTaskScreen(onBack: () -> Unit) {
    val state = LocalAppState.current

    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var startH by remember { mutableIntStateOf(9) }
    var startM by remember { mutableIntStateOf(0) }
    var endH by remember { mutableIntStateOf(10) }
    var endM by remember { mutableIntStateOf(0) }
    var icon by remember { mutableStateOf(WizardIconCatalog[10]) } // dumbbell default
    var color by remember { mutableStateOf<Color>(AccentPalette[2]) }
    var days by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var remind by remember { mutableStateOf(false) }
    var remindMinutesBefore by remember { mutableIntStateOf(10) }

    val startMin = startH * 60 + startM
    val endMin = endH * 60 + endM
    val timeRangeValid = endMin > startMin

    val stepLabels = listOf("Что и когда", "Стиль", "Дни")
    val canAdvance = when (step) {
        1 -> name.isNotBlank() && timeRangeValid
        2 -> true
        3 -> days.isNotEmpty()
        else -> false
    }
    val primaryLabel = if (step < 3) "Далее" else "Создать задачу"

    WizardScaffold(
        title = "Новая задача",
        stepCount = 3,
        currentStep = step,
        stepLabel = "Шаг $step из 3 · ${stepLabels[step - 1]}",
        onBack = { if (step > 1) step-- else onBack() },
        primaryLabel = primaryLabel,
        primaryEnabled = canAdvance,
        onPrimary = {
            if (step < 3) {
                step++
            } else {
                state.addTask(
                    TaskItem(
                        id = 0,
                        name = name.trim(),
                        icon = icon,
                        color = color,
                        startMinutes = startMin,
                        endMinutes = endMin,
                        days = days,
                        remind = remind,
                        remindMinutesBefore = remindMinutesBefore,
                    )
                )
                onBack()
            }
        },
        showSecondary = step > 1,
        secondaryLabel = "Назад",
        onSecondary = { if (step > 1) step-- },
    ) {
        when (step) {
            1 -> TaskStep1(
                name = name, onNameChange = { name = it },
                startH = startH, startM = startM, endH = endH, endM = endM,
                onStartChange = { h, m -> startH = h; startM = m },
                onEndChange = { h, m -> endH = h; endM = m },
                rangeValid = timeRangeValid,
            )
            2 -> TaskStep2(
                name = name, startH = startH, startM = startM, endH = endH, endM = endM,
                icon = icon, onIconChange = { icon = it },
                color = color, onColorChange = { color = it },
            )
            3 -> TaskStep3(
                days = days, onDaysChange = { days = it },
                remind = remind, onRemindChange = { remind = it },
                remindMinutesBefore = remindMinutesBefore,
                onRemindMinutesBeforeChange = { remindMinutesBefore = it },
            )
        }
    }
}

@Composable
private fun TaskStep1(
    name: String, onNameChange: (String) -> Unit,
    startH: Int, startM: Int, endH: Int, endM: Int,
    onStartChange: (Int, Int) -> Unit,
    onEndChange: (Int, Int) -> Unit,
    rangeValid: Boolean,
) {
    FieldLabel("Название")
    TextInput(value = name, placeholder = "Например: Тренировка ног", onValueChange = onNameChange)

    FieldLabel("Начало")
    TimePickerInline(hour = startH, minute = startM, onChange = onStartChange)

    FieldLabel("Окончание")
    TimePickerInline(hour = endH, minute = endM, onChange = onEndChange)

    if (!rangeValid) {
        Spacer(Modifier.height(4.dp))
        Text(
            "Окончание должно быть позже начала",
            color = Wellness.colors.accent,
            style = Wellness.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun TaskStep2(
    name: String, startH: Int, startM: Int, endH: Int, endM: Int,
    icon: String, onIconChange: (String) -> Unit,
    color: Color, onColorChange: (Color) -> Unit,
) {
    val timeLabel = "%02d:%02d – %02d:%02d".format(startH, startM, endH, endM)
    WizardPreviewTile(icon = icon, color = color, title = name, meta = timeLabel)

    FieldLabel("Иконка")
    WizardIconGrid(selected = icon, tint = color, onSelect = onIconChange)

    FieldLabel("Цвет")
    ColorPaletteGrid(selected = color, onSelect = onColorChange)
}

private val remindPresets = listOf(0, 5, 10, 15, 30, 60)

@Composable
private fun TaskStep3(
    days: Set<Int>, onDaysChange: (Set<Int>) -> Unit,
    remind: Boolean, onRemindChange: (Boolean) -> Unit,
    remindMinutesBefore: Int, onRemindMinutesBeforeChange: (Int) -> Unit,
) {
    FieldLabel("Дни недели")
    WeekdaysPicker(selected = days, onChange = onDaysChange)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DayPreset("Ежедневно", days.size == 7) { onDaysChange(setOf(1, 2, 3, 4, 5, 6, 7)) }
        DayPreset("Будни", days == setOf(1, 2, 3, 4, 5)) { onDaysChange(setOf(1, 2, 3, 4, 5)) }
        DayPreset("Выходные", days == setOf(6, 7)) { onDaysChange(setOf(6, 7)) }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Расписание: ${scheduleTextFor(days)}",
        color = Wellness.colors.muted,
        style = Wellness.typography.bodySmall,
        modifier = Modifier.padding(start = 4.dp),
    )

    FieldLabel("Напоминание")
    ToggleRow(
        title = "Напомнить заранее",
        subtitle = if (remind) {
            if (remindMinutesBefore == 0) "В момент начала" else "За $remindMinutesBefore мин до начала"
        } else "Без напоминаний",
        on = remind,
        onToggle = onRemindChange,
    )
    if (remind) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            remindPresets.forEach { m ->
                MinutesChip(
                    label = if (m == 0) "вовремя" else "${m}м",
                    active = m == remindMinutesBefore,
                    onClick = { onRemindMinutesBeforeChange(m) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.MinutesChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .background(
                if (active) Wellness.colors.accentSoft else Wellness.colors.track,
                RoundedCornerShape(999.dp),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) Wellness.colors.accent else Wellness.colors.text,
            style = Wellness.typography.labelMedium,
        )
    }
}

@Composable
private fun RowScope.DayPreset(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .background(
                if (active) Wellness.colors.accentSoft else Wellness.colors.track,
                RoundedCornerShape(999.dp),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) Wellness.colors.accent else Wellness.colors.text,
            style = Wellness.typography.labelMedium,
        )
    }
}
