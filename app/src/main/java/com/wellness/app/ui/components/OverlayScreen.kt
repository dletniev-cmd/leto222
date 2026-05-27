package com.wellness.app.ui.components

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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
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
 * Scaffolding for a full-screen overlay screen.
 *
 * Layout matches the other settings/inner screens (e.g. AppearanceScreen):
 * the back/title header is the FIRST row inside the scrollable column and
 * scrolls away with the content — no pinned gradient plate on top. The only
 * floating element is the primary action button at the bottom, which still
 * follows the IME so the keyboard doesn't cover it.
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
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = elastic.verticalOverscroll.floatValue }
                    .verticalScroll(scroll)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Header is part of the scroll content — it slides up with
                // the page like on every other inner screen. No background,
                // no gradient — fully transparent. The only horizontal
                // padding comes from the parent column.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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

                content()

                // Extra space at the bottom so the floating action button
                // doesn't overlap the last input row.
                Spacer(modifier = Modifier.height(120.dp))
            }

            // Primary action — the button itself. No backing container plate,
            // no surrounding card. imePadding makes it follow the keyboard.
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
