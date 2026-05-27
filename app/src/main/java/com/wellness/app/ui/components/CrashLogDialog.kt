package com.wellness.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wellness.app.CrashReporter
import com.wellness.app.ui.theme.Wellness

/**
 * One-shot prompt shown on app launch if the previous session terminated
 * with an unhandled exception. The captured stack trace is rendered in a
 * scrollable panel together with two actions:
 *
 *   - **Скопировать** — writes the full crash text into the system
 *     clipboard, confirms with a Toast, and clears the on-disk report.
 *   - **Закрыть** — clears the on-disk report without copying.
 *
 * The dialog is purely advisory — it never blocks app startup or any
 * existing UI work, and the side-effect that reads the crash file is
 * keyed on `Unit` so it only fires once per process.
 */
@Composable
fun CrashLogDialog() {
    val context = LocalContext.current
    var crashLog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        crashLog = CrashReporter.read(context)
    }

    val log = crashLog ?: return
    Dialog(
        onDismissRequest = {
            CrashReporter.consume(context)
            crashLog = null
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(Wellness.colors.container, RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            Column {
                Text(
                    "Прошлый запуск завершился ошибкой",
                    color = Wellness.colors.text,
                    style = Wellness.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Скопируй логи и пришли — починю.",
                    color = Wellness.colors.muted,
                    style = Wellness.typography.bodyMedium,
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 220.dp)
                        .background(Wellness.colors.bg, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    val scroll = rememberScrollState()
                    Text(
                        log,
                        color = Wellness.colors.text,
                        style = Wellness.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroll),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DialogTextButton(
                        text = "Закрыть",
                        onClick = {
                            CrashReporter.consume(context)
                            crashLog = null
                        },
                        accent = false,
                    )
                    Spacer(Modifier.width(10.dp))
                    DialogTextButton(
                        text = "Скопировать",
                        onClick = {
                            copyToClipboard(context, log)
                            Toast.makeText(
                                context,
                                "Логи скопированы",
                                Toast.LENGTH_SHORT,
                            ).show()
                            CrashReporter.consume(context)
                            crashLog = null
                        },
                        accent = true,
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Wellness crash log", text))
}

@Composable
private fun DialogTextButton(text: String, onClick: () -> Unit, accent: Boolean) {
    Box(
        Modifier
            .height(40.dp)
            .background(
                if (accent) Wellness.colors.accent else Wellness.colors.bg,
                RoundedCornerShape(12.dp),
            )
            .noFeedbackClick(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (accent) androidx.compose.ui.graphics.Color.White else Wellness.colors.text,
            style = Wellness.typography.labelMedium,
        )
    }
}
