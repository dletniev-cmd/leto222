package com.wellness.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.theme.Wellness

/** Single progress ring with rounded cap. */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 138.dp,
    strokeWidth: Dp = 14.dp,
    @Suppress("UNUSED_PARAMETER") trackColor: Color? = null,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "ringp",
    )
    Canvas(modifier.size(size)) {
        val sw = strokeWidth.toPx()
        val inset = sw / 2f
        val arcSize = Size(this.size.width - sw, this.size.height - sw)
        val topLeft = Offset(inset, inset)
        // Soft tinted disc fill behind the ring — gives the habit ring a
        // gentle colour wash so the icon sits in a coloured object instead
        // of a grey donut. No grey track ring — we want a single rounded
        // progress arc on top of the filled disc, nothing more.
        drawCircle(
            color = color.copy(alpha = 0.18f),
            radius = (this.size.width - sw) / 2f,
            center = Offset(this.size.width / 2f, this.size.height / 2f),
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )
    }
}

/** Stacked multi-segment ring (used for macros). */
@Composable
fun StackedRing(
    segments: List<Pair<Color, Float>>, // color to length [0..1]
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 16.dp,
) {
    // Theme-aware track so the StackedRing reads correctly on both light and
    // dark backgrounds (used to be a hard-coded translucent white that was
    // invisible against the light theme).
    val track = Wellness.colors.track
    Canvas(modifier.size(size)) {
        val sw = strokeWidth.toPx()
        val inset = sw / 2f
        val arcSize = Size(this.size.width - sw, this.size.height - sw)
        val topLeft = Offset(inset, inset)
        // Track
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw),
        )
        var startAngle = -90f
        segments.forEach { (color, len) ->
            val sweep = 360f * len.coerceIn(0f, 1f)
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
            startAngle += sweep
        }
    }
}
