package com.wellness.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.theme.Wellness

data class SegItem(val key: String, val title: String, val icon: String? = null)

@Composable
fun SegmentedTabs(
    items: List<SegItem>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val itemWidthDp = with(density) { (size.width / items.size).toDp() }
    val selectedIndex = items.indexOfFirst { it.key == selected }.coerceAtLeast(0)
    val offsetX by animateDpAsState(
        targetValue = itemWidthDp * selectedIndex,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "segx",
    )
    Box(
        modifier
            .fillMaxWidth()
            .background(Wellness.colors.container, RoundedCornerShape(999.dp))
            .padding(5.dp)
            .onSizeChanged { size = it }
    ) {
        if (size != IntSize.Zero) {
            Box(
                Modifier
                    .offset(x = offsetX, y = 0.dp)
                    .width(itemWidthDp)
                    .height(44.dp)
                    .background(Wellness.colors.accentSoft, RoundedCornerShape(999.dp))
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEach { item ->
                val active = item.key == selected
                NoFeedbackButton(
                    onClick = { onSelect(item.key) },
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        if (item.icon != null) {
                            SolarIcon(
                                name = item.icon,
                                tint = if (active) Wellness.colors.accent else Wellness.colors.muted,
                                size = 18.dp,
                            )
                            Box(Modifier.size(8.dp))
                        }
                        Text(
                            item.title,
                            color = if (active) Wellness.colors.accent else Wellness.colors.muted,
                            style = Wellness.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

