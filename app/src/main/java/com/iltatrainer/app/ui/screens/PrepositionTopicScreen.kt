package com.iltatrainer.app.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.round
import androidx.compose.ui.window.Dialog
import com.iltatrainer.app.data.PrepositionEntry
import com.iltatrainer.app.data.PrepositionTopic
import com.iltatrainer.app.data.ProgressState
import com.iltatrainer.app.data.Question
import com.iltatrainer.app.data.Rule
import com.iltatrainer.app.core.AppLanguage
import com.iltatrainer.app.domain.StatsCalculator
import com.iltatrainer.app.ui.components.AccuracyBar
import com.iltatrainer.app.ui.components.AppCard
import com.iltatrainer.app.ui.components.BackTopBar
import com.iltatrainer.app.ui.components.EmptyState
import com.iltatrainer.app.ui.components.LevelChip
import com.iltatrainer.app.ui.components.PrepositionArt
import com.iltatrainer.app.ui.components.SectionHeader
import com.iltatrainer.app.ui.tr

/**
 * Visual dictionary of prepositions: adaptive picture tiles (blue cube + red sphere artwork).
 * Tapping a tile opens a dialog with translation, explanation and English examples.
 */
@Composable
fun PrepositionTopicScreen(
    rule: Rule?,
    topic: PrepositionTopic?,
    questions: List<Question>,
    progress: ProgressState,
    onBack: () -> Unit,
    onOpenTheory: () -> Unit,
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
            contentPadding = PaddingValues(16.dp),
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
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onOpenTheory,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(tr("Правила", "Rules"))
                            }
                            Button(
                                onClick = onTrain,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(tr("Тренировать", "Train"))
                            }
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
    AppCard(onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                if (drawable != null) {
                    Image(
                        painter = painterResource(drawable),
                        contentDescription = entry.label,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Text(
                text = entry.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
            Text(
                text = entry.translation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PrepositionDialog(entry: PrepositionEntry, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val drawable = PrepositionArt.drawableFor(entry.key)
                if (drawable != null) {
                    Image(
                        painter = painterResource(drawable),
                        contentDescription = entry.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = entry.translation,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                if (entry.short.isNotBlank()) {
                    Text(
                        text = entry.short,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(text = entry.explanation, style = MaterialTheme.typography.bodyMedium)
                if (entry.examples.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tr("Примеры", "Examples"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    entry.examples.forEach { example ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = example.en,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = example.ru,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(tr("Понятно", "Got it"))
                }
            }
        }
    }
}
