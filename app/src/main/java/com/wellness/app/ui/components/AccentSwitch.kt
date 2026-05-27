package com.wellness.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.theme.Wellness

/**
 * Pill-shaped toggle in the spirit of Telegram / iOS settings. Track colour
 * animates between muted-grey (off) and the current accent (on); thumb glides
 * with a soft spring.
 */
@Composable
fun AccentSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width = 52.dp
    val height = 32.dp
    val thumbSize = 26.dp
    val padding = 3.dp
    val trackOff = Wellness.colors.text.copy(alpha = 0.18f)
    val trackOn = Wellness.colors.accent
    val trackColor by animateColorAsState(
        if (checked) trackOn else trackOff,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switchtrack",
    )
    val offsetX by animateDpAsState(
        if (checked) width - thumbSize - padding else padding,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "switchthumb",
    )
    Box(
        modifier
            .width(width)
            .height(height)
            .background(trackColor, RoundedCornerShape(999.dp))
            .noFeedbackClick { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = offsetX, y = padding)
                .size(thumbSize)
                .background(Color.White, RoundedCornerShape(999.dp)),
        )
    }
}
