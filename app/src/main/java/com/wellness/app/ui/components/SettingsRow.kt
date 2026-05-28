package com.wellness.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.theme.Wellness

/**
 * Telegram-style row used inside the settings container. Each row owns a
 * vivid coloured rounded square with a white icon centred inside, a label
 * and an optional trailing value / chevron.
 */
@Composable
fun SettingsRow(
    icon: String,
    iconTile: Color,
    title: String,
    iconTint: Color = Color.White,
    value: String? = null,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.noFeedbackClick(onClick = onClick) else it }
        .padding(horizontal = 12.dp, vertical = 8.dp)
    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(34.dp)
                .background(iconTile, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = icon, tint = iconTint, size = 21.dp)
        }
        Box(Modifier.width(14.dp))
        Text(
            title,
            color = Wellness.colors.text,
            style = Wellness.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
            if (showChevron) Box(Modifier.width(8.dp))
        } else if (value != null) {
            Text(value, color = Wellness.colors.muted, style = Wellness.typography.bodyMedium)
            if (showChevron) Box(Modifier.width(8.dp))
        }
        if (showChevron) {
            SolarIcon(
                name = "alt-arrow-right-outline",
                tint = Wellness.colors.muted,
                size = 20.dp,
            )
        }
    }
}

/**
 * Thin 1px-ish divider sitting under settings rows, indented to align with
 * the row text (i.e. starts after the icon tile).
 */
@Composable
fun SettingsRowDivider(insetStart: Dp = 60.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = insetStart, end = 12.dp)
            .height(0.7.dp)
            .background(Wellness.colors.text.copy(alpha = 0.05f)),
    )
}

/** Card container shared by all settings groups (Telegram-style ~22dp radius). */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .padding(contentPadding),
    ) {
        Column { content() }
    }
}

/**
 * Standard sticky-style header used on every settings screen — a thin outline
 * chevron tap-target on the left and the page title next to it. No filled
 * background plate around the arrow so it reads as a proper Telegram-style
 * back button rather than a chunky chip.
 */
@Composable
fun SettingsHeader(
    title: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 6.dp),
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
            title,
            color = Wellness.colors.text,
            style = Wellness.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

/**
 * Compact accent checkmark button suitable for the top-right slot of
 * a [SettingsHeader] — replaces a heavier pinned bottom CTA. Rendered
 * as a soft accent pill with a bold check glyph; disabled state fades
 * to muted so the user can still see the affordance.
 */
@Composable
fun HeaderCheckButton(enabled: Boolean = true, onClick: () -> Unit) {
    val bg = if (enabled) Wellness.colors.accent else Wellness.colors.muted.copy(alpha = 0.18f)
    val tint = if (enabled) Color.White else Wellness.colors.muted
    NoFeedbackButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(bg, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = "check-bold", tint = tint, size = 22.dp)
        }
    }
}
