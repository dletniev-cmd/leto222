package com.wellness.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wellness.app.ui.components.NoFeedbackButton
import com.wellness.app.ui.components.ScreenScaffold
import com.wellness.app.ui.components.SettingsCard
import com.wellness.app.ui.components.SettingsRow
import com.wellness.app.ui.components.SettingsRowDivider
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit = {},
    onGoals: () -> Unit = {},
    onAppearance: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onBindings: () -> Unit = {},
    onTiwi: () -> Unit = {},
    onOther: () -> Unit = {},
) {
    val state = LocalAppState.current
    ScreenScaffold(topPadding = 0.dp) {
        // Pencil edit button anchored top-right with no surrounding plate —
        // matches Telegram's minimal corner button.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 6.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            NoFeedbackButton(onClick = onEditProfile, modifier = Modifier.size(44.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SolarIcon(name = "pen-outline", tint = Wellness.colors.text, size = 24.dp)
                }
            }
        }

        // Avatar. When a Telegram binding is in place we render the user's
        // Telegram profile photo via Coil, falling back to the generic
        // accent-tinted user glyph when the photo URL hasn't resolved yet
        // (or the user has no profile photo). The fallback always paints
        // first so the empty avatar slot never shows through during the
        // network round-trip.
        val photoUrl = state.telegramUser?.photoUrl
        val context = LocalContext.current
        Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(Wellness.colors.accentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SolarIcon(name = "user-outline", tint = Wellness.colors.accent, size = 56.dp)
                if (photoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        // Name + subtitle. We render `state.userName` directly — Telegram
        // binding writes the TG display name into `userName` once on bind
        // (see [AppState.bindTelegram]), and the profile editor mutates the
        // same field. Reading a single source of truth keeps the header and
        // the editor in sync regardless of whether the user edited the name
        // manually after binding.
        val displayName = state.userName
        Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                displayName,
                color = Wellness.colors.text,
                style = Wellness.typography.headlineLarge,
            )
        }
        Box(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                "${state.age} лет · ${state.gender.title}",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodyMedium,
            )
        }

        // One unified stats container with thin vertical dividers between
        // the four metrics. The previous four-card pill layout looked busy
        // because of the coloured icon tiles; the bug report explicitly
        // asked for "единый контейнер но с разделителями".
        Row(
            Modifier
                .fillMaxWidth()
                .screenHPad()
                .padding(bottom = 14.dp)
                .background(Wellness.colors.container, RoundedCornerShape(22.dp))
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickStat(
                modifier = Modifier.weight(1f),
                label = "Вес",
                value = "%.1f".format(state.weight).replace('.', ','),
                unit = "кг",
            )
            StatDivider()
            QuickStat(
                modifier = Modifier.weight(1f),
                label = "Цель",
                value = "%.1f".format(state.weightGoal).replace('.', ','),
                unit = "кг",
            )
            StatDivider()
            QuickStat(
                modifier = Modifier.weight(1f),
                label = "Вода",
                value = formatCompact(state.waterTarget),
                unit = "мл",
            )
            StatDivider()
            QuickStat(
                modifier = Modifier.weight(1f),
                label = "Ккал",
                value = formatCompact(state.kcalTarget),
                unit = "",
            )
        }

        // Settings container
        SettingsCard(
            modifier = Modifier.screenHPad(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            SettingsRow(
                icon = "target-outline",
                iconTile = WellnessColors.TileGreen,
                title = "Цели",
                onClick = onGoals,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = "moon-stars-outline",
                iconTile = WellnessColors.TileViolet,
                title = "Оформление",
                onClick = onAppearance,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = "bell-outline",
                iconTile = WellnessColors.TileRed,
                title = "Уведомления",
                onClick = onNotifications,
            )
            SettingsRowDivider()
            // The bindings row is the entry point to the new Telegram-link
            // flow. We surface the live binding state in the row's trailing
            // value so users get an at-a-glance "Привязано/Не
            // привязано" without opening the screen.
            SettingsRow(
                icon = "link-round-outline",
                iconTile = WellnessColors.TelegramBlue,
                title = "Привязки",
                value = state.telegramUser?.let { "Привязан" } ?: "Не привязан",
                onClick = onBindings,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = "smile-circle-outline",
                iconTile = WellnessColors.TilePink,
                title = "Тифи",
                onClick = onTiwi,
            )
        }

        // "Другое" is a single row that opens a sub-screen — keeps the
        // main settings list short while hiding diagnostic items
        // (currently just Логи) one tap deeper, like iOS Settings →
        // General → Other.
        Box(Modifier.height(18.dp))
        SettingsCard(
            modifier = Modifier.screenHPad(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            SettingsRow(
                icon = "menu-dots-outline",
                iconTile = WellnessColors.TileBlue,
                title = "Другое",
                onClick = onOther,
            )
        }
    }
}

/** Single metric inside the unified profile stats container. Value sits on
 *  top in title weight, label underneath in muted body. The optional unit
 *  is appended to the value with a thin space so the metric reads as one
 *  glyph cluster (e.g. "78,4 кг"). */
@Composable
private fun QuickStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
) {
    Column(
        modifier = modifier.padding(vertical = 2.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                color = Wellness.colors.text,
                style = Wellness.typography.headlineLarge,
                maxLines = 1,
            )
            if (unit.isNotEmpty()) {
                Text(
                    " $unit",
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        Box(Modifier.height(2.dp))
        Text(
            label,
            color = Wellness.colors.muted,
            style = Wellness.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Thin vertical hairline used between [QuickStat] columns. Inset top and
 *  bottom so it doesn't touch the container edges. */
@Composable
private fun StatDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(36.dp)
            .background(Wellness.colors.text.copy(alpha = 0.08f)),
    )
}

/** Short numeric formatter — keeps four-digit values intact, drops trailing
 *  zeros from .0 decimals. */
private fun formatCompact(value: Int): String {
    if (value >= 10000) return (value / 1000).toString() + "к"
    return value.toString()
}
