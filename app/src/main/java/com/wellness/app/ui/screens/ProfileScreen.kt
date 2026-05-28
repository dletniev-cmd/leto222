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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wellness.app.ui.components.GoalProgressBar
import com.wellness.app.ui.components.NoFeedbackButton
import com.wellness.app.ui.components.ScreenScaffold
import com.wellness.app.ui.components.SettingsCard
import com.wellness.app.ui.components.SettingsRow
import com.wellness.app.ui.components.SettingsRowDivider
import com.wellness.app.ui.components.noFeedbackClick
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.state.LocalAppState
import com.wellness.app.ui.state.calculateGoalProgress
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors

/**
 * Profile screen. Recently rebuilt around the user-approved HTML
 * prototype — the old four-up "Вес / Цель / Вода / Ккал" stat row was
 * replaced with a thick weighted-goal progress bar plus three rounded
 * quick-action buttons. The settings stack underneath stays untouched.
 */
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit = {},
    onGoals: () -> Unit = {},
    onAppearance: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onBindings: () -> Unit = {},
    onTiwi: () -> Unit = {},
    onOther: () -> Unit = {},
    onProgressDetail: () -> Unit = {},
    onQuickScan: () -> Unit = {},
    onQuickWeight: () -> Unit = {},
    onQuickAchievements: () -> Unit = {},
) {
    val state = LocalAppState.current
    val breakdown = calculateGoalProgress(state)
    val percent = (breakdown.overall * 100f).toInt()

    ScreenScaffold(topPadding = 0.dp) {
        // Pencil edit button anchored top-right with no surrounding plate.
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

        // Avatar. Falls back to the accent-tinted user glyph when no
        // Telegram photo is bound — the fallback is painted first so the
        // empty slot never shows through.
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

        // Name + subtitle. `userName` is the single source of truth shared
        // between Telegram bind, profile editor and this header.
        Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                state.userName,
                color = Wellness.colors.text,
                style = Wellness.typography.headlineLarge,
            )
        }
        Box(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 14.dp), contentAlignment = Alignment.Center) {
            Text(
                "${state.age} лет · ${state.gender.title}",
                color = Wellness.colors.muted,
                style = Wellness.typography.bodyMedium,
            )
        }

        // ── Goal progress block ──────────────────────────────────────────
        //
        // Matches HTML prototype 1:1 — bare bar on background, then a
        // single centred caption "Цель достигнута на N%" (percent in
        // text/bold, prefix in muted). Тap anywhere in the block opens
        // the detail screen — the chevron/«подробнее» row was removed
        // per user request because the bar itself is the hit target.
        Box(
            Modifier
                .fillMaxWidth()
                .screenHPad()
                .noFeedbackClick(onClick = onProgressDetail),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GoalProgressBar(progress = breakdown.overall, modifier = Modifier.fillMaxWidth())
                Box(Modifier.height(10.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Wellness.colors.muted)) {
                            append("Цель достигнута на ")
                        }
                        withStyle(
                            SpanStyle(
                                color = Wellness.colors.text,
                                fontWeight = FontWeight.Bold,
                            ),
                        ) {
                            append("$percent%")
                        }
                    },
                    style = Wellness.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // ── 3 quick action buttons ───────────────────────────────────────
        //
        // Spacing matches the prototype: 16dp top / 18dp bottom around
        // the row, 10dp gap between tiles, 62dp tile height, 22dp radius.
        Row(
            Modifier
                .fillMaxWidth()
                .screenHPad()
                .padding(top = 16.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = "scanner-outline",
                label = "Сканер",
                onClick = onQuickScan,
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = "scale-outline",
                label = "Вес",
                onClick = onQuickWeight,
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = "medal-star-outline",
                label = "Награды",
                onClick = onQuickAchievements,
            )
        }

        // ── Settings list ────────────────────────────────────────────────
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

/**
 * Single rounded quick-action tile. Centred icon over a small label —
 * matches the proto. Buttons are visually unified (same height /
 * radius / background) so the row reads as a connected control strip
 * rather than three disparate chips.
 */
@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(62.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Wellness.colors.container, RoundedCornerShape(22.dp))
            .noFeedbackClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SolarIcon(name = icon, tint = Wellness.colors.accent, size = 24.dp)
            Box(Modifier.height(4.dp))
            Text(
                label,
                color = Wellness.colors.muted,
                style = Wellness.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
