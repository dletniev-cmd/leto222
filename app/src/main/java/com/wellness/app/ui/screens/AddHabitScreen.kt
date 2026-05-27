package com.wellness.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.wellness.app.ui.components.ChipRow
import com.wellness.app.ui.components.FieldLabel
import com.wellness.app.ui.components.NumberStepper
import com.wellness.app.ui.components.TextInput
import com.wellness.app.ui.components.TimePickerInline
import com.wellness.app.ui.components.ToggleRow
import com.wellness.app.ui.components.WeekdaysPicker
import com.wellness.app.ui.components.WizardIconCatalog
import com.wellness.app.ui.components.WizardIconGrid
import com.wellness.app.ui.components.WizardPreviewTile
import com.wellness.app.ui.components.WizardScaffold
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.Habit
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.scheduleTextFor
import com.wellness.app.ui.theme.AccentPalette
import com.wellness.app.ui.theme.Wellness

private val habitUnitPresets = listOf("раз", "стаканов", "страниц", "мин", "км", "шагов", "минут", "ч", "грамм", "ккал")

/**
 * Three-step "create habit" wizard. The function keeps the original name
 * (`AddHabitScreen`) so `WellnessApp` keeps compiling — under the hood it's
 * a real wizard with progress bar and gated CTA.
 *
 * Step 1 — Что и сколько: name + target count + unit
 * Step 2 — Стиль:        icon + colour + live preview tile
 * Step 3 — Когда:        days of week + optional reminder time
 */
@Composable
fun AddHabitScreen(onBack: () -> Unit) {
    val state = LocalAppState.current

    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var target by remember { mutableIntStateOf(1) }
    var unit by remember { mutableStateOf("раз") }
    var icon by remember { mutableStateOf(WizardIconCatalog[0]) }
    var color by remember { mutableStateOf<Color>(AccentPalette[1]) }
    var days by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var remind by remember { mutableStateOf(false) }
    var remindH by remember { mutableIntStateOf(8) }
    var remindM by remember { mutableIntStateOf(30) }

    val stepLabels = listOf("Что и сколько", "Стиль", "Когда")
    val canAdvance = when (step) {
        1 -> name.isNotBlank() && target >= 1
        2 -> true
        3 -> days.isNotEmpty()
        else -> false
    }
    val primaryLabel = if (step < 3) "Далее" else "Создать привычку"

    WizardScaffold(
        title = "Новая привычка",
        stepCount = 3,
        currentStep = step,
        stepLabel = "Шаг $step из 3 · ${stepLabels[step - 1]}",
        onBack = {
            if (step > 1) step-- else onBack()
        },
        primaryLabel = primaryLabel,
        primaryEnabled = canAdvance,
        onPrimary = {
            if (step < 3) {
                step++
            } else {
                val remindAt = if (remind) "%02d:%02d".format(remindH, remindM) else null
                state.addHabit(
                    Habit(
                        id = 0,
                        name = name.trim(),
                        icon = icon,
                        color = color,
                        target = target.coerceAtLeast(1),
                        unit = unit,
                        days = days,
                        remind = remind,
                        remindAt = remindAt,
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
            1 -> HabitStep1(
                name = name, onNameChange = { name = it },
                target = target, onTargetChange = { target = it },
                unit = unit, onUnitChange = { unit = it },
            )
            2 -> HabitStep2(
                name = name, target = target, unit = unit,
                icon = icon, onIconChange = { icon = it },
                color = color, onColorChange = { color = it },
            )
            3 -> HabitStep3(
                days = days, onDaysChange = { days = it },
                remind = remind, onRemindChange = { remind = it },
                remindH = remindH, remindM = remindM,
                onTimeChange = { h, m -> remindH = h; remindM = m },
            )
        }
    }
}

@Composable
private fun HabitStep1(
    name: String, onNameChange: (String) -> Unit,
    target: Int, onTargetChange: (Int) -> Unit,
    unit: String, onUnitChange: (String) -> Unit,
) {
    FieldLabel("Название")
    TextInput(value = name, placeholder = "Например: Прочитать 10 страниц", onValueChange = onNameChange)

    FieldLabel("Цель в день")
    NumberStepper(value = target, min = 1, max = 50, onChange = onTargetChange)

    FieldLabel("Единица измерения")
    TextInput(value = unit, placeholder = "раз / страниц / мин…", onValueChange = onUnitChange)
    Spacer(Modifier.height(6.dp))
    ChipRow(options = habitUnitPresets, selected = unit, onSelect = onUnitChange)
}

@Composable
private fun HabitStep2(
    name: String, target: Int, unit: String,
    icon: String, onIconChange: (String) -> Unit,
    color: Color, onColorChange: (Color) -> Unit,
) {
    val previewMeta = if (target > 1) "$target $unit · в день" else "Раз в день"
    WizardPreviewTile(icon = icon, color = color, title = name, meta = previewMeta)

    FieldLabel("Иконка")
    WizardIconGrid(selected = icon, tint = color, onSelect = onIconChange)

    FieldLabel("Цвет")
    ColorPaletteGrid(selected = color, onSelect = onColorChange)
}

@Composable
private fun HabitStep3(
    days: Set<Int>, onDaysChange: (Set<Int>) -> Unit,
    remind: Boolean, onRemindChange: (Boolean) -> Unit,
    remindH: Int, remindM: Int,
    onTimeChange: (Int, Int) -> Unit,
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
        title = "Напомнить о привычке",
        subtitle = if (remind) "В выбранное время каждый день из расписания" else "Без напоминаний",
        on = remind,
        onToggle = onRemindChange,
    )
    if (remind) {
        Spacer(Modifier.height(8.dp))
        TimePickerInline(hour = remindH, minute = remindM, onChange = onTimeChange)
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

/**
 * 6×3 colour palette. Selected swatch grows + halos with its own colour at
 * low alpha; unselected swatches are simple discs.
 */
@Composable
fun ColorPaletteGrid(selected: Color, onSelect: (Color) -> Unit) {
    val rows = AccentPalette.chunked(6)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { c ->
                    val active = c.value == selected.value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (active) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .background(c.copy(alpha = 0.22f), CircleShape)
                                    .clickable { onSelect(c) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .background(c, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SolarIcon(name = "check-read-bold", tint = Color(0xFF0C1F12), size = 14.dp)
                                }
                            }
                        } else {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .background(c, CircleShape)
                                    .clickable { onSelect(c) },
                            )
                        }
                    }
                }
            }
        }
    }
}
