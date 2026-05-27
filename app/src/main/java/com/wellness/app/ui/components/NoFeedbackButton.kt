package com.wellness.app.ui.components

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/** Clickable that produces zero visual feedback: no ripple, no scale, no color change. */
@Composable
fun Modifier.noFeedbackClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
    )
}

@Composable
fun NoFeedbackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.noFeedbackClick(enabled, onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        content()
    }
}
