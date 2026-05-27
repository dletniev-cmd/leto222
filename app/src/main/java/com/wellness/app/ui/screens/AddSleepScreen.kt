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
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.theme.Wellness

@Composable
fun AddSleepScreen(onBack: () -> Unit) {
    var fromH by remember { mutableStateOf(23) }
    var fromM by remember { mutableStateOf(30) }
    var toH by remember { mutableStateOf(7) }
    var toM by remember { mutableStateOf(0) }
    var quality by remember { mutableStateOf(2) }

    OverlayScreen(
        title = "Записать сон",
        onBack = onBack,
        primaryLabel = "Сохранить",
        onPrimary = onBack,
    ) {
        FieldLabel("Лёг спать")
        TimePicker(fromH, fromM) { h, m -> fromH = h; fromM = m }
        FieldLabel("Проснулся")
        TimePicker(toH, toM) { h, m -> toH = h; toM = m }
        FieldLabel("Качество сна")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Плохо", "Так себе", "Норм", "Отлично").forEachIndexed { idx, q ->
                val active = idx == quality
                NoFeedbackButton(onClick = { quality = idx }, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .background(if (active) Wellness.colors.accentSoft else Wellness.colors.track, RoundedCornerShape(12.dp))
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(q, color = if (active) Wellness.colors.accent else Wellness.colors.text, style = Wellness.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePicker(hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Wellness.colors.track, RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stepper(hour, 0, 23) { onChange(it, minute) }
        Text(":", color = Wellness.colors.text, style = Wellness.typography.displayMedium, modifier = Modifier.padding(horizontal = 8.dp))
        Stepper(minute, 0, 59, step = 5) { onChange(hour, it) }
    }
}

@Composable
private fun Stepper(value: Int, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    // Wrap-around helper. Standard modulo with the +span tweak so negative
    // remainders (Kotlin's `%` is sign-preserving) still land on the positive
    // side of the range — e.g. minute 0, step 5 going DOWN wraps to 55, not 4.
    fun wrap(next: Int): Int {
        val span = max - min + 1
        return ((next - min) % span + span) % span + min
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        NoFeedbackButton(onClick = { onChange(wrap(value - step)) }, modifier = Modifier.size(36.dp)) {
            Box(
                Modifier.size(36.dp).background(Wellness.colors.container, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { SolarIcon(name = "alt-arrow-down-outline", tint = Wellness.colors.text, size = 18.dp) }
        }
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            Text("%02d".format(value), color = Wellness.colors.text, style = Wellness.typography.displayMedium)
        }
        NoFeedbackButton(onClick = { onChange(wrap(value + step)) }, modifier = Modifier.size(36.dp)) {
            Box(
                Modifier.size(36.dp).background(Wellness.colors.container, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { SolarIcon(name = "alt-arrow-up-outline", tint = Wellness.colors.text, size = 18.dp) }
        }
    }
}
