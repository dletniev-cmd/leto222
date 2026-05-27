package com.wellness.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.theme.Wellness

@Composable
fun Chip(
    text: String,
    active: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val bg = if (active) Wellness.colors.accentSoft else Wellness.colors.track
    val color = if (active) Wellness.colors.accent else Wellness.colors.text
    NoFeedbackButton(onClick = onClick, modifier = modifier) {
        Row(
            Modifier
                .background(bg, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, color = color, style = Wellness.typography.labelMedium)
        }
    }
}
