package com.wellness.app.ui.components

import android.os.Build
import android.view.RoundedCorner
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Slide-in / dismiss spring — copied 1:1 from the reference zhuzha
// implementation. Slightly under-damped + high stiffness gives a brisk,
// "immersive" feel: the overlay eases in, settles without a sharp stop,
// and pops back from a swipe-cancel like it has weight.
private val OverlaySpring = spring<Float>(
    dampingRatio = 0.92f,
    stiffness = 500f,
)

// Snappier spring for the rollback-from-cancel direction. Higher
// stiffness so the overlay returns to rest quickly when the user changes
// their mind mid-gesture.
private val OverlayCancelSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 600f,
)

private val FallbackDeviceCornerRadius: Dp = 32.dp

// Width of the swipe-back "grab" zone measured from the left edge of the overlay.
private val SwipeBackEdgeWidth: Dp = 120.dp

// How far the host content parallax-translates to the LEFT when an overlay is
// fully on top of it (fraction of the screen width). Matches zhuzha.
private const val HostParallaxFraction = 0.28f

// Release thresholds. Slightly more forgiving than the original 30%/800dps
// because the previous implementation read a stale dismissProgress.value
// at release time — see notes around fingerProgress in the gesture loop.
// With synchronous tracking the threshold can reflect intent more directly,
// so commit on a 22% pull or a moderate flick (550dp/s).
private const val SwipeBackCommitFraction = 0.22f
private val SwipeBackCommitVelocityDpPerSec: Dp = 550.dp

/**
 * Renders the host content with an iOS-style parallax that follows the
 * progress of any overlay currently on top of it. The host stays in
 * composition while an overlay slides — there is no screenshot/snapshot hack.
 */
@Composable
fun OverlayHost(
    parallaxProgress: MutableFloatState,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = parallaxProgress.floatValue
                translationX = -(1f - p) * size.width * HostParallaxFraction
            },
    ) {
        content()
    }
}

/**
 * Animated, gesture-aware overlay that hosts an arbitrary [content] on top of
 * the rest of the app. Slides in from the right with rounded device-matching
 * corners during motion, follows the finger on a left-edge swipe-back, and
 * plays a smooth dismiss when [content]'s back affordance fires.
 *
 * A single dismissProgress in [0..1] controls everything:
 *   * 0f — overlay at rest, edge-to-edge, no clipping
 *   * 1f — overlay fully off the right edge, corners fully rounded
 *
 * Structurally identical to the reference zhuzha implementation. The only
 * deviations are NonCancellable wraps around suspend cleanup so that a
 * mid-animation cancellation (predictive back release, recomposition, etc.)
 * can't leave the overlay in a half-dismissed state or crash the suspend
 * point inside a cancelled context.
 */
@Composable
fun RoundedSlideOverlay(
    parallaxProgress: MutableFloatState,
    onDismissed: () -> Unit,
    content: @Composable (animatedBack: () -> Unit) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val cornerRadiusDp = remember(view) { resolveDeviceCornerRadius(view, density) }
    val cornerRadiusPx = with(density) { cornerRadiusDp.toPx() }
    val edgeWidthPx = with(density) { SwipeBackEdgeWidth.toPx() }
    val velocityCommitPx = with(density) { SwipeBackCommitVelocityDpPerSec.toPx() }

    val dismissProgress = remember { Animatable(1f) }
    val currentOnDismissed by rememberUpdatedState(onDismissed)

    // Mirror dismissProgress into the host's parallax so the layer behind us
    // translates in lockstep on the same frame.
    LaunchedEffect(dismissProgress, parallaxProgress) {
        snapshotFlow { dismissProgress.value }.collect { v ->
            parallaxProgress.floatValue = v
        }
    }

    // Slide in on first composition. No frame-waiting or preload tricks —
    // they make the slide feel "delayed" because the user already tapped.
    // Heavy first-frame work (SVG decodes, font shaping) is paid up-front
    // at app startup via the icon prewarm in MainActivity, so the
    // destination is already in cache by the time it composes here.
    LaunchedEffect(Unit) {
        dismissProgress.animateTo(targetValue = 0f, animationSpec = OverlaySpring)
    }

    val animatedBack: () -> Unit = remember(scope) {
        {
            scope.launch {
                try {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    dismissProgress.animateTo(targetValue = 1f, animationSpec = OverlaySpring)
                } finally {
                    withContext(NonCancellable) { currentOnDismissed() }
                }
            }
        }
    }

    PredictiveBackHandler(enabled = true) { progressFlow ->
        try {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            progressFlow.collect { event ->
                if (event.progress < 0.999f) {
                    dismissProgress.snapTo(event.progress)
                }
            }
            // Flow completed — user committed the gesture. Cleanup runs in
            // NonCancellable so a racing recomposition can't crash us mid-
            // dismiss with a CancellationException at the next suspend point.
            withContext(NonCancellable) {
                dismissProgress.animateTo(targetValue = 1f, animationSpec = OverlaySpring)
                currentOnDismissed()
            }
        } catch (_: CancellationException) {
            // User released early. We're inside a catch(CancellationException)
            // and the parent context can be in cancelling state — without
            // NonCancellable the next suspend call throws immediately and
            // the overlay is left stranded half-on-screen (manifests as a
            // crash / frozen UI on swipe-back).
            withContext(NonCancellable) {
                dismissProgress.animateTo(targetValue = 0f, animationSpec = OverlayCancelSpring)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Manual follow-finger swipe-back on the left edge — runs in parallel
            // to PredictiveBackHandler so we cover both gesture stacks.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x > edgeWidthPx) return@awaitEachGesture

                    var consumedRight = false
                    val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, dragAmount ->
                        if (dragAmount > 0) {
                            consumedRight = true
                            change.consume()
                        }
                    } ?: return@awaitEachGesture
                    if (!consumedRight) return@awaitEachGesture

                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    velocityTracker.addPosition(drag.uptimeMillis, drag.position)

                    val width = size.width.toFloat()
                    val initialDx = (drag.position.x - down.position.x).coerceAtLeast(0f)
                    // awaitEachGesture { ... } is a restricted-suspension scope:
                    // Animatable.snapTo isn't on its dispatch surface, so we
                    // launch it on the composable scope. The launched snapTos
                    // are queued and apply asynchronously — DO NOT read
                    // dismissProgress.value from inside this gesture loop for
                    // commit/cancel decisions, it lags behind the finger.
                    // Track the latest finger progress synchronously here so
                    // the release threshold reflects where the finger
                    // actually was when the user lifted (otherwise short,
                    // quick swipes get cancelled because the queued snapTos
                    // hadn't yet caught up to the visible drag distance,
                    // making the overlay snap back even when the user pulled
                    // it well past the commit point — the exact "тянется,
                    // потом возвращается назад" symptom from the bug report).
                    var fingerProgress = (initialDx / width).coerceIn(0f, 1f)
                    scope.launch {
                        dismissProgress.snapTo(fingerProgress)
                    }

                    var released = false
                    while (!released) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            released = true
                        } else {
                            val dx = (change.position.x - down.position.x).coerceAtLeast(0f)
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            fingerProgress = (dx / width).coerceIn(0f, 1f)
                            scope.launch {
                                dismissProgress.snapTo(fingerProgress)
                            }
                        }
                    }

                    val velocity = velocityTracker.calculateVelocity().x
                    val shouldDismiss =
                        fingerProgress >= SwipeBackCommitFraction ||
                            velocity >= velocityCommitPx
                    if (shouldDismiss) {
                        scope.launch {
                            try {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                // Make sure the visual catches up to the
                                // finger before the dismiss animation
                                // starts — otherwise the spring snaps from
                                // a stale value (queued snapTos may not
                                // have flushed yet) and the overlay
                                // appears to jump backwards before sliding
                                // out.
                                dismissProgress.snapTo(fingerProgress)
                                dismissProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = OverlaySpring,
                                    initialVelocity = velocity / width,
                                )
                            } finally {
                                withContext(NonCancellable) { currentOnDismissed() }
                            }
                        }
                    } else {
                        scope.launch {
                            dismissProgress.snapTo(fingerProgress)
                            dismissProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = OverlayCancelSpring,
                                initialVelocity = velocity / width,
                            )
                        }
                    }
                }
            }
            .graphicsLayer {
                // Spring physics can briefly overshoot to negative values
                // when the user flings the overlay closed and the spring
                // settles back through zero — RoundedCornerShape rejects
                // negative corner sizes with an IllegalArgumentException,
                // so clamp `p` (and the resulting radius) to >= 0 before
                // feeding them into translation/shape.
                val p = dismissProgress.value.coerceAtLeast(0f)
                translationX = p * size.width
                // Left corners grow as the overlay moves out (matches device curve);
                // right corners stay 0 since the overlay is at-or-past the right edge.
                val leftRadiusPx = (p * cornerRadiusPx).coerceAtLeast(0f)
                shape = RoundedCornerShape(
                    topStart = leftRadiusPx,
                    topEnd = 0f,
                    bottomEnd = 0f,
                    bottomStart = leftRadiusPx,
                )
                clip = true
            },
    ) {
        content(animatedBack)
    }
}

/** Best-effort lookup of the device's physical display corner radius. */
private fun resolveDeviceCornerRadius(view: android.view.View, density: Density): Dp {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val insets = view.rootWindowInsets
        if (insets != null) {
            val positions = intArrayOf(
                RoundedCorner.POSITION_TOP_LEFT,
                RoundedCorner.POSITION_TOP_RIGHT,
                RoundedCorner.POSITION_BOTTOM_LEFT,
                RoundedCorner.POSITION_BOTTOM_RIGHT,
            )
            var maxPx = 0
            for (p in positions) {
                val r = insets.getRoundedCorner(p)
                if (r != null && r.radius > maxPx) maxPx = r.radius
            }
            if (maxPx > 0) {
                return with(density) { maxPx.toFloat().toDp() }
            }
        }
    }
    return FallbackDeviceCornerRadius
}

@Composable
fun rememberParallaxProgress(): MutableFloatState = remember { mutableFloatStateOf(1f) }
