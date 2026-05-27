package com.wellness.app.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
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
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalScreenHPad provides horizontalPadding) {
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
                content()
            }
        }
    }
}
