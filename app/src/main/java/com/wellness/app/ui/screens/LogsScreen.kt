package com.wellness.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wellness.app.CrashReporter
import com.wellness.app.ui.components.SettingsHeader
import com.wellness.app.ui.components.noFeedbackClick
import com.wellness.app.ui.components.screenHPad
import com.wellness.app.ui.icons.SolarIcon
import com.wellness.app.ui.theme.Wellness

/**
 * "Logs" screen — surfaces the most recent captured crash report and lets
 * the user copy it to the clipboard. Replaces the launch-time blocking
 * dialog: the dialog interrupted every launch after a crash, so the
 * report is now passive and discoverable from Profile → Другое → Логи.
 *
 * If no report is on disk we render a friendly empty state instead of
 * an empty card.
 */
@Composable
fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var log by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        log = CrashReporter.read(context)
    }

    Box(Modifier.fillMaxSize().background(Wellness.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 6.dp, bottom = 0.dp),
        ) {
            SettingsHeader(title = "Логи", onBack = onBack)

            val current = log
            if (current.isNullOrBlank()) {
                Spacer(Modifier.height(36.dp))
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .background(
                                    Wellness.colors.accentSoft,
                                    RoundedCornerShape(999.dp),
                                )
                                .padding(20.dp),
                        ) {
                            SolarIcon(
                                name = "check-circle-bold-duotone",
                                tint = Wellness.colors.accent,
                                size = 44.dp,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Логов нет",
                            color = Wellness.colors.text,
                            style = Wellness.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Приложение работает чисто.",
                            color = Wellness.colors.muted,
                            style = Wellness.typography.bodyMedium,
                        )
                    }
                }
            } else {
                // Header chip — quick summary tag above the body
                Box(
                    Modifier
                        .screenHPad()
                        .padding(top = 12.dp, bottom = 10.dp),
                ) {
                    Text(
                        "Последний сбой",
                        color = Wellness.colors.muted,
                        style = Wellness.typography.labelSmall,
                    )
                }
                // Body — scrollable monospaced-ish text inside a card
                Box(
                    Modifier
                        .screenHPad()
                        .fillMaxWidth()
                        .background(Wellness.colors.container, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    val scroll = rememberScrollState()
                    Text(
                        current,
                        color = Wellness.colors.text,
                        style = Wellness.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 460.dp)
                            .verticalScroll(scroll),
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier.screenHPad().fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(Wellness.colors.accent, RoundedCornerShape(14.dp))
                            .noFeedbackClick {
                                copyToClipboard(context, current)
                                Toast.makeText(
                                    context,
                                    "Скопировано",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SolarIcon(
                                name = "copy-bold-duotone",
                                tint = Color.White,
                                size = 18.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Скопировать",
                                color = Color.White,
                                style = Wellness.typography.labelMedium,
                            )
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(Wellness.colors.container, RoundedCornerShape(14.dp))
                            .noFeedbackClick {
                                CrashReporter.consume(context)
                                log = null
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Очистить",
                            color = Wellness.colors.text,
                            style = Wellness.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Wellness crash log", text))
}
