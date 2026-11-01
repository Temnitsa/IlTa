package com.iltatrainer.app.ui.screens

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iltatrainer.app.BuildConfig
import com.iltatrainer.app.core.AppLanguage
import com.iltatrainer.app.core.ThemeMode
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.ProgressStore
import com.iltatrainer.app.data.UpdateState
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.SectionHeader
import com.iltatrainer.app.ui.pluralRu
import com.iltatrainer.app.ui.questionsLabel
import com.iltatrainer.app.ui.tr
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

    val exportDoneMessage = tr("Прогресс экспортирован", "Progress exported")
    val exportFailedMessage = tr("Не удалось экспортировать прогресс", "Couldn't export progress")
    val importDoneMessage = tr("Прогресс импортирован", "Progress imported")
    val importFailedMessage = tr("Не удалось импортировать прогресс", "Couldn't import progress")
    val resetDoneMessage = tr("Прогресс сброшен", "Progress reset")

    val questionsText = questionsLabel(questionCount)
    val countsLine = tr(
        "$questionsText · $ruleCount ${pluralRu(ruleCount, "тема", "темы", "тем")}",
        "$questionsText · $ruleCount ${if (ruleCount == 1) "topic" else "topics"}",
    )

    val exportFileName = remember { "ilta-progress-${LocalDate.now()}.json" }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            val result = store.exportTo(uri)
            showToast(if (result.isSuccess) exportDoneMessage else exportFailedMessage)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            store.importFrom(uri)
                .onSuccess { showToast(importDoneMessage) }
                .onFailure { showToast(importFailedMessage) }
        }
    }

    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = tr("Настройки", "Settings"),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = tr(
                    "Язык, оформление, тренировки и данные",
                    "Language, appearance, practice and data",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionHeader(tr("Язык", "Language"))
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = tr("Язык интерфейса и пояснений", "Language of the interface and explanations"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChoiceChips(
                    options = listOf(
                        AppLanguage.RU to tr("Русский", "Russian"),
                        AppLanguage.EN to tr("Английский", "English"),
                    ),
                    selected = settings.appLanguage,
                    onSelect = { language ->
                        store.updateSettings { it.copy(language = language.code) }
                    },
                )
            }
        }

        SectionHeader(tr("Тема оформления", "Theme"))
        AppCard {
            ChoiceChips(
                options = listOf(
                    ThemeMode.SYSTEM to tr("Системная", "System"),
                    ThemeMode.LIGHT to tr("Светлая", "Light"),
                    ThemeMode.DARK to tr("Тёмная", "Dark"),
                ),
                selected = settings.themeMode,
                onSelect = { theme ->
                    store.updateSettings { it.copy(theme = theme.code) }
                },
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SectionHeader(tr("Динамические цвета", "Dynamic colors"))
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = tr("Цвета Material You", "Material You colors"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = tr(
                                "Оттенки интерфейса подстраиваются под обои устройства.",
                                "Interface colors adapt to your wallpaper.",
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.dynamicColor,
                        onCheckedChange = { enabled ->
                            store.updateSettings { it.copy(dynamicColor = enabled) }
                        },
                    )
                }
            }
        }

        SectionHeader(tr("Длина сессии", "Session length"))
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = tr("Сколько вопросов в одной тренировке", "Questions per practice session"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChoiceChips(
                    options = SESSION_SIZES.map { it to it.toString() },
                    selected = settings.sessionSize,
                    onSelect = { size ->
                        store.updateSettings { it.copy(sessionSize = size) }
                    },
                )
            }
        }

        SectionHeader(tr("Данные", "Data"))
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = tr("Экспорт прогресса", "Export progress"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = tr(
                        "Сохраните резервную копию прогресса и настроек в файл JSON.",
                        "Save a backup copy of your progress and settings to a JSON file.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { exportLauncher.launch(exportFileName) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(tr("Сохранить в файл", "Save to file"))
                }

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))

                Text(
                    text = tr("Импорт прогресса", "Import progress"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = tr(
                        "Восстановите прогресс из сохранённого файла. Текущий прогресс будет заменён.",
                        "Restore progress from a saved file. Current progress will be replaced.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(tr("Выбрать файл", "Choose file"))
                }

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))

                Text(
                    text = tr("Сброс прогресса", "Reset progress"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = tr(
                        "Удаляет ответы, статистику, избранное и серию. Настройки сохраняются.",
                        "Deletes answers, statistics, favorites and your streak. Settings are kept.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(tr("Сбросить прогресс", "Reset progress"))
                }
            }
        }

        SectionHeader(tr("Обновления", "Updates"))
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (updateState !is UpdateState.Available) {
                    Text(
                        text = tr("Версия ${BuildConfig.VERSION_NAME}", "Version ${BuildConfig.VERSION_NAME}"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                val statusText = when (updateState) {
                    UpdateState.Idle -> tr(
                        "Проверка выполняется при запуске приложения и вручную.",
                        "Updates are checked on launch and manually.",
                    )
                    UpdateState.Checking -> tr("Проверяем обновления…", "Checking for updates…")
                    UpdateState.UpToDate -> tr("У вас последняя версия.", "You have the latest version.")
                    is UpdateState.Available -> tr(
                        "Доступна версия ${updateState.info.versionName}",
                        "Version ${updateState.info.versionName} is available",
                    )
                    is UpdateState.Downloading -> tr("Скачивание…", "Downloading…")
                    is UpdateState.Installing -> tr("Открываем установщик…", "Opening the installer…")
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
                when (val state = updateState) {
                    is UpdateState.Available -> {
                        Button(
                            onClick = onDownloadUpdate,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Text(tr("Скачать и установить", "Download and install"))
                        }
                    }
                    is UpdateState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "${state.progress}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> Unit
                }
                if (updateState !is UpdateState.Available) {
                    OutlinedButton(
                        onClick = onCheckUpdates,
                        enabled = updateState !is UpdateState.Checking &&
                            updateState !is UpdateState.Downloading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(tr("Проверить обновления", "Check for updates"))
                    }
                }
            }
        }

        SectionHeader(tr("О приложении", "About"))
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📘",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "IlTa",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = tr(
                                "Версия ${BuildConfig.VERSION_NAME}",
                                "Version ${BuildConfig.VERSION_NAME}",
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = countsLine,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = tr(
                        "Основные функции работают офлайн. Интернет нужен только для проверки обновлений; прогресс хранится на этом устройстве.",
                        "Core features work offline. The internet is only needed for update checks; progress is stored on this device.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(tr("Сбросить прогресс?", "Reset progress?")) },
            text = {
                Text(
                    tr(
                        "Будут удалены все ответы, статистика, избранное и серия. Язык, тема оформления и другие настройки сохранятся.",
                        "All answers, statistics, favorites and your streak will be deleted. Language, theme and other settings will be kept.",
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.resetAll()
                        showResetDialog = false
                        showToast(resetDoneMessage)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(tr("Сбросить", "Reset"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(tr("Отмена", "Cancel"))
                }
            },
        )
    }
}

@Composable
private fun <T> ChoiceChips(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}
