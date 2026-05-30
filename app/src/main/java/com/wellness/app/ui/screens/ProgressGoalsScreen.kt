package com.wellness.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wellness.app.ui.components.ElasticOverscroll
import com.wellness.app.ui.components.IconButtonRound
import com.wellness.app.ui.components.NoFeedbackButton
import com.wellness.app.ui.components.ProgressRing
import com.wellness.app.ui.components.SettingsHeader
import com.wellness.app.ui.components.rememberElasticOverscroll
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.GoalBreakdown
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.SleepEntry
import com.wellness.app.ui.state.WeightEntry
import com.wellness.app.ui.state.calculateGoalProgress
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// ── Shared helpers ─────────────────────────────────────────────────────

private val SHORT_DOW = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private val DAY_MONTH = DateTimeFormatter.ofPattern("dd.MM")

private fun parseDate(dateKey: String): LocalDate? =
    runCatching { LocalDate.parse(dateKey) }.getOrNull()

private fun shortDow(dateKey: String): String =
    parseDate(dateKey)?.let { SHORT_DOW[it.dayOfWeek.value - 1] } ?: ""

private fun formatHm(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}ч" else "${h}ч ${m}м"
}

private fun sleepQualityLabel(q: Int): String =
    listOf("плохо", "так себе", "норм", "отлично").getOrElse(q) { "норм" }

private fun oneDecimalComma(v: Float): String = "%.1f".format(v).replace('.', ',')

// Detail routes. The calm overview is shown when no metric is open (null);
// tapping a metric row opens one of these full detail screens.
private const val WEIGHT = 0
private const val SLEEP = 1
private const val STEPS = 2

// Date windows for the period control, in days.
private enum class Period(val title: String, val days: Long) {
    WEEK("Неделя", 7),
    MONTH("Месяц", 31),
    YEAR("Год", 366),
}

/**
 * "Прогресс целей" — r52, вариант «Спокойный».
 *
 * One calm vertical screen. No swipe, no top tabs. The top of the screen has a
 * single focus — the overall progress ring with a short status line. Below it a
 * quiet list of metrics (Вес / Сон / Шаги): each row shows an icon, the metric
 * name, its current value, a small sparkline and a trend pill. Tapping a row
 * opens that metric's full detail screen (big value + period control + chart +
 * "Записать…" button) — charts are revealed on tap, so the overview itself
 * stays light and easy to read.
 *
 * Weight + Sleep entry are root-level bottom sheets driven by [onAddWeight] /
 * [onAddSleep] — they do NOT push onto the overlay stack, so this screen never
 * re-mounts and the open detail / scroll position / period never snap back.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProgressGoalsScreen(
    onBack: () -> Unit,
    onAddWeight: () -> Unit = {},
    onAddSleep: () -> Unit = {},
) {
    val state = LocalAppState.current
    val breakdown = calculateGoalProgress(state)

    // null = calm overview only. Tapping a metric row opens its detail via a
    // container-transform overlay: the card surface morphs to full screen while
    // its content settles in (it does NOT stretch), and a swipe down dismisses.
    var open by remember { mutableStateOf<Int?>(null) }
    var bounds by remember { mutableStateOf<CardBounds?>(null) }

    val scroll = remember { ScrollState(0) }
    val elastic = rememberElasticOverscroll(maxVertical = 56.dp, maxHorizontal = 0.dp)
    val topInset =
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            Box(Modifier.fillMaxSize().nestedScroll(elastic.connection)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = elastic.verticalOverscroll.floatValue }
                        .verticalScroll(scroll)
                        .padding(top = topInset, bottom = 160.dp),
                ) {
                    SettingsHeader(title = "Прогресс целей", onBack = onBack)
                    OverviewCalm(
                        breakdown = breakdown,
                        hidden = open,
                        onOpen = { id, b -> bounds = b; open = id },
                    )
                    Box(Modifier.height(24.dp))
                }
            }
        }

        val cur = open
        val b = bounds
        if (cur != null && b != null) {
            MetricMorphOverlay(
                metric = cur,
                bounds = b,
                topInset = topInset,
                onAddWeight = onAddWeight,
                onAddSleep = onAddSleep,
                onClosed = { open = null },
            )
        }
    }
}

// ── Calm overview ──────────────────────────────────────────────────────

@Composable
private fun OverviewCalm(
    breakdown: GoalBreakdown,
    hidden: Int?,
    onOpen: (Int, CardBounds) -> Unit,
) {
    val state = LocalAppState.current
    val percent = (breakdown.overall * 100f).toInt()

    val weightSorted = state.weightLog.sortedBy { it.dateKey }
    val curWeight = weightSorted.lastOrNull()?.kg

    // Weekly weight delta from real weigh-ins (drives the weight trend pill).
    val weeklyDelta: Float? = run {
        val last = weightSorted.lastOrNull()
        if (last == null || weightSorted.size < 2) null
        else {
            val weekAgo = parseDate(last.dateKey)?.minusDays(7)
            val ref = weekAgo?.let { wa ->
                weightSorted.lastOrNull { e -> parseDate(e.dateKey)?.let { it <= wa } == true }
            } ?: weightSorted.first()
            last.kg - ref.kg
        }
    }

    val sleepSorted = state.sleepLog.sortedBy { it.dateKey }
    val lastSleep = sleepSorted.lastOrNull()

    // ── Single focus: overall ring + short status line ──
    Row(
        Modifier.fillMaxWidth().screenHPad().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = breakdown.overall,
                color = Wellness.colors.accent,
                size = 92.dp,
                strokeWidth = 9.dp,
                discFillAlpha = 0.10f,
            )
            Text(
                "$percent%",
                color = Wellness.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        }
        Box(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (percent >= 50) "Идёшь по плану" else "Только начало",
                color = Wellness.colors.text,
                style = Wellness.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Box(Modifier.height(3.dp))
            Text(
                "Общий прогресс целей",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodyMedium,
            )
            if (weeklyDelta != null && abs(weeklyDelta) >= 0.05f) {
                val loss = weeklyDelta < 0f
                Text(
                    "За неделю ${if (loss) "↓" else "↑"} ${oneDecimalComma(abs(weeklyDelta))} кг",
                    color = if (loss) WellnessColors.Mint else WellnessColors.Pink,
                    style = Wellness.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    Box(Modifier.height(20.dp))
    CalmSectionLabel("Мои показатели")
    Box(Modifier.height(12.dp))

    Column(Modifier.screenHPad(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Вес
        MetricRow(
            metric = WEIGHT,
            icon = "scale-bold-duotone",
            color = Wellness.colors.accent,
            name = "Вес",
            value = curWeight?.let { "${oneDecimalComma(it)} кг" } ?: "—",
            spark = weightSorted.takeLast(7).map { it.kg }.takeIf { it.size >= 2 },
            trend = weeklyDelta?.takeIf { abs(it) >= 0.05f }?.let {
                val loss = it < 0f
                Triple(
                    "${if (loss) "↓" else "↑"} ${oneDecimalComma(abs(it))} кг",
                    if (loss) WellnessColors.Mint else WellnessColors.Pink,
                    true,
                )
            },
            hidden = hidden == WEIGHT,
            onOpen = onOpen,
        )
        // Сон
        MetricRow(
            metric = SLEEP,
            icon = "moon-sleep-bold-duotone",
            color = WellnessColors.Water,
            name = "Сон",
            value = lastSleep?.let { formatHm(it.durationMinutes) } ?: "—",
            spark = sleepSorted.takeLast(7).map { it.durationMinutes.toFloat() }
                .takeIf { it.size >= 2 && it.any { v -> v > 0f } },
            trend = lastSleep?.let {
                Triple(sleepQualityLabel(it.quality), WellnessColors.Water, false)
            },
            hidden = hidden == SLEEP,
            onOpen = onOpen,
        )
        // Шаги (coming soon)
        MetricRow(
            metric = STEPS,
            icon = "walking-bold-duotone",
            color = WellnessColors.Mint,
            name = "Шаги",
            value = "—",
            spark = null,
            trend = Triple("скоро", Wellness.colors.muted, false),
            hidden = hidden == STEPS,
            onOpen = onOpen,
        )
    }
}

@Composable
private fun CalmSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Wellness.colors.muted,
        style = Wellness.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.screenHPad().padding(start = 6.dp),
    )
}

/**
 * One quiet metric row: icon · name + value · sparkline + trend pill, tappable
 * to open the metric detail. [trend] is (text, color, withArrow) — withArrow
 * shows a small caret for the weight delta; sleep/steps pills carry no arrow.
 */
@Composable
private fun MetricRow(
    metric: Int,
    icon: String,
    color: Color,
    name: String,
    value: String,
    spark: List<Float>?,
    trend: Triple<String, Color, Boolean>?,
    hidden: Boolean,
    onOpen: (Int, CardBounds) -> Unit,
) {
    // Root-space bounds of the card, its icon box and its name label — handed to
    // the overlay so the morph and the flying icon/name start exactly here.
    var cardR by remember { mutableStateOf(Rect.Zero) }
    var iconR by remember { mutableStateOf(Rect.Zero) }
    var nameR by remember { mutableStateOf(Rect.Zero) }
    NoFeedbackButton(
        onClick = { onOpen(metric, CardBounds(cardR, iconR, nameR, color, icon, name)) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { cardR = it.boundsInRoot() }
                .clip(RoundedCornerShape(22.dp))
                .background(Wellness.colors.container, RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .onGloballyPositioned { iconR = it.boundsInRoot() }
                    .graphicsLayer { alpha = if (hidden) 0f else 1f }
                    .background(color.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { SolarIcon(name = icon, tint = color, size = 24.dp) }
            Box(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodySmall,
                    modifier = Modifier
                        .onGloballyPositioned { nameR = it.boundsInRoot() }
                        .graphicsLayer { alpha = if (hidden) 0f else 1f },
                )
                Box(Modifier.height(2.dp))
                Text(
                    value,
                    color = Wellness.colors.text,
                    style = Wellness.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (spark != null) {
                    Sparkline(data = spark, color = color, width = 82.dp, height = 30.dp)
                    Box(Modifier.height(8.dp))
                }
                if (trend != null) {
                    CalmTrendPill(text = trend.first, color = trend.second, withArrow = trend.third)
                }
            }
        }
    }
}

// ── Container-transform overlay ────────────────────────────────────────

/** Root-space geometry + identity of a tapped metric card. */
private data class CardBounds(
    val card: Rect,
    val icon: Rect,
    val name: Rect,
    val color: Color,
    val iconName: String,
    val title: String,
)

/**
 * The detail screen as a container transform of the tapped card.
 *
 * Only an EMPTY surface morphs from the card rect to full screen (size +
 * corner radius + a small bg→container colour blend). The real content lives
 * in a separate layer that is NOT scaled by the morph — it just fades into
 * place, so nothing stretches. The metric icon + name fly from the card to the
 * detail header. A downward drag on the header dismisses the sheet (it follows
 * the finger and scales down a touch); release past a threshold, hardware back,
 * or the ✕ button reverse-morphs it back into the card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MetricMorphOverlay(
    metric: Int,
    bounds: CardBounds,
    topInset: Dp,
    onAddWeight: () -> Unit,
    onAddSleep: () -> Unit,
    onClosed: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }

    // Calm, slightly-damped spring — matches the web prototype's feel.
    val morphSpec = spring<Float>(dampingRatio = 0.9f, stiffness = 340f)

    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            launch { dragY.animateTo(0f, spring(stiffness = 700f)) }
            progress.animateTo(0f, morphSpec)
            onClosed()
        }
    }

    LaunchedEffect(Unit) { progress.animateTo(1f, morphSpec) }
    BackHandler(enabled = true) { close() }

    // Measured target rects of the detail-header icon / name (the flight ends).
    var iconEnd by remember { mutableStateOf(Rect.Zero) }
    var nameEnd by remember { mutableStateOf(Rect.Zero) }
    val bodyScroll = rememberScrollState()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wpx = constraints.maxWidth.toFloat()
        val hpx = constraints.maxHeight.toFloat()
        val p = progress.value
        val dragFrac = (dragY.value / hpx).coerceIn(0f, 1f)
        val contentAlpha = ((p - 0.18f) / 0.55f).coerceIn(0f, 1f)

        // ── backdrop dim (under the sheet) ──
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.45f * p * (1f - dragFrac * 0.85f) }
                .background(Color.Black),
        )

        // ── sheet group: surface + content, drag-translated & scaled together ──
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationY = dragY.value
                val s = 1f - dragFrac * 0.12f
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0.5f, 0f)
            },
        ) {
            // Surface — the only thing that morphs. Empty, so no content stretch.
            val left = lerp(bounds.card.left, 0f, p)
            val top = lerp(bounds.card.top, 0f, p)
            val w = lerp(bounds.card.width, wpx, p)
            val h = lerp(bounds.card.height, hpx, p)
            val cornerDp = with(density) { lerp(22.dp.toPx(), 0f, p).toDp() }
            val surfaceColor = androidx.compose.ui.graphics.lerp(
                Wellness.colors.container,
                Wellness.colors.bg,
                (p * 2.2f).coerceIn(0f, 1f),
            )
            Box(
                Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                    .clip(RoundedCornerShape(cornerDp))
                    .background(surfaceColor),
            )

            // Content layer — fixed full-screen layout, fades in (never scaled).
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topInset))

                // Header zone (fixed). Drag down here to dismiss.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dy ->
                                    change.consume()
                                    scope.launch {
                                        dragY.snapTo((dragY.value + dy).coerceAtLeast(0f))
                                    }
                                },
                                onDragEnd = {
                                    if (dragY.value > with(density) { 120.dp.toPx() }) {
                                        close()
                                    } else {
                                        scope.launch { dragY.animateTo(0f, spring(stiffness = 600f)) }
                                    }
                                },
                            )
                        },
                ) {
                    // grab handle
                    Box(
                        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .graphicsLayer { alpha = contentAlpha }
                                .width(40.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Wellness.colors.track),
                        )
                    }
                    // header row: icon slot · title · close
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .onGloballyPositioned { iconEnd = it.boundsInRoot() }
                                .graphicsLayer { alpha = if (p >= 0.999f) 1f else 0f }
                                .background(bounds.color.copy(alpha = 0.16f), RoundedCornerShape(17.dp)),
                            contentAlignment = Alignment.Center,
                        ) { SolarIcon(name = bounds.iconName, tint = bounds.color, size = 28.dp) }
                        Box(Modifier.width(14.dp))
                        Text(
                            bounds.title,
                            color = Wellness.colors.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { nameEnd = it.boundsInRoot() }
                                .graphicsLayer { alpha = if (p >= 0.999f) 1f else 0f },
                        )
                        NoFeedbackButton(onClick = { close() }, modifier = Modifier.size(44.dp)) {
                            Box(
                                Modifier
                                    .graphicsLayer { alpha = contentAlpha }
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Wellness.colors.container),
                                contentAlignment = Alignment.Center,
                            ) {
                                SolarIcon(
                                    name = "close-circle-bold-duotone",
                                    tint = Wellness.colors.muted,
                                    size = 24.dp,
                                )
                            }
                        }
                    }
                }

                // Body (scrolls), fades in on its own layer.
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer { alpha = contentAlpha },
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(bodyScroll)
                            .padding(top = 6.dp, bottom = 40.dp),
                    ) {
                        when (metric) {
                            WEIGHT -> WeightSection(onAddWeight = onAddWeight)
                            SLEEP -> SleepSection(onAddSleep = onAddSleep)
                            else -> StepsSection()
                        }
                    }
                }
            }
        }

        // ── Flying icon + name (travel card → header), in pure root space ──
        if (p < 0.999f && iconEnd != Rect.Zero && nameEnd != Rect.Zero) {
            // icon
            val ix = lerp(bounds.icon.left, iconEnd.left, p)
            val iy = lerp(bounds.icon.top, iconEnd.top, p)
            val isz = lerp(bounds.icon.width, iconEnd.width, p)
            val iCorner = with(density) { lerp(14.dp.toPx(), 17.dp.toPx(), p).toDp() }
            val glyph = with(density) { lerp(24.dp.toPx(), 28.dp.toPx(), p).toDp() }
            Box(
                Modifier
                    .offset { IntOffset(ix.roundToInt(), iy.roundToInt()) }
                    .size(with(density) { isz.toDp() })
                    .clip(RoundedCornerShape(iCorner))
                    .background(bounds.color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { SolarIcon(name = bounds.iconName, tint = bounds.color, size = glyph) }
        }
    }
}

@Composable
private fun CalmTrendPill(text: String, color: Color, withArrow: Boolean) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .padding(horizontal = if (withArrow) 9.dp else 10.dp, vertical = 5.dp),
    ) {
        Text(
            text,
            color = color,
            style = Wellness.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Tiny smooth sparkline (no axes) ending in a filled dot — mirrors the web
 * prototype's Spark. Draws via the shared [smoothPath] cubic builder.
 */
@Composable
private fun Sparkline(data: List<Float>, color: Color, width: Dp, height: Dp) {
    Canvas(Modifier.size(width, height)) {
        val pad = 4.dp.toPx()
        val min = data.min()
        val max = data.max()
        val range = (max - min).takeIf { it > 0.0001f } ?: 1f
        val w = size.width
        val h = size.height
        val pts = data.mapIndexed { i, v ->
            val x = if (data.size > 1) pad + i * (w - pad * 2) / (data.size - 1) else w / 2f
            val y = pad + (1f - (v - min) / range) * (h - pad * 2)
            Offset(x, y)
        }
        if (pts.size >= 2) {
            drawPath(
                smoothPath(pts),
                color = color,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        val last = pts.last()
        drawCircle(color = color, radius = 3.2.dp.toPx(), center = last)
    }
}


// ── Section: Вес ───────────────────────────────────────────────────────

@Composable
private fun WeightSection(onAddWeight: () -> Unit) {
    val state = LocalAppState.current
    var period by remember { mutableStateOf(Period.WEEK) }
    val goal = state.weightGoal

    val all = state.weightLog.sortedBy { it.dateKey }
    val cutoff = LocalDate.now().minusDays(period.days)
    val windowed = all.filter { parseDate(it.dateKey)?.let { d -> !d.isBefore(cutoff) } ?: false }
    val entries = windowed.ifEmpty { all.takeLast(1) }
    val points = entries.map { it.kg }
    val hasData = points.isNotEmpty()
    val current = points.lastOrNull() ?: state.weight
    val delta = if (points.size >= 2) points.last() - points.first() else 0f
    val avg = if (hasData) points.average().toFloat() else 0f
    val remaining = (current - goal).coerceAtLeast(0f)

    Column(Modifier.screenHPad().padding(start = 4.dp)) {
        Text("Текущий вес", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (hasData) oneDecimalComma(current) else "—",
                color = Wellness.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 44.sp,
            )
            Text(" кг", color = Wellness.colors.muted, style = Wellness.typography.titleMedium)
            if (points.size >= 2) {
                Box(Modifier.width(10.dp))
                val loss = delta < 0f
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            (if (loss) WellnessColors.Mint else WellnessColors.Pink).copy(alpha = 0.16f),
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${if (loss) "↓" else "↑"} ${oneDecimalComma(abs(delta))} кг",
                        color = if (loss) WellnessColors.Mint else WellnessColors.Pink,
                        style = Wellness.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    Box(Modifier.height(16.dp))
    PeriodControl(period = period, onSelect = { period = it })
    Box(Modifier.height(16.dp))

    if (hasData) {
        SwipeLineChart(entries = entries, goalKg = goal, period = period)
    } else {
        EmptyState("Запишите вес, чтобы увидеть динамику")
    }

    Box(Modifier.height(18.dp))
    Row(Modifier.screenHPad(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(Modifier.weight(1f), "Среднее", if (hasData) "${oneDecimalComma(avg)} кг" else "—")
        StatTile(Modifier.weight(1f), "Цель", "${oneDecimalComma(goal)} кг")
        StatTile(Modifier.weight(1f), "Осталось", "${oneDecimalComma(remaining)} кг")
    }

    Box(Modifier.height(16.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Последние записи",
                color = Wellness.colors.text,
                style = Wellness.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddWeight)
        }
        if (all.isEmpty()) {
            Box(Modifier.height(8.dp))
            Text("Пока нет записей", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        } else {
            val recent = all.reversed().take(6)
            recent.forEachIndexed { i, e ->
                if (i > 0) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Wellness.colors.track))
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        parseDate(e.dateKey)?.format(DAY_MONTH) ?: e.dateKey,
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodyMedium,
                    )
                    Text(
                        "${oneDecimalComma(e.kg)} кг",
                        color = Wellness.colors.text,
                        style = Wellness.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ── Section: Сон ───────────────────────────────────────────────────────

@Composable
private fun SleepSection(onAddSleep: () -> Unit) {
    val state = LocalAppState.current
    var period by remember { mutableStateOf(Period.WEEK) }

    val all = state.sleepLog.sortedBy { it.dateKey }
    val cutoff = LocalDate.now().minusDays(period.days)
    val windowed = all.filter { parseDate(it.dateKey)?.let { d -> !d.isBefore(cutoff) } ?: false }
    val entries = windowed.ifEmpty { all.takeLast(1) }
    val hasData = entries.isNotEmpty() && entries.any { it.durationMinutes > 0 }
    val last = all.lastOrNull()
    val avgMin = if (hasData) entries.map { it.durationMinutes }.average().toInt() else 0
    val bestMin = entries.maxOfOrNull { it.durationMinutes } ?: 0
    val goalMin = state.sleepGoalMinutes
    val goalText = if (goalMin % 60 == 0) "${goalMin / 60} ч" else formatHm(goalMin)

    Column(Modifier.screenHPad().padding(start = 4.dp)) {
        Text("Прошлой ночью", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                last?.let { formatHm(it.durationMinutes) } ?: "—",
                color = Wellness.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 44.sp,
            )
            if (last != null) {
                Box(Modifier.width(10.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(WellnessColors.Water.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        sleepQualityLabel(last.quality),
                        color = WellnessColors.Water,
                        style = Wellness.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    Box(Modifier.height(16.dp))
    PeriodControl(period = period, onSelect = { period = it })
    Box(Modifier.height(16.dp))

    if (hasData) {
        Row(
            Modifier.screenHPad().fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Длительность сна", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
            Text("цель $goalText", color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        }
        Box(Modifier.height(10.dp))
        SleepBars(entries = entries, goalMin = goalMin)
    } else {
        EmptyState("Запишите сон, чтобы увидеть статистику")
    }

    Box(Modifier.height(18.dp))
    Row(Modifier.screenHPad(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(Modifier.weight(1f), "Среднее", if (hasData) oneDecimalComma(avgMin / 60f) + " ч" else "—")
        StatTile(Modifier.weight(1f), "Лучшая", if (bestMin > 0) oneDecimalComma(bestMin / 60f) + " ч" else "—")
        StatTile(Modifier.weight(1f), "Цель", goalText)
    }

    Box(Modifier.height(16.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Записать сон",
            color = Wellness.colors.text,
            style = Wellness.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddSleep)
    }
}

// ── Section: Шаги (coming soon) ────────────────────────────────────────

@Composable
private fun StepsSection() {
    Box(Modifier.height(8.dp))
    Column(
        Modifier.fillMaxWidth().screenHPad(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = 0f,
                color = WellnessColors.Mint,
                size = 132.dp,
                strokeWidth = 12.dp,
                discFillAlpha = 0.10f,
            )
            SolarIcon(name = "walking-bold-duotone", tint = WellnessColors.Mint, size = 44.dp)
        }
        Box(Modifier.height(18.dp))
        Text(
            "Шаги скоро здесь",
            color = Wellness.colors.text,
            style = Wellness.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Box(Modifier.height(6.dp))
        Text(
            "Здесь будут ваши шаги, дистанция и активность за день и за неделю.",
            color = Wellness.colors.muted,
            style = Wellness.typography.bodyMedium,
        )
    }

    Box(Modifier.height(20.dp))
    Row(Modifier.screenHPad(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(Modifier.weight(1f), "Дистанция", "—")
        StatTile(Modifier.weight(1f), "Калории", "—")
        StatTile(Modifier.weight(1f), "Активность", "—")
    }

    Box(Modifier.height(16.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(WellnessColors.Mint.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(WellnessColors.Mint.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { SolarIcon(name = "walking-bold-duotone", tint = WellnessColors.Mint, size = 22.dp) }
        Box(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Скоро: встроенный шагомер",
                color = Wellness.colors.text,
                style = Wellness.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Box(Modifier.height(2.dp))
            Text(
                "Шаги будут считаться автоматически с датчиков телефона.",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall,
            )
        }
    }
}

// ── Reusable bits ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Wellness.colors.text,
        style = Wellness.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.screenHPad().padding(start = 4.dp),
    )
}

/**
 * Segmented period control (Неделя / Месяц / Год) with a single pill that
 * slides smoothly between segments via [animateDpAsState] when the selection
 * changes.
 */
@Composable
private fun PeriodControl(period: Period, onSelect: (Period) -> Unit) {
    val items = Period.entries
    val selected = items.indexOf(period).coerceAtLeast(0)
    Box(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(14.dp))
            .background(Wellness.colors.track, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(36.dp)) {
            val n = items.size
            val cell = maxWidth / n
            val x by animateDpAsState(
                targetValue = cell * selected,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                label = "periodPill",
            )
            Box(
                Modifier
                    .offset(x = x)
                    .width(cell)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Wellness.colors.container, RoundedCornerShape(10.dp)),
            )
            Row(Modifier.fillMaxSize()) {
                items.forEach { p ->
                    val active = p == period
                    NoFeedbackButton(
                        onClick = { onSelect(p) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                p.title,
                                color = if (active) Wellness.colors.text else Wellness.colors.muted,
                                style = Wellness.typography.bodyMedium,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier, label: String, value: String) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Wellness.colors.container, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(label, color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
        Box(Modifier.height(4.dp))
        Text(value, color = Wellness.colors.text, style = Wellness.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        Modifier.fillMaxWidth().screenHPad().height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Wellness.colors.muted, style = Wellness.typography.bodyMedium)
    }
}

// ── Charts ─────────────────────────────────────────────────────────────

/**
 * Build a smooth (rounded) path through [pts] using a cubic bezier per
 * segment with horizontal control tangents. Gives the gentle S-curve look of
 * iOS-style line charts instead of hard polyline corners.
 */
private fun smoothPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
        val p0 = pts[i - 1]
        val p1 = pts[i]
        val cx = (p0.x + p1.x) / 2f
        path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
    }
    return path
}

/**
 * Full-bleed weight line chart with a draggable scrubber. Smooth cubic line,
 * vertical gradient fill, subtle baseline grid and a dashed goal line. Touch
 * anywhere and drag: the nearest weigh-in highlights and a floating label
 * shows its exact value + date. No card background — it sits on the page.
 */
@Composable
private fun SwipeLineChart(entries: List<WeightEntry>, goalKg: Float, period: Period) {
    val points = entries.map { it.kg }
    val color = Wellness.colors.accent
    val bg = Wellness.colors.bg
    val muted = Wellness.colors.muted
    val grid = Wellness.colors.track
    val n = points.size
    val density = LocalDensity.current

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var active by remember(entries, period) { mutableStateOf<Int?>(null) }

    val rawMin = minOf(points.min(), goalKg) - 0.6f
    val rawMax = maxOf(points.max(), goalKg) + 0.6f
    val hPadPx = with(density) { 12.dp.toPx() }

    fun xAt(i: Int, w: Float): Float {
        val usable = w - hPadPx * 2
        return if (n > 1) hPadPx + i * (usable / (n - 1)) else w / 2f
    }
    fun nearest(x: Float): Int {
        if (canvasSize.width == 0) return 0
        val w = canvasSize.width.toFloat()
        var best = 0
        var bd = Float.MAX_VALUE
        for (i in 0 until n) {
            val d = abs(xAt(i, w) - x)
            if (d < bd) { bd = d; best = i }
        }
        return best
    }

    Box(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(entries, period) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        active = nearest(down.position.x)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { ch ->
                                active = nearest(ch.position.x)
                                ch.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        active = null
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val vPad = 22.dp.toPx()
            fun yAt(v: Float): Float = vPad + (1f - (v - rawMin) / (rawMax - rawMin)) * (h - vPad * 2)

            // Subtle horizontal grid lines (top, middle, bottom of plot area).
            val plotTop = vPad
            val plotBottom = h - vPad
            listOf(0f, 0.5f, 1f).forEach { f ->
                val gy = plotTop + (plotBottom - plotTop) * f
                drawLine(
                    color = grid.copy(alpha = 0.6f),
                    start = Offset(hPadPx, gy),
                    end = Offset(w - hPadPx, gy),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // Dashed goal line.
            val goalY = yAt(goalKg)
            drawLine(
                color = muted.copy(alpha = 0.45f),
                start = Offset(hPadPx, goalY),
                end = Offset(w - hPadPx, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 7.dp.toPx()), 0f),
            )

            if (n > 1) {
                val pts = points.mapIndexed { i, v -> Offset(xAt(i, w), yAt(v)) }
                val line = smoothPath(pts)
                // Gradient area under the curve.
                val area = Path().apply {
                    addPath(line)
                    lineTo(pts.last().x, h)
                    lineTo(pts.first().x, h)
                    close()
                }
                drawPath(
                    area,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.02f)),
                        startY = plotTop,
                        endY = h,
                    ),
                )
                // Soft underlay stroke + crisp line on top.
                drawPath(line, color = color.copy(alpha = 0.18f), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
                drawPath(line, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }

            val act = active
            if (act != null && act in points.indices) {
                val ax = xAt(act, w)
                drawLine(
                    color = color.copy(alpha = 0.4f),
                    start = Offset(ax, plotTop),
                    end = Offset(ax, plotBottom),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            points.forEachIndexed { i, v ->
                val on = active == i
                val r = if (on) 6.dp.toPx() else 3.5.dp.toPx()
                if (on) {
                    drawCircle(color = color.copy(alpha = 0.18f), radius = 12.dp.toPx(), center = Offset(xAt(i, w), yAt(v)))
                }
                drawCircle(color = color, radius = r, center = Offset(xAt(i, w), yAt(v)))
                drawCircle(color = bg, radius = if (on) 2.5.dp.toPx() else 2.dp.toPx(), center = Offset(xAt(i, w), yAt(v)))
            }
        }

        // Floating value label
        val act = active
        if (act != null && act in entries.indices && canvasSize.width > 0) {
            val w = canvasSize.width.toFloat()
            val labelWidthDp = 116.dp
            val labelHalfPx = with(density) { (labelWidthDp / 2).toPx() }
            val ax = xAt(act, w)
            val clampedX = ax.coerceIn(labelHalfPx, w - labelHalfPx)
            val xDp = with(density) { (clampedX - labelHalfPx).toDp() }
            Box(
                Modifier
                    .offset(x = xDp, y = 0.dp)
                    .width(labelWidthDp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Wellness.colors.text, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "${oneDecimalComma(entries[act].kg)} кг",
                            color = Wellness.colors.bg,
                            style = Wellness.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(Modifier.height(2.dp))
                    Text(
                        parseDate(entries[act].dateKey)?.format(DAY_MONTH) ?: "",
                        color = Wellness.colors.muted,
                        style = Wellness.typography.bodySmall,
                    )
                }
            }
        }
    }

    // X-axis labels
    Box(Modifier.height(6.dp))
    if (period == Period.WEEK && n <= 7) {
        Row(
            Modifier.fillMaxWidth().screenHPad(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            entries.forEach {
                Text(shortDow(it.dateKey), color = Wellness.colors.muted, style = Wellness.typography.bodySmall)
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().screenHPad(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                parseDate(entries.first().dateKey)?.format(DAY_MONTH) ?: "",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall,
            )
            Text(
                parseDate(entries.last().dateKey)?.format(DAY_MONTH) ?: "",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodySmall,
            )
        }
    }
}

/**
 * Full-bleed sleep bars. Tap a bar to highlight it and show its duration.
 */
@Composable
private fun SleepBars(entries: List<SleepEntry>, goalMin: Int) {
    val maxMin = (entries.maxOfOrNull { it.durationMinutes } ?: goalMin)
        .coerceAtLeast(goalMin)
        .coerceAtLeast(1)
    var selected by remember(entries) { mutableStateOf(entries.lastIndex) }

    Column(Modifier.fillMaxWidth().screenHPad()) {
        if (selected in entries.indices) {
            val e = entries[selected]
            Text(
                "${shortDow(e.dateKey).ifEmpty { parseDate(e.dateKey)?.format(DAY_MONTH) ?: "" }} · ${formatHm(e.durationMinutes)}",
                color = Wellness.colors.text,
                style = Wellness.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.height(10.dp))
        }
        Row(
            Modifier.fillMaxWidth().height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            entries.forEachIndexed { i, e ->
                val fraction = (e.durationMinutes.toFloat() / maxMin).coerceIn(0f, 1f)
                val on = i == selected
                NoFeedbackButton(onClick = { selected = i }, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(24.dp)
                                .height((14 + fraction * 96).dp)
                                .background(
                                    if (on) WellnessColors.Water else WellnessColors.Water.copy(alpha = 0.35f),
                                    RoundedCornerShape(999.dp),
                                ),
                        )
                        Box(Modifier.height(6.dp))
                        Text(
                            shortDow(e.dateKey),
                            color = if (on) Wellness.colors.text else Wellness.colors.muted,
                            style = Wellness.typography.bodySmall,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
