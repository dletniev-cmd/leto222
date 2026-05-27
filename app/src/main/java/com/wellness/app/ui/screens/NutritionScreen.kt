package com.wellness.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.components.NoFeedbackButton
import com.wellness.app.ui.components.ProgressRing
import com.wellness.app.ui.components.ScreenHeader
import com.wellness.app.ui.components.ScreenScaffold
import com.wellness.app.ui.components.SectionTitle
import com.wellness.app.ui.components.SegItem
import com.wellness.app.ui.components.SegmentedTabs
import com.wellness.app.ui.components.StackedRing
import com.wellness.app.ui.components.WCard
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.WaterEntry
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors

@Composable
fun NutritionScreen(onAddMeal: () -> Unit = {}) {
    var tab by remember { mutableStateOf("water") }

    ScreenScaffold {
        ScreenHeader(
            title = "Питание",
            trailingIcon = "add-circle-bold-duotone",
            trailingAccent = true,
            onTrailingClick = onAddMeal,
        )
        Box(Modifier.screenHPad()) {
            SegmentedTabs(
                items = listOf(
                    SegItem("water", "Вода", "bottle-bold-duotone"),
                    SegItem("food", "Еда", "plate-bold-duotone"),
                ),
                selected = tab,
                onSelect = { tab = it },
            )
        }
        Box(Modifier.height(14.dp))
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInHorizontally(tween(260)) { it / 12 })
                    .togetherWith(fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { -it / 24 })
            },
            label = "nutrition_pane"
        ) { current ->
            if (current == "water") WaterPane() else FoodPane()
        }
    }
}

@Composable
private fun WaterPane() {
    val state = LocalAppState.current
    Column {
        // Big water ring lives directly on the page — no container plate.
        Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = state.waterMl.toFloat() / state.waterTarget,
                    color = WellnessColors.Water,
                    size = 200.dp,
                    strokeWidth = 14.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SolarIcon(name = "bottle-bold-duotone", tint = WellnessColors.Water, size = 26.dp)
                    Box(Modifier.height(2.dp))
                    Text(state.waterMl.toString(), color = Wellness.colors.text, style = Wellness.typography.displayLarge)
                    Text("из ${state.waterTarget} мл", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
                }
            }
        }
        SectionTitle("Быстрое добавление")
        val quick = listOf(200 to "waterdrop-outline", 350 to "cup-paper-bold-duotone", 500 to "bottle-bold-duotone", 750 to "bottle-bold-duotone")
        Column(modifier = Modifier.screenHPad(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in quick.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for ((ml, icon) in row) {
                        QuickAddButton(ml, icon, Modifier.weight(1f)) {
                            val newVal = (state.waterMl + ml).coerceAtMost(state.waterTarget)
                            state.waterMl = newVal
                            state.waterHistory.add(0, WaterEntry(ml, "сейчас", labelFor(ml), icon))
                        }
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
        SectionTitle("История")
        WCard(modifier = Modifier.screenHPad(), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
            Column {
                state.waterHistory.forEach { e ->
                    HistoryRow(e)
                }
            }
        }
    }
}

private fun labelFor(ml: Int): String = when {
    ml <= 250 -> "Глоток"
    ml <= 400 -> "Стакан воды"
    ml <= 600 -> "Бутылка"
    else -> "Большая бутылка"
}

@Composable
private fun QuickAddButton(ml: Int, icon: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    NoFeedbackButton(onClick = onClick, modifier = modifier) {
        Box(
            modifier
                .fillMaxWidth()
                .background(Wellness.colors.container, RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                SolarIcon(name = icon, tint = WellnessColors.Water, size = 22.dp)
                Box(Modifier.height(8.dp))
                Text("+$ml мл", color = Wellness.colors.text, style = Wellness.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun HistoryRow(e: WaterEntry) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).background(WellnessColors.Water.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = e.icon, tint = WellnessColors.Water, size = 20.dp)
        }
        Box(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.label, color = Wellness.colors.text, style = Wellness.typography.titleSmall)
            Text("${e.time} · ${e.ml} мл", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        }
        Text("+${e.ml}", color = Wellness.colors.text, style = Wellness.typography.titleSmall)
    }
}

@Composable
private fun FoodPane() {
    val state = LocalAppState.current
    Column {
        // Stacked macros ring sits directly on the page, no plate.
        Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                val pLen = state.protein.toFloat() / state.proteinTarget * 0.33f
                val fLen = state.fat.toFloat() / state.fatTarget * 0.33f
                val cLen = state.carb.toFloat() / state.carbTarget * 0.33f
                StackedRing(
                    segments = listOf(
                        WellnessColors.Protein to pLen,
                        WellnessColors.Fat to fLen,
                        WellnessColors.Carb to cLen,
                    ),
                    size = 200.dp,
                    strokeWidth = 14.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.kcal.toString(), color = Wellness.colors.text, style = Wellness.typography.displayLarge)
                    Text("из ${state.kcalTarget} ккал", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
                }
            }
        }
        Box(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().screenHPad(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MacroRow("Белки", state.protein, state.proteinTarget, "г", WellnessColors.Protein)
            MacroRow("Жиры", state.fat, state.fatTarget, "г", WellnessColors.Fat)
            MacroRow("Углеводы", state.carb, state.carbTarget, "г", WellnessColors.Carb)
        }
        SectionTitle("Сегодня")
        WCard(modifier = Modifier.screenHPad(), contentPadding = PaddingValues(8.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                state.meals.forEach { meal ->
                    MealRow(meal.title, meal.icon, meal.color, meal.kcal, meal.description ?: "")
                }
            }
        }
    }
}

@Composable
private fun MacroRow(label: String, value: Int, target: Int, unit: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(10.dp).background(color, RoundedCornerShape(999.dp))
        )
        Box(Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value.toString(), color = Wellness.colors.text, style = Wellness.typography.titleMedium)
                Text(" / $target $unit", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
            }
            Text(label, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        }
    }
}

@Composable
private fun MealRow(title: String, icon: String, color: Color, kcal: Int?, description: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = icon, tint = color, size = 22.dp)
        }
        Box(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Wellness.colors.text, style = Wellness.typography.titleSmall)
            Text(
                if (kcal != null) "$description · $kcal ккал" else description,
                color = Wellness.colors.muted, style = Wellness.typography.bodySmall,
            )
        }
        NoFeedbackButton(onClick = {}, modifier = Modifier.size(28.dp)) {
            SolarIcon(name = "add-circle-bold-duotone", tint = Wellness.colors.accent, size = 22.dp)
        }
    }
}
