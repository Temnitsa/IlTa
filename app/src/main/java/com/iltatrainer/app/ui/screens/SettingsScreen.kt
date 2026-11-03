package com.iltatrainer.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.iltatrainer.app.BuildConfig
import com.iltatrainer.app.core.ThemeMode
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.ProgressStore
import com.iltatrainer.app.data.UpdateState
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.MonogramBadge
import com.iltatrainer.app.ui.components.SectionHeader
import com.iltatrainer.app.ui.pluralRu
import com.iltatrainer.app.ui.questionsLabel
import java.time.LocalDate

private val SESSION_SIZES = listOf(5, 10, 15, 20)

@Composable
fun SettingsScreen(
    progress: ProgressState,
    store: ProgressStore,
    questionCount: Int,
    ruleCount: Int,
    updateState: UpdateState,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings = progress.settings

    val showToast: (String) -> Unit = { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val exportFileName = remember { "ilta-progress-${LocalDate.now()}.json" }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            val ok = store.exportTo(uri).isSuccess
            showToast(if (ok) "Прогресс экспортирован" else "Не удалось экспортировать прогресс")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            store.importFrom(uri)
                .onSuccess { showToast("Прогресс импортирован") }
                .onFailure { showToast("Не удалось импортировать прогресс") }
        }
    }

    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))

        SectionHeader("Оформление")
        AppCard {
            ChipRow {
                SettingChip("Системная", settings.themeMode == ThemeMode.SYSTEM) {
                    store.updateSettings { it.copy(theme = ThemeMode.SYSTEM.code) }
                }
                SettingChip("Светлая", settings.themeMode == ThemeMode.LIGHT) {
                    store.updateSettings { it.copy(theme = ThemeMode.LIGHT.code) }
                }
                SettingChip("Тёмная", settings.themeMode == ThemeMode.DARK) {
                    store.updateSettings { it.copy(theme = ThemeMode.DARK.code) }
                }
            }
        }

        SectionHeader("Тренировка")
        AppCard {
            Text(
                text = "Вопросов в одной сессии",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ChipRow {
                SESSION_SIZES.forEach { size ->
                    SettingChip(size.toString(), settings.sessionSize == size) {
                        store.updateSettings { it.copy(sessionSize = size) }
                    }
                }
            }
        }

        SectionHeader("Данные")
        AppCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { exportLauncher.launch(exportFileName) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                ) {
                    Text("Экспорт")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                ) {
                    Text("Импорт")
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                Text("Сбросить прогресс")
            }
        }

        SectionHeader("Обновления")
        AppCard {
            val statusText = when (updateState) {
                UpdateState.Idle -> "Проверка обновлений выполняется при запуске и вручную."
                UpdateState.Checking -> "Проверяем обновления…"
                UpdateState.UpToDate -> "У вас последняя версия."
                is UpdateState.Available -> "Доступна версия ${updateState.info.versionName}"
                is UpdateState.Downloading -> "Скачивание…"
                is UpdateState.Installing -> "Открываем установщик…"
                is UpdateState.Failed -> updateState.message
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (updateState is UpdateState.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(10.dp))
            when (val state = updateState) {
                is UpdateState.Available -> {
                    Button(
                        onClick = onDownloadUpdate,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                    ) {
                        Text("Скачать и установить")
                    }
                }
                is UpdateState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${state.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    OutlinedButton(
                        onClick = onCheckUpdates,
                        enabled = updateState !is UpdateState.Checking,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                    ) {
                        Text("Проверить обновления")
                    }
                }
            }
        }

        SectionHeader("О приложении")
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonogramBadge(text = "IlTa", size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("IlTa", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Версия ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${questionsLabel(questionCount)} · " +
                    "$ruleCount ${pluralRu(ruleCount, "тема", "темы", "тем")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Работает офлайн. Прогресс хранится на этом устройстве.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(28.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Сбросить прогресс?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    text = "Будут удалены все ответы и статистика. Настройки сохранятся.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.resetAll()
                        showResetDialog = false
                        showToast("Прогресс сброшен")
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена")
                }
            },
        )
    }
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun SettingChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
