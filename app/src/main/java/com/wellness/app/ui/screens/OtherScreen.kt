package com.wellness.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wellness.app.ui.components.SettingsCard
import com.wellness.app.ui.components.SettingsHeader
import com.wellness.app.ui.components.SettingsRow
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.theme.Wellness
import com.wellness.app.ui.theme.WellnessColors

/**
 * Profile → Другое sub-screen. Hosts the diagnostic / power-user entries
 * that don't belong on the main Profile list (currently just Логи).
 * Telegram-style: a single SettingsCard with one or more rows.
 */
@Composable
fun OtherScreen(
    onBack: () -> Unit,
    onLogs: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 6.dp),
        ) {
            SettingsHeader(title = "Другое", onBack = onBack)
            Box(Modifier.height(6.dp))
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "notebook-outline",
                    iconTile = WellnessColors.TileBlue,
                    title = "Логи",
                    onClick = onLogs,
                )
            }
        }
    }
}
