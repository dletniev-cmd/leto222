package com.wellness.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.theme.Wellness
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * iOS-style wheel/drum picker. Each row is fixed-height; the middle slot is
 * the "selected" slot. Rows above/below the centre smoothly fade and shrink
 * — no hard edge, no clipping. A DstIn alpha gradient turns the top and
 * bottom of the column transparent so the rendered glyphs dissolve into the
 * sheet background instead of being cropped by a rectangular clip.
 *
 * Snap-fling makes the inertial scroll always land a row exactly in the
 * middle slot. Haptic ticks fire on every index change, matching the iOS
 * spinner feel.
 *
 * The pickable values are passed via [values]; per-row labels come from
 * [label]. The currently selected row is reported through [onSelected]
 * every time the centre row changes (initial value is suppressed).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    values: List<T>,
    initialIndex: Int,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 44.dp,
    visibleItems: Int = 5,
    textStyle: TextStyle = Wellness.typography.headlineLarge,
    selectedTextStyle: TextStyle = Wellness.typography.displayMedium,
    onSelected: (index: Int, value: T) -> Unit,
    label: (T) -> String,
) {
    require(visibleItems % 2 == 1) { "visibleItems must be odd so a row sits dead centre" }

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    val itemHeightPx = with(density) { itemHeight.toPx() }
    val totalHeight = itemHeight * visibleItems
    val sidePadCount = visibleItems / 2

    val startIndex = remember(values.size, initialIndex) {
        initialIndex.coerceIn(0, max(0, values.size - 1))
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    // Selected index = the row whose centre is closest to the column's
    // centre. We use the list's offset/itemHeight to determine whether
    // the first-visible row has scrolled past its halfway mark; if so,
    // the row below it is the centred one.
    val selectedIndex by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val candidate = first + if (offset > itemHeightPx / 2f) 1 else 0
            candidate.coerceIn(0, max(0, values.size - 1))
        }
    }

    LaunchedEffect(values, haptics) {
        snapshotFlow { selectedIndex }
            .drop(1)
            .distinctUntilChanged()
            .collect { idx ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (idx in values.indices) onSelected(idx, values[idx])
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
        contentAlignment = Alignment.Center,
    ) {
        // No selection pill — iOS-style wheel: the centre row reads as
        // "selected" purely through its larger/bolder type and the fact
        // that adjacent rows fade and shrink away. A boxed background
        // behind the centre row looks disconnected (user feedback).

        // The scrolling column. Wrapped in an offscreen compositing
        // layer so we can apply a DstIn alpha mask that softly fades the
        // rows at the top and bottom edges.
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyColumn(
                state = listState,
                flingBehavior = fling,
                contentPadding = PaddingValues(vertical = itemHeight * sidePadCount),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val mask = Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.18f to Color.Black.copy(alpha = 0.35f),
                            0.34f to Color.Black.copy(alpha = 0.85f),
                            0.50f to Color.Black,
                            0.66f to Color.Black.copy(alpha = 0.85f),
                            0.82f to Color.Black.copy(alpha = 0.35f),
                            1.00f to Color.Transparent,
                        )
                        drawRect(brush = mask, blendMode = BlendMode.DstIn)
                    },
            ) {
                items(count = values.size, key = { it }) { idx ->
                    WheelRow(
                        text = label(values[idx]),
                        height = itemHeight,
                        isSelected = idx == selectedIndex,
                        textStyle = textStyle,
                        selectedTextStyle = selectedTextStyle,
                        listState = listState,
                        index = idx,
                        itemHeightPx = itemHeightPx,
                        visibleItems = visibleItems,
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelRow(
    text: String,
    height: Dp,
    isSelected: Boolean,
    textStyle: TextStyle,
    selectedTextStyle: TextStyle,
    listState: LazyListState,
    index: Int,
    itemHeightPx: Float,
    visibleItems: Int,
) {
    // Normalised distance from the column centre. Reads happen inside a
    // derivedStateOf so we only recompose on scroll position changes,
    // not on every frame.
    val distanceFromCenter by remember(index, itemHeightPx) {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
            if (item == null) {
                Float.POSITIVE_INFINITY
            } else {
                val itemCenter = item.offset + item.size / 2f
                abs(itemCenter - viewportCenter) / itemHeightPx
            }
        }
    }

    val half = (visibleItems / 2f).coerceAtLeast(1f)
    val t = (distanceFromCenter / half).coerceIn(0f, 1f)

    // Quadratic falloff: centre stays crisp, edges drop off without a
    // hard transition. Combined with the DstIn mask above we get the
    // "smoothly fades out at the borders" feel.
    val alpha = (1f - t * t).coerceIn(0f, 1f)
    val scale = lerp(1.0f, 0.78f, t)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (isSelected) selectedTextStyle else textStyle,
            color = Wellness.colors.text,
        )
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)
