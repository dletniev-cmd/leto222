package com.wellness.app.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default horizontal page padding. Sections opt in via [Modifier.screenHPad].
 * The scaffold itself has NO horizontal padding so horizontal scrollers and
 * "bleed" content can extend past the screen edges and back.
 */
val ScreenHorizontalPadding: Dp = 18.dp

val LocalScreenHPad = staticCompositionLocalOf { ScreenHorizontalPadding }

fun Modifier.screenHPad(pad: Dp = ScreenHorizontalPadding): Modifier =
    this.padding(horizontal = pad)

@Composable
fun ScreenScaffold(
    horizontalPadding: Dp = ScreenHorizontalPadding,
    scrollState: ScrollState = rememberScrollState(),
    topPadding: Dp = 6.dp,
    pinnedHeader: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    // Measured height of the pinned header, so the scrolling content can
    // reserve exactly that much room at the top: at rest the first item
    // sits right below the title (nothing clipped), and on scroll the
    // content slides UP and passes *under* the (transparent, no-plate)
    // header which stays nailed in place.
    var headerHeight by remember { mutableStateOf(0.dp) }

    CompositionLocalProvider(LocalScreenHPad provides horizontalPadding) {
        Box(Modifier.fillMaxSize()) {
            // ElasticOverscroll replaces the system stretch with a damped
            // iOS-style translation: scrolls slide a touch further past the
            // edge and ping back — nothing gets visually deformed.
            ElasticOverscroll(modifier = Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = topPadding, bottom = 160.dp)
                ) {
                    if (pinnedHeader != null) {
                        Spacer(Modifier.height(headerHeight))
                    }
                    content()
                }
            }
            // Pinned header: same top inset + padding as the scrolling
            // column so it lines up pixel-for-pixel with where the header
            // used to live, but it never scrolls. No background — the
            // content is fully visible scrolling beneath it.
            if (pinnedHeader != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = topPadding)
                        .onSizeChanged {
                            headerHeight = with(density) { it.height.toDp() }
                        }
                ) {
                    pinnedHeader()
                }
            }
        }
    }
}
