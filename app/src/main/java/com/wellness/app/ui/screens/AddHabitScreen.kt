package com.wellness.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.wellness.app.ui.components.ColorPickerGrid
import com.wellness.app.ui.components.HeaderCheckButton
import com.wellness.app.ui.components.ProgressRing
import com.wellness.app.ui.components.OverlayHost
import com.wellness.app.ui.components.RoundedSlideOverlay
import com.wellness.app.ui.components.SettingsCard
import com.wellness.app.ui.components.SettingsHeader
import com.wellness.app.ui.components.SettingsRow
import com.wellness.app.ui.components.SettingsRowDivider
import com.wellness.app.ui.components.WheelPicker
import com.wellness.app.ui.components.noFeedbackClick
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
    val color: Color = AccentPalette[3],
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
 * "Create habit" — settings-list flow with modal bottom sheets for icon and
 * colour, wheel-picker goal/time selection, and a top-right checkmark
 * commit in the header (no pinned bottom CTA).
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

private enum class InlinePanel { None, Icon, Color }

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
    var panel by remember { mutableStateOf(InlinePanel.None) }

    // Width of the rows (so the popovers can match it exactly).
    var rowWidthPx by remember { mutableStateOf(0) }

    // Dismiss any open popover the moment the user scrolls the screen.
    LaunchedEffect(Unit) {
        snapshotFlow { scroll.value }.collect {
            if (panel != InlinePanel.None) panel = InlinePanel.None
        }
    }

    val canCreate = draft.name.trim().isNotEmpty()

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
                .padding(top = 6.dp, bottom = 24.dp),
        ) {
            SettingsHeader(
                title = "Новая привычка",
                onBack = onBack,
                trailing = {
                    HeaderCheckButton(enabled = canCreate, onClick = onCreate)
                },
            )

            // Hero — the ring+icon badge that the habit shows on the Home
            // and Plan screens, rendered live above the name field so the
            // user previews the exact artwork they're configuring.
            RingHeroPreview(icon = draft.icon, color = draft.color)
            // Name input — back to a regular full-width text field row
            // (no centered hero text). Slim Telegram-style container.
            Box(Modifier.screenHPad().padding(top = 14.dp)) {
                HabitNameField(
                    name = draft.name,
                    onChange = { onDraft(draft.copy(name = it)) },
                )
            }

            SectionLabel("ПАРАМЕТРЫ", topPad = 22.dp)
            SettingsCard(
                modifier = Modifier
                    .screenHPad()
                    .onGloballyPositioned { rowWidthPx = it.size.width },
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

                // ── Иконка: row anchors a floating popover above content ─
                Box {
                    SettingsRow(
                        icon = "stars-bold-duotone",
                        iconTile = WellnessColors.TilePink,
                        title = "Иконка",
                        showChevron = false,
                        trailing = { MiniIconPreview(icon = draft.icon, color = draft.color) },
                        onClick = {
                            panel = if (panel == InlinePanel.Icon) InlinePanel.None else InlinePanel.Icon
                        },
                    )
                    FloatingAnchoredPopover(
                        visible = panel == InlinePanel.Icon,
                        widthPx = rowWidthPx,
                        onDismiss = { panel = InlinePanel.None },
                    ) {
                        IconGrid(
                            selected = draft.icon,
                            tint = draft.color,
                            onSelect = { onDraft(draft.copy(icon = it)) },
                        )
                    }
                }
                SettingsRowDivider()

                // ── Цвет: floating popover ──────────────────────────────
                Box {
                    SettingsRow(
                        icon = "star-shine-bold-duotone",
                        iconTile = WellnessColors.TileViolet,
                        title = "Цвет",
                        showChevron = false,
                        trailing = {
                            Box(Modifier.size(26.dp).background(draft.color, CircleShape))
                        },
                        onClick = {
                            panel = if (panel == InlinePanel.Color) InlinePanel.None else InlinePanel.Color
                        },
                    )
                    FloatingAnchoredPopover(
                        visible = panel == InlinePanel.Color,
                        widthPx = rowWidthPx,
                        onDismiss = { panel = InlinePanel.None },
                    ) {
                        ColorPickerGrid(
                            colors = AccentPalette,
                            selected = draft.color,
                            onSelect = { onDraft(draft.copy(color = it)) },
                        )
                    }
                }
                SettingsRowDivider()

                SettingsRow(
                    icon = "bell-bold-duotone",
                    iconTile = WellnessColors.TileOrange,
                    title = "Напомнить",
                    value = draft.reminderLabel(),
                    onClick = onReminder,
                )
            }

            // The "Заметка" block was removed by request — descriptions live
            // implicitly in the habit title now. The note field is kept on
            // the draft model so nothing breaks downstream / on disk; it
            // just isn't editable from this screen any more.

            Spacer(Modifier.height(36.dp))
        }
    }
}

/**
 * A floating popover that anchors to the BOTTOM-START of its parent layout
 * (i.e. the row Box it sits inside) and appears in its own window above
 * everything else — exactly like Telegram's message context menu / folder
 * icon picker. Tap outside / press back to dismiss.
 *
 * - [widthPx]   : the target popover width in pixels (we match the parent
 *                 SettingsCard width so the panel snaps under the row).
 * - [visible]   : when this flips false the popup is unmounted; we drive
 *                 enter animation via a one-shot LaunchedEffect.
 * - [onDismiss] : called on outside-tap / back press.
 */
@Composable
private fun FloatingAnchoredPopover(
    visible: Boolean,
    widthPx: Int,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (widthPx <= 0) return
    val density = LocalDensity.current
    val widthDp = with(density) { widthPx.toDp() }

    // MutableTransitionState lets us keep the Popup mounted while the exit
    // animation is running. `targetState` follows `visible`; `currentState`
    // only flips to false after the exit transition finishes. We render the
    // Popup as long as either is true, which gives us a symmetric in/out
    // scale+fade — exactly like the Telegram context menu.
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = visible
    val mounted = transition.currentState || transition.targetState
    if (!mounted) return

    // Custom provider: anchor the TOP-LEFT of the popup to the
    // BOTTOM-LEFT of the row + a 6dp gap, so the popover hangs cleanly
    // straight under the tapped row instead of stacking above it (which
    // is what Alignment.BottomStart did — that one aligns the popup's
    // BOTTOM to the anchor's BOTTOM, which is why it used to creep up
    // the screen on a tall popup).
    val gapPx = with(density) { 6.dp.roundToPx() }
    val provider = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = anchorBounds.left
                val y = anchorBounds.bottom + gapPx
                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        AnimatedVisibility(
            visibleState = transition,
            enter = scaleIn(
                initialScale = 0.86f,
                animationSpec = tween(durationMillis = 220),
                // Grow out of the row above — origin at top-center of the
                // popup, i.e. directly under the centre of the anchor.
                transformOrigin = TransformOrigin(0.5f, 0f),
            ) + fadeIn(tween(180)),
            exit = scaleOut(
                targetScale = 0.86f,
                animationSpec = tween(durationMillis = 180),
                transformOrigin = TransformOrigin(0.5f, 0f),
            ) + fadeOut(tween(150)),
        ) {
            Box(
                Modifier
                    .width(widthDp)
                    .background(
                        color = Wellness.colors.container,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                content()
            }
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

/**
 * Hero preview at the top of the New/Edit Habit screen. Renders the
 * exact ring-with-icon artwork the user sees on the Plan and Home
 * screens (full-progress ProgressRing in the habit's colour with the
 * picked Solar glyph centred inside the ring). Centred horizontally so
 * the screen reads as "this is the badge you're building" before any
 * settings rows.
 */
@Composable
private fun RingHeroPreview(icon: String, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(96.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProgressRing(
                progress = 1f,
                color = color,
                size = 96.dp,
                strokeWidth = 6.dp,
            )
            SolarIcon(name = icon, tint = color, size = 40.dp)
        }
    }
}

/**
 * Plain Telegram-style title field — full-width container, left-aligned
 * placeholder, native cursor. Replaces the briefly-shipped centred hero
 * field which felt too "form header" rather than an input.
 */
@Composable
private fun HabitNameField(name: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Wellness.colors.container, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
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

@Composable
private fun MiniIconPreview(icon: String, color: Color) {
    Box(
        Modifier
            .size(28.dp)
            .background(color, RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        SolarIcon(name = icon, tint = Color.White, size = 16.dp)
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

/**
 * 6-column dense icon grid (TG-style). Cells are monochrome glyphs sized
 * up so they read clearly inside the floating popover. The selected one
 * tints in the active colour. Selection follows the finger continuously —
 * touch down on a cell and drag across the grid, the highlight tracks
 * the pointer until you lift, exactly like the colour picker right above.
 */
@Composable
private fun IconGrid(selected: String, tint: Color, onSelect: (String) -> Unit) {
    val icons = HabitIconCatalog
    val columns = 6
    val rows = (icons.size + columns - 1) / columns
    val gap = 4.dp
    val cellHeight = 52.dp

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val gapPx = with(density) { gap.toPx() }
    val cellHeightPx = with(density) { cellHeight.toPx() }

    var sizePx by remember { mutableStateOf(IntSize.Zero) }

    fun cellIndex(pos: Offset): Int? {
        if (sizePx.width <= 0) return null
        val cellW = (sizePx.width - gapPx * (columns - 1)) / columns
        val x = pos.x.coerceIn(0f, sizePx.width.toFloat())
        val y = pos.y.coerceAtLeast(0f)
        val col = (x / (cellW + gapPx)).toInt().coerceIn(0, columns - 1)
        val row = (y / (cellHeightPx + gapPx)).toInt().coerceIn(0, rows - 1)
        // Ignore gap strips so dragging through a gap doesn't reselect.
        val colStart = col * (cellW + gapPx)
        val rowStart = row * (cellHeightPx + gapPx)
        if (x - colStart > cellW + 0.5f) return null
        if (y - rowStart > cellHeightPx + 0.5f) return null
        val idx = row * columns + col
        return idx.takeIf { it in icons.indices }
    }

    var lastEmittedIdx by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { sizePx = it }
            .pointerInput(icons) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Main,
                    )
                    cellIndex(down.position)?.let {
                        if (it != lastEmittedIdx) {
                            lastEmittedIdx = it
                            onSelect(icons[it])
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        cellIndex(change.position)?.let {
                            if (it != lastEmittedIdx) {
                                lastEmittedIdx = it
                                onSelect(icons[it])
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        change.consume()
                    }
                    lastEmittedIdx = -1
                }
            },
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        repeat(rows) { r ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                repeat(columns) { c ->
                    val idx = r * columns + c
                    if (idx < icons.size) {
                        val ic = icons[idx]
                        val active = ic == selected
                        Box(
                            Modifier
                                .weight(1f)
                                .height(cellHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            SolarIcon(
                                name = ic,
                                tint = if (active) tint else Wellness.colors.muted,
                                size = 30.dp,
                            )
                        }
                    } else {
                        Box(Modifier.weight(1f).height(cellHeight))
                    }
                }
            }
        }
    }
}

// 36 curated icons → exactly 6 rows × 6 columns. Every row is filled so the
// grid reads as a tidy square; categories spread top-down: drink, food,
// fitness, body / mind, time-of-day, books / misc.
private val HabitIconCatalog: List<String> = listOf(
    // row 1 — drink / hydration
    "bottle-bold-duotone", "cup-paper-bold-duotone", "cup-hot-bold-duotone",
    "tea-cup-bold-duotone", "wineglass-bold-duotone", "waterdrop-bold-duotone",
    // row 2 — food
    "plate-bold-duotone", "donut-bold-duotone", "chef-hat-bold-duotone",
    "pill-bold-duotone", "leaf-bold-duotone", "scale-bold-duotone",
    // row 3 — fitness / movement
    "dumbbell-bold-duotone", "running-bold-duotone", "walking-bold-duotone",
    "bicycling-bold-duotone", "swimming-bold-duotone", "stretching-bold-duotone",
    // row 4 — body / mind / sleep
    "heart-bold-duotone", "heart-pulse-bold-duotone", "meditation-bold-duotone",
    "moon-stars-bold-duotone", "bed-bold-duotone", "smile-circle-bold-duotone",
    // row 5 — time of day / energy
    "sun-bold-duotone", "sunrise-bold-duotone", "sunset-bold-duotone",
    "flame-bold-duotone", "alarm-bold-duotone", "stopwatch-bold-duotone",
    // row 6 — work / study / misc
    "book-bookmark-bold-duotone", "notebook-bold-duotone", "pen-bold-duotone",
    "clipboard-check-bold-duotone", "star-bold-duotone", "stars-bold-duotone",
)

// ───────────────────────────────────────────────────────────────────────────
// GOAL SUB-SCREEN — wheel-based number picker
// ───────────────────────────────────────────────────────────────────────────

private val UnitPresets = listOf("раз", "стак.", "мл", "мин", "км", "стр.", "шаг.", "г", "ккал")
private val TargetValues: List<Int> = (1..999).toList()

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
                .padding(top = 6.dp, bottom = 24.dp),
        ) {
            SettingsHeader(
                title = "Цель",
                onBack = onBack,
                trailing = { HeaderCheckButton(onClick = onBack) },
            )

            // ── Wheel (number) + unit label ──────────────────────────────
            val enabled = draft.goalKind == GoalKind.Count
            Column(
                Modifier
                    .screenHPad()
                    .fillMaxWidth()
                    .background(Wellness.colors.container, RoundedCornerShape(22.dp))
                    .padding(vertical = 12.dp, horizontal = 14.dp),
            ) {
                if (enabled) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        WheelPicker(
                            values = TargetValues,
                            initialIndex = (draft.target - 1).coerceIn(0, TargetValues.lastIndex),
                            modifier = Modifier.weight(2f),
                            visibleItems = 5,
                            onSelected = { _, v -> onDraft(draft.copy(target = v)) },
                            label = { it.toString() },
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            draft.unit.ifBlank { "раз" },
                            color = Wellness.colors.muted,
                            style = Wellness.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp),
                        )
                    }
                } else {
                    // "Просто факт" — show centred check glyph instead of wheel
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(96.dp)
                                .background(
                                    Wellness.colors.accentSoft,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            SolarIcon(
                                name = "check-bold",
                                tint = Wellness.colors.accent,
                                size = 56.dp,
                            )
                        }
                    }
                }
            }

            // Goal kind segmented
            SectionLabel("ТИП ЦЕЛИ", topPad = 22.dp)
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

            if (draft.goalKind == GoalKind.Count) {
                SectionLabel("ЕДИНИЦА ИЗМЕРЕНИЯ", topPad = 22.dp)
                Column(Modifier.screenHPad()) {
                    ChipWrap(
                        options = UnitPresets,
                        selected = draft.unit,
                        onSelect = { onDraft(draft.copy(unit = it)) },
                    )
                }

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
            .noFeedbackClick { onClick() },
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
                            .noFeedbackClick { onSelect(opt) }
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
// REMINDER SUB-SCREEN — wheel HH:MM
// ───────────────────────────────────────────────────────────────────────────

private val HourValues: List<Int> = (0..23).toList()
private val MinuteValues: List<Int> = (0..59).toList()

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
                .padding(top = 6.dp, bottom = 24.dp),
        ) {
            SettingsHeader(
                title = "Напоминание",
                onBack = onBack,
                trailing = { HeaderCheckButton(onClick = onBack) },
            )

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

            // Push toggle
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
            }

            // Time wheels
            if (draft.remind) {
                SectionLabel("ВРЕМЯ", topPad = 22.dp)
                Box(
                    Modifier
                        .screenHPad()
                        .fillMaxWidth()
                        .background(Wellness.colors.container, RoundedCornerShape(22.dp))
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        WheelPicker(
                            values = HourValues,
                            initialIndex = draft.remindH.coerceIn(0, 23),
                            modifier = Modifier.weight(1f),
                            visibleItems = 5,
                            onSelected = { _, v -> onDraft(draft.copy(remindH = v)) },
                            label = { "%02d".format(it) },
                        )
                        Text(
                            text = ":",
                            color = Wellness.colors.text,
                            style = Wellness.typography.displayMedium,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        WheelPicker(
                            values = MinuteValues,
                            initialIndex = draft.remindM.coerceIn(0, 59),
                            modifier = Modifier.weight(1f),
                            visibleItems = 5,
                            onSelected = { _, v -> onDraft(draft.copy(remindM = v)) },
                            label = { "%02d".format(it) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
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
                        CircleShape,
                    )
                    .noFeedbackClick {
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
            .noFeedbackClick { onClick() },
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
            .noFeedbackClick { onToggle(!on) },
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
