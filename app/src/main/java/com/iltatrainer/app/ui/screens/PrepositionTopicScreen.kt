package com.iltatrainer.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iltatrainer.app.core.AppLanguage
import com.iltatrainer.app.data.PrepositionEntry
import com.iltatrainer.app.data.PrepositionTopic
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import com.iltatrainer.app.domain.StatsCalculator
import com.iltatrainer.app.ui.components.AccuracyBar
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.BackTopBar
import com.iltatrainer.app.ui.components.EmptyState
import com.iltatrainer.app.ui.components.LevelChip
import com.iltatrainer.app.ui.components.PrepositionArt
import com.iltatrainer.app.ui.components.SectionHeader
import com.iltatrainer.app.ui.tr
import kotlin.math.round

/**
 * Visual dictionary of prepositions: adaptive picture tiles (blue cube + red sphere artwork).
 * Tapping a tile opens a card with the word, its translation, an explanation and examples.
 */
@Composable
fun PrepositionTopicScreen(
    rule: Rule?,
    topic: PrepositionTopic?,
    questions: List<Question>,
    progress: ProgressState,
    onBack: () -> Unit,
    onTrain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember(topic?.ruleId) { mutableStateOf<PrepositionEntry?>(null) }
    val title = topic?.title?.ifBlank { null } ?: rule?.title?.text(AppLanguage.RU).orEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        BackTopBar(title = title, onBack = onBack)
        if (topic == null || topic.prepositions.isEmpty()) {
            EmptyState(
                title = tr("Словарь не загружен", "Dictionary is not loaded"),
                message = tr(
                    "Не удалось загрузить картинки предлогов. Попробуйте открыть тему ещё раз.",
                    "The preposition pictures could not be loaded. Try opening the topic again.",
                ),
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }

        val accuracy = rule?.let { StatsCalculator.ruleAccuracy(progress, it.id) }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topic.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val count = topic.prepositions.size
                                    Text(
                                        text = tr("$count предлогов", "$count prepositions"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = tr(
                                            "${questions.size} упражнений",
                                            "${questions.size} exercises",
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                AccuracyBar(accuracy = accuracy ?: 0f)
                                Text(
                                    text = if (accuracy == null) {
                                        tr("Ещё нет ответов", "No answers yet")
                                    } else {
                                        tr(
                                            "Точность ${round(accuracy * 100).toInt()}%",
                                            "Accuracy ${round(accuracy * 100).toInt()}%",
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.size(12.dp))
                            LevelChip(level = rule?.level ?: "A1")
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onTrain,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(tr("Тренировать", "Train"))
                        }
                    }
                    SectionHeader(
                        text = tr("Нажмите на картинку, чтобы узнать значение", "Tap a picture to see the meaning"),
                    )
                }
            }
            items(topic.prepositions, key = { it.key }) { entry ->
                PrepositionTile(entry = entry, onClick = { selected = entry })
            }
        }
    }

    selected?.let { entry ->
        PrepositionDialog(entry = entry, onDismiss = { selected = null })
    }
}

@Composable
private fun PrepositionTile(entry: PrepositionEntry, onClick: () -> Unit) {
    val drawable = PrepositionArt.drawableFor(entry.key)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(10f / 7f),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ) {
                if (drawable != null) {
                    Image(
                        painter = painterResource(drawable),
                        contentDescription = entry.label,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun PrepositionDialog(entry: PrepositionEntry, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                val drawable = PrepositionArt.drawableFor(entry.key)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(10f / 7f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    if (drawable != null) {
                        Image(
                            painter = painterResource(drawable),
                            contentDescription = entry.label,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = entry.translation,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                if (entry.short.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = entry.short,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = entry.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 23.sp,
                )

                if (entry.examples.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = tr("Примеры", "Examples"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    entry.examples.forEachIndexed { index, example ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${index + 1}. ${example.en}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 21.sp,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = example.ru,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(tr("Понятно", "Got it"))
                }
            }
        }
    }
}
