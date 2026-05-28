package com.wellness.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wellness.app.ui.components.ColorPickerGrid
import com.wellness.app.ui.components.OverlayHost
import com.wellness.app.ui.components.PrimaryActionButton
import com.wellness.app.ui.components.RoundedSlideOverlay
import com.wellness.app.ui.components.SettingsCard
import com.wellness.app.ui.components.SettingsHeader
import com.wellness.app.ui.components.SettingsRow
import com.wellness.app.ui.components.SettingsRowDivider
import com.wellness.app.ui.components.TimePickerInline
import com.wellness.app.ui.components.rememberParallaxProgress
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.Habit
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.scheduleTextFor
import com.wellness.app.ui.theme.AccentPalette
import com.wellness.app.ui.theme.Manrope
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors

private enum class HabitRoute { Root, Goal, Reminder }

private enum class GoalKind { Count, Fact }
private enum class GoalPeriod { Day, Week }

private data class HabitDraft(
    val name: String = "",
    val icon: String = "bottle-bold-duotone",
    val color: Color = AccentPalette[3],   // sky blue by default
    val goalKind: GoalKind = GoalKind.Count,
    val target: Int = 8,
    val unit: String = "стак.",
    val period: GoalPeriod = GoalPeriod.Day,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val remind: Boolean = true,
    val remindH: Int = 9,
    val remindM: Int = 0,
    val note: String = "",
) {
    fun goalLabel(): String = when (goalKind) {
        GoalKind.Fact -> "Сделано или нет"
        GoalKind.Count -> {
            val periodSuffix = if (period == GoalPeriod.Day) "в день" else "в неделю"
            "$target ${unit.ifBlank { "раз" }} $periodSuffix"
        }
    }

    fun reminderLabel(): String {
        if (!remind) return "Выключено"
        val sched = scheduleTextFor(days)
        val time = "%02d:%02d".format(remindH, remindM)
        return "$sched · $time"
    }
}

/**
 * "Create habit" screen — new settings-list flow.
 *
 *   Root           name input + 4 settings rows (Цель / Иконка / Цвет /
 *                  Напоминание) + note + CTA. Icon and Color rows expand a
 *                  rounded popover card inline below themselves; Goal and
 *                  Reminder push a full sub-screen via RoundedSlideOverlay.
 */
@Composable
fun AddHabitScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    var route by remember { mutableStateOf(HabitRoute.Root) }
    var draft by remember { mutableStateOf(HabitDraft()) }

    val parallax = rememberParallaxProgress()
    Box(Modifier.fillMaxSize()) {
        OverlayHost(parallaxProgress = parallax) {
            HabitRootScreen(
                draft = draft,
                onDraft = { draft = it },
                onBack = onBack,
                onGoal = { route = HabitRoute.Goal },
                onReminder = { route = HabitRoute.Reminder },
                onCreate = {
                    val remindAt = if (draft.remind) "%02d:%02d".format(draft.remindH, draft.remindM) else null
                    val target = if (draft.goalKind == GoalKind.Count) draft.target.coerceAtLeast(1) else 1
                    val unit = if (draft.goalKind == GoalKind.Count) draft.unit else ""
                    state.addHabit(
                        Habit(
                            id = 0,
                            name = draft.name.trim(),
                            icon = draft.icon,
                            color = draft.color,
                            target = target,
                            unit = unit,
                            days = draft.days,
                            remind = draft.remind,
                            remindAt = remindAt,
                        )
                    )
                    onBack()
                },
            )
        }
        if (route != HabitRoute.Root) {
            key(route) {
                RoundedSlideOverlay(
                    parallaxProgress = parallax,
                    onDismissed = { route = HabitRoute.Root },
                ) { animatedBack ->
                    when (route) {
                        HabitRoute.Goal -> HabitGoalSubScreen(draft, { draft = it }, animatedBack)
                        HabitRoute.Reminder -> HabitReminderSubScreen(draft, { draft = it }, animatedBack)
                        HabitRoute.Root -> Unit
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// ROOT
// ───────────────────────────────────────────────────────────────────────────

private enum class RootExpand { None, Icon, Color }

@Composable
private fun HabitRootScreen(
    draft: HabitDraft,
    onDraft: (HabitDraft) -> Unit,
    onBack: () -> Unit,
    onGoal: () -> Unit,
    onReminder: () -> Unit,
    onCreate: () -> Unit,
) {
    val scroll = rememberScrollState()
    var expand by remember { mutableStateOf(RootExpand.None) }

    val canCreate = draft.name.trim().isNotEmpty()

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
                .padding(top = 6.dp, bottom = 120.dp),
        ) {
            SettingsHeader(title = "Новая привычка", onBack = onBack)

            // ── Name field with mini icon+color preview ────────────────────
            Box(Modifier.screenHPad().padding(top = 10.dp)) {
                NamePreviewField(
                    name = draft.name,
                    icon = draft.icon,
                    color = draft.color,
                    onChange = { onDraft(draft.copy(name = it)) },
                )
            }

            // ── Parameters card ───────────────────────────────────────────
            SectionLabel("ПАРАМЕТРЫ", topPad = 22.dp)
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "target-bold-duotone",
                    iconTile = WellnessColors.TileBlue,
                    title = "Цель",
                    value = draft.goalLabel(),
                    onClick = onGoal,
                )
                SettingsRowDivider()

                SettingsRow(
                    icon = "stars-bold-duotone",
                    iconTile = WellnessColors.TilePink,
                    title = "Иконка",
                    showChevron = false,
                    trailing = {
                        MiniIconPreview(icon = draft.icon, color = draft.color)
                    },
                    onClick = {
                        expand = if (expand == RootExpand.Icon) RootExpand.None else RootExpand.Icon
                    },
                )
                IconPopover(
                    visible = expand == RootExpand.Icon,
                    selected = draft.icon,
                    tint = draft.color,
                    onSelect = { onDraft(draft.copy(icon = it)) },
                )
                SettingsRowDivider()

                SettingsRow(
                    icon = "star-shine-bold-duotone",
                    iconTile = WellnessColors.TileViolet,
                    title = "Цвет",
                    showChevron = false,
                    trailing = {
                        Box(
                            Modifier
                                .size(26.dp)
                                .background(draft.color, CircleShape),
                        )
                    },
                    onClick = {
                        expand = if (expand == RootExpand.Color) RootExpand.None else RootExpand.Color
                    },
                )
                ColorPopover(
                    visible = expand == RootExpand.Color,
                    selected = draft.color,
                    onSelect = { onDraft(draft.copy(color = it)) },
                )
                SettingsRowDivider()

                SettingsRow(
                    icon = "bell-bold-duotone",
                    iconTile = WellnessColors.TileOrange,
                    title = "Напомнить",
                    value = draft.reminderLabel(),
                    onClick = onReminder,
                )
            }

            // ── Note ─────────────────────────────────────────────────────
            SectionLabel("ЗАМЕТКА", topPad = 22.dp)
            NoteCard(
                value = draft.note,
                onChange = { onDraft(draft.copy(note = it)) },
            )

            Spacer(Modifier.height(36.dp))
        }

        // ── Pinned bottom CTA ─────────────────────────────────────────────
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Wellness.colors.bg)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            PrimaryActionButton(
                label = "Создать привычку",
                enabled = canCreate,
                onClick = onCreate,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, topPad: androidx.compose.ui.unit.Dp = 16.dp) {
    Text(
        text,
        color = Wellness.colors.muted,
        style = Wellness.typography.labelSmall,
        modifier = Modifier.padding(start = 28.dp, top = topPad, bottom = 8.dp),
    )
}

@Composable
private fun NamePreviewField(name: String, icon: String, color: Color, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Wellness.colors.container, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(color, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = icon, tint = Color.White, size = 22.dp)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (name.isEmpty()) {
                Text(
                    "Название привычки",
                    color = Wellness.colors.muted,
                    style = Wellness.typography.titleSmall,
                )
            }
            BasicTextField(
                value = name,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = Manrope,
                    fontSize = 17.sp,
                    color = Wellness.colors.text,
                ),
                cursorBrush = SolidColor(Wellness.colors.accent),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
        }
    }
}

@Composable
private fun MiniIconPreview(icon: String, color: Color) {
    Box(
        Modifier
            .size(26.dp)
            .background(color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        SolarIcon(name = icon, tint = Color.White, size = 15.dp)
    }
}

@Composable
private fun NoteCard(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .screenHPad()
            .fillMaxWidth()
            .background(Wellness.colors.container, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                "Добавьте короткое описание…",
                color = Wellness.colors.muted.copy(alpha = 0.7f),
                style = Wellness.typography.bodyMedium,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(
                fontFamily = Manrope,
                fontSize = 15.sp,
                color = Wellness.colors.text,
            ),
            cursorBrush = SolidColor(Wellness.colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 22.dp),
        )
    }
}

// ───────────────────────────────────────────────────────────────────────────
// POPOVERS — anchored expansion below the tapped row.
// ───────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope_ExpandedCard(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .background(
                color = Wellness.colors.bg.copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) { content() }
}

@Composable
private fun IconPopover(
    visible: Boolean,
    selected: String,
    tint: Color,
    onSelect: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
            fadeIn(tween(220)),
        exit = shrinkVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
            fadeOut(tween(160)),
    ) {
        ColumnScope_ExpandedCard {
            IconGrid(selected = selected, tint = tint, onSelect = onSelect)
        }
    }
}

@Composable
private fun ColorPopover(
    visible: Boolean,
    selected: Color,
    onSelect: (Color) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
            fadeIn(tween(220)),
        exit = shrinkVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
            fadeOut(tween(160)),
    ) {
        ColumnScope_ExpandedCard {
            ColorPickerGrid(
                colors = AccentPalette,
                selected = selected,
                onSelect = onSelect,
            )
        }
    }
}

/** 5-column icon grid used inside the icon popover. */
@Composable
private fun IconGrid(selected: String, tint: Color, onSelect: (String) -> Unit) {
    val icons = HabitIconCatalog
    val columns = 5
    val rows = (icons.size + columns - 1) / columns
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(rows) { r ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(columns) { c ->
                    val idx = r * columns + c
                    if (idx < icons.size) {
                        val ic = icons[idx]
                        val active = ic == selected
                        Box(
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    if (active) tint.copy(alpha = 0.20f)
                                    else Wellness.colors.track,
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { onSelect(ic) },
                            contentAlignment = Alignment.Center,
                        ) {
                            SolarIcon(
                                name = ic,
                                tint = if (active) tint else Wellness.colors.text,
                                size = 22.dp,
                            )
                        }
                    } else {
                        Box(Modifier.weight(1f).height(44.dp))
                    }
                }
            }
        }
    }
}

/**
 * Curated catalog of icons for the habit popover — broader than the wizard
 * catalog so users can find what they want without scrolling forever.
 * Falls back to [WizardIconCatalog] entries.
 */
private val HabitIconCatalog: List<String> = listOf(
    // hydration / food
    "bottle-bold-duotone", "cup-paper-bold-duotone", "cup-hot-bold-duotone", "tea-cup-bold-duotone", "wineglass-bold-duotone",
    "waterdrop-bold-duotone", "plate-bold-duotone", "donut-bold-duotone", "donut-bitten-bold-duotone", "chef-hat-bold-duotone",
    // fitness
    "dumbbell-bold-duotone", "dumbbells-bold-duotone", "running-bold-duotone", "running-2-bold-duotone", "walking-bold-duotone",
    "bicycling-bold-duotone", "swimming-bold-duotone", "stretching-bold-duotone", "hiking-bold-duotone", "treadmill-round-bold-duotone",
    // mind / sleep / health
    "meditation-bold-duotone", "meditation-round-bold-duotone", "moon-stars-bold-duotone", "moon-sleep-bold-duotone", "bed-bold-duotone",
    "heart-bold-duotone", "heart-pulse-bold-duotone", "pill-bold-duotone", "pills-bold-duotone", "leaf-bold-duotone",
    // learning / focus
    "book-bookmark-bold-duotone", "book-2-bold-duotone", "notebook-bold-duotone", "square-academic-cap-bold-duotone", "pen-bold-duotone",
    "clipboard-check-bold-duotone", "stopwatch-bold-duotone", "alarm-bold-duotone", "star-bold-duotone", "medal-star-bold-duotone",
    // misc
    "sun-bold-duotone", "sunrise-bold-duotone", "sunset-bold-duotone", "flame-bold-duotone", "fire-bold-duotone",
    "scale-bold-duotone", "wallet-bold-duotone", "smile-circle-bold-duotone", "heart-shine-bold-duotone", "stars-bold-duotone",
)

// ───────────────────────────────────────────────────────────────────────────
// GOAL SUB-SCREEN
// ───────────────────────────────────────────────────────────────────────────

private val UnitPresets = listOf("раз", "стак.", "мл", "мин", "км", "стр.", "шаг.", "г", "ккал")

@Composable
private fun HabitGoalSubScreen(draft: HabitDraft, onDraft: (HabitDraft) -> Unit, onBack: () -> Unit) {
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
                .padding(top = 6.dp, bottom = 120.dp),
        ) {
            SettingsHeader(title = "Цель", onBack = onBack)

            // BIG NUMBER HERO -------------------------------------------------
            Column(
                Modifier
                    .screenHPad()
                    .fillMaxWidth()
                    .background(Wellness.colors.container, RoundedCornerShape(24.dp))
                    .padding(vertical = 28.dp, horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val enabled = draft.goalKind == GoalKind.Count
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (enabled) draft.target.toString() else "✓",
                        color = if (enabled) Wellness.colors.text else Wellness.colors.accent,
                        style = TextStyle(
                            fontFamily = Manrope,
                            fontSize = 56.sp,
                            color = if (enabled) Wellness.colors.text else Wellness.colors.accent,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            letterSpacing = (-0.03).sp,
                        ),
                    )
                    if (enabled) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            draft.unit.ifBlank { "раз" } + " " +
                                if (draft.period == GoalPeriod.Day) "в день" else "в неделю",
                            color = Wellness.colors.muted,
                            style = Wellness.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (enabled)
                        "Привычка считается выполненной, когда вы дойдёте до этого значения."
                    else "Просто галочка раз в день — без счётчика.",
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodySmall,
                )
                if (enabled) {
                    Spacer(Modifier.height(18.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        StepperButton(icon = "alt-arrow-down-outline") {
                            if (draft.target > 1) onDraft(draft.copy(target = draft.target - 1))
                        }
                        StepperButton(icon = "alt-arrow-up-outline", accent = true) {
                            if (draft.target < 999) onDraft(draft.copy(target = draft.target + 1))
                        }
                    }
                }
            }

            // Goal kind segmented --------------------------------------------
            SectionLabel("ТИП ЦЕЛИ")
            Row(
                Modifier
                    .screenHPad()
                    .fillMaxWidth()
                    .background(Wellness.colors.container, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SegItem("Количество", draft.goalKind == GoalKind.Count) {
                    onDraft(draft.copy(goalKind = GoalKind.Count))
                }
                SegItem("Просто факт", draft.goalKind == GoalKind.Fact) {
                    onDraft(draft.copy(goalKind = GoalKind.Fact))
                }
            }

            // Unit chips ------------------------------------------------------
            if (draft.goalKind == GoalKind.Count) {
                SectionLabel("ЕДИНИЦА ИЗМЕРЕНИЯ")
                Column(Modifier.screenHPad()) {
                    ChipWrap(
                        options = UnitPresets,
                        selected = draft.unit,
                        onSelect = { onDraft(draft.copy(unit = it)) },
                    )
                }

                // Period
                SectionLabel("ПЕРИОД", topPad = 22.dp)
                SettingsCard(
                    modifier = Modifier.screenHPad(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    PeriodRow(
                        icon = "sun-bold-duotone",
                        tile = WellnessColors.TileLemon,
                        title = "За день",
                        selected = draft.period == GoalPeriod.Day,
                        onClick = { onDraft(draft.copy(period = GoalPeriod.Day)) },
                    )
                    SettingsRowDivider()
                    PeriodRow(
                        icon = "calendar-bold-duotone",
                        tile = WellnessColors.TileSky,
                        title = "За неделю",
                        selected = draft.period == GoalPeriod.Week,
                        onClick = { onDraft(draft.copy(period = GoalPeriod.Week)) },
                    )
                }
            }

            Spacer(Modifier.height(36.dp))
        }

        // Pinned bottom CTA
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Wellness.colors.bg)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            PrimaryActionButton(label = "Сохранить", onClick = onBack)
        }
    }
}

@Composable
private fun RowScope.SegItem(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .height(40.dp)
            .background(
                if (active) Wellness.colors.accentSoft else Color.Transparent,
                RoundedCornerShape(11.dp),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) Wellness.colors.accent else Wellness.colors.muted,
            style = Wellness.typography.labelMedium,
        )
    }
}

@Composable
private fun StepperButton(icon: String, accent: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .background(
                if (accent) Wellness.colors.accent else Wellness.colors.track,
                CircleShape,
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        SolarIcon(
            name = icon,
            tint = if (accent) Color(0xFF0C1F12) else Wellness.colors.text,
            size = 24.dp,
        )
    }
}

@Composable
private fun PeriodRow(icon: String, tile: Color, title: String, selected: Boolean, onClick: () -> Unit) {
    SettingsRow(
        icon = icon,
        iconTile = tile,
        title = title,
        showChevron = false,
        trailing = {
            if (selected) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(Wellness.colors.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SolarIcon(name = "check-bold", tint = Color.White, size = 13.dp)
                }
            } else {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(
                            Wellness.colors.muted.copy(alpha = 0.16f),
                            CircleShape,
                        ),
                )
            }
        },
        onClick = onClick,
    )
}

/** Wrapping chip layout — items flow onto a new row when they don't fit. */
@Composable
private fun ChipWrap(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    // Simple wrap by chunking into rows of approximate width — Compose has no
    // FlowRow in base material, but for short labels 4-per-row reads cleanly.
    val rows = options.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { opt ->
                    val active = opt == selected
                    Box(
                        Modifier
                            .background(
                                if (active) Wellness.colors.accentSoft else Wellness.colors.track,
                                RoundedCornerShape(999.dp),
                            )
                            .clickable { onSelect(opt) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text(
                            opt,
                            color = if (active) Wellness.colors.accent else Wellness.colors.text,
                            style = Wellness.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// REMINDER SUB-SCREEN
// ───────────────────────────────────────────────────────────────────────────

@Composable
private fun HabitReminderSubScreen(draft: HabitDraft, onDraft: (HabitDraft) -> Unit, onBack: () -> Unit) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
                .padding(top = 6.dp, bottom = 120.dp),
        ) {
            SettingsHeader(title = "Напоминание", onBack = onBack)

            // Day grid
            SectionLabel("ДНИ")
            DayRow(
                selected = draft.days,
                onChange = { onDraft(draft.copy(days = it)) },
            )
            Row(
                Modifier.screenHPad().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DayPreset("Каждый день", draft.days.size == 7) {
                    onDraft(draft.copy(days = setOf(1, 2, 3, 4, 5, 6, 7)))
                }
                DayPreset("Будни", draft.days == setOf(1, 2, 3, 4, 5)) {
                    onDraft(draft.copy(days = setOf(1, 2, 3, 4, 5)))
                }
                DayPreset("Выходные", draft.days == setOf(6, 7)) {
                    onDraft(draft.copy(days = setOf(6, 7)))
                }
            }

            // Notification toggle + time
            SectionLabel("УВЕДОМЛЕНИЕ", topPad = 22.dp)
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "bell-bing-bold-duotone",
                    iconTile = WellnessColors.TileOrange,
                    title = "Присылать пуш",
                    showChevron = false,
                    trailing = {
                        SwitchPill(
                            on = draft.remind,
                            onToggle = { onDraft(draft.copy(remind = it)) },
                        )
                    },
                )
                if (draft.remind) {
                    SettingsRowDivider()
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        TimePickerInline(
                            hour = draft.remindH,
                            minute = draft.remindM,
                            onChange = { h, m -> onDraft(draft.copy(remindH = h, remindM = m)) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
        }

        // Bottom CTA
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Wellness.colors.bg)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            PrimaryActionButton(label = "Сохранить", onClick = onBack)
        }
    }
}

@Composable
private fun DayRow(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    Row(
        Modifier
            .screenHPad()
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val iso = i + 1
            val active = iso in selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(
                        if (active) Wellness.colors.accentSoft else Wellness.colors.track,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable {
                        onChange(if (active) selected - iso else selected + iso)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) Wellness.colors.accent else Wellness.colors.text,
                    style = Wellness.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun RowScope.DayPreset(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
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

@Composable
private fun SwitchPill(on: Boolean, onToggle: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(width = 48.dp, height = 28.dp)
            .background(
                if (on) Wellness.colors.accent else Wellness.colors.muted.copy(alpha = 0.35f),
                RoundedCornerShape(999.dp),
            )
            .clickable { onToggle(!on) },
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .size(22.dp)
                .background(Color.White, CircleShape),
        )
    }
}
