package com.wellness.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.theme.Wellness

/**
 * Scaffolding for a full-screen overlay screen (Edit Profile, Goals, Add* …).
 *
 * Layout (top → bottom):
 *   1. Pinned header — back arrow + title. Stays glued to the top edge of
 *      the screen while content scrolls underneath it. Opaque page bg so
 *      scrolling content never bleeds through.
 *   2. Scrollable content column. Sits BELOW the pinned header (top padding
 *      reserves the header height) and ABOVE the floating primary button
 *      (bottom padding reserves the button area). `imePadding()` is applied
 *      to the scroll container so the scroll viewport shrinks above the
 *      keyboard when it opens — this is what allows BasicTextField's
 *      built-in bring-into-view to actually scroll a focused field into the
 *      visible portion instead of leaving it hidden behind the IME or the
 *      floating button.
 *   3. Floating primary action button — anchored to the bottom edge with
 *      `imePadding()` + `navigationBarsPadding()` so it rises with the
 *      keyboard and clears the gesture nav.
 *
 * Elastic overscroll is kept on the scroll column only — the pinned header
 * and the floating button stay perfectly still.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OverlayScreen(
    title: String,
    onBack: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val elastic = rememberElasticOverscroll()
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Wellness.colors.bg)
                .nestedScroll(elastic.connection),
        ) {
            // ── 1. Scrollable content ────────────────────────────────────
            //   - statusBars padding so content starts below the cutout
            //   - top padding == header height (54dp) so the first content
            //     row clears the pinned header
            //   - imePadding so the focused TextField can be scrolled into
            //     view above the keyboard
            //   - bottom padding leaves room for the floating button
            //     (button ~58dp + 18dp top/bottom margins ≈ 96dp; +24dp
            //     breathing room) AND the bottom system navigation bar
            //     handled by the button itself.
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = HEADER_HEIGHT_DP)
                    .imePadding()
                    .verticalScroll(scroll)
                    .graphicsLayer { translationY = elastic.verticalOverscroll.floatValue }
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                content()
                // Reserve space so the last input row can scroll above the
                // floating button (and above the IME when it's open).
                Spacer(modifier = Modifier.height(120.dp))
            }

            // ── 2. Pinned header ────────────────────────────────────────
            //   Transparent — content visibly slides UNDER the title/back
            //   arrow (per design). `windowInsetsPadding(statusBars)`
            //   keeps it under the notch / status icons.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HEADER_HEIGHT_DP)
                        .padding(top = 4.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NoFeedbackButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            SolarIcon(
                                name = "alt-arrow-left-outline",
                                tint = Wellness.colors.text,
                                size = 28.dp,
                            )
                        }
                    }
                    Box(Modifier.width(4.dp))
                    Text(
                        text = title,
                        color = Wellness.colors.text,
                        style = Wellness.typography.headlineMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── 3. Floating primary action button ───────────────────────
            //   imePadding lifts it above the keyboard; navigationBarsPadding
            //   keeps it above the gesture pill when keyboard is closed.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                PrimaryActionButton(
                    label = primaryLabel,
                    enabled = primaryEnabled,
                    onClick = onPrimary,
                )
            }
        }
    }
}

/**
 * Height of the pinned header bar (excluding the status-bar inset). Has to
 * match the bottom-of-back-button position so the first scrollable row
 * doesn't tuck under it.
 */
private val HEADER_HEIGHT_DP = 54.dp
