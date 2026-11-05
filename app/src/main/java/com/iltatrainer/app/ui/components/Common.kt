package com.iltatrainer.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iltatrainer.app.R
import com.iltatrainer.app.domain.RadarMetric
import com.iltatrainer.app.ui.tr

/** Flat card: white surface, 1dp subtle border, 14dp radius, no shadow. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = border,
        ) {
            Column(modifier = Modifier.padding(14.dp), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = border,
        ) {
            Column(modifier = Modifier.padding(14.dp), content = content)
        }
    }
}

/** Small section label above a group of flat cards/rows. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

/** Compact neutral level chip (A1/A2/B1/B2) — no per-level colours. */
@Composable
fun LevelChip(level: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = level.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}


/** Minimal line icon for a grammar section (articles / prepositions / tenses). */
@Composable
fun SectionIcon(sectionId: String, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val icon = when (sectionId) {
        "articles" -> R.drawable.ic_section_articles
        "prepositions" -> R.drawable.ic_section_prepositions
        "tenses" -> R.drawable.ic_section_tenses
        else -> R.drawable.ic_rule_form
    }
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.56f),
            )
        }
    }
}
/** Neutral monogram badge used instead of emoji icons. */
@Composable
fun MonogramBadge(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text.trim().take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Compact metric tile; tappable when [onClick] is provided. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = border,
        ) {
            StatTileContent(label = label, value = value)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = border,
        ) {
            StatTileContent(label = label, value = value)
        }
    }
}

@Composable
private fun StatTileContent(label: String, value: String) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
    }
}

/** Thin flat progress bar. */
@Composable
fun AccuracyBar(accuracy: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    LinearProgressIndicator(
        progress = { accuracy.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    strokeWidth: Dp = 8.dp,
    label: String? = null,
    caption: String? = null,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (label != null) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Empty state without decorative emoji. */
@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("Назад", "Back"),
                )
            }
        },
        actions = actions,
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

/** Three thin bars: custom stats icon so the app does not need material-icons-extended. */
@Composable
fun StatsBarIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.size(24.dp)) {
        val barWidth = size.width / 6f
        val gap = barWidth
        val heights = listOf(0.45f, 0.7f, 1f)
        heights.forEachIndexed { index, fraction ->
            val barHeight = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = index * (barWidth + gap) + gap * 0.5f,
                    y = size.height - barHeight,
                ),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
            )
        }
    }
}

/**
 * English sentence with a highlighted answer.
 * [filled] == null → renders the blank as an underline gap; otherwise the answer is inserted in primary color.
 */
@Composable
fun SentenceText(
    sentence: String,
    filled: String?,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val annotated: AnnotatedString = remember(sentence, filled, accent) {
        buildAnnotatedString {
            val parts = sentence.split("___")
            if (parts.size == 1) {
                append(sentence)
                return@buildAnnotatedString
            }
            if (filled != null && filled.trim() in setOf("", "—")) {
                append(sentence.replace("___", "").replace(Regex(" {2,}"), " ").trim())
                return@buildAnnotatedString
            }
            val insertion = if (filled == null) "________" else filled.trim()
            val capitalize = filled != null && parts[0].isBlank()
            parts.forEachIndexed { index, part ->
                append(part)
                if (index < parts.lastIndex) {
                    val value = if (capitalize) {
                        insertion.replaceFirstChar { it.uppercase() }
                    } else {
                        insertion
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = accent)) {
                        append(value)
                    }
                }
            }
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 26.sp),
        modifier = modifier,
    )
}


/** Minimal radar chart for the hidden knowledge-profile metrics. */
@Composable
fun RadarChart(metrics: List<RadarMetric>, modifier: Modifier = Modifier) {
    if (metrics.size < 3) {
        Box(modifier)
        return
    }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    val dataColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val labelSize = 11.sp

    Canvas(modifier = modifier) {
        val count = metrics.size
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 38.dp.toPx()
        val levels = 4

        fun point(index: Int, distance: Float): Offset {
            val angle = (-Math.PI / 2.0) + (2.0 * Math.PI * index / count)
            return Offset(
                x = center.x + (kotlin.math.cos(angle) * distance).toFloat(),
                y = center.y + (kotlin.math.sin(angle) * distance).toFloat(),
            )
        }

        for (level in 1..levels) {
            val r = radius * level / levels
            val path = Path().apply {
                for (i in 0 until count) {
                    val p = point(i, r)
                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(path, color = gridColor, style = Stroke(width = 1.dp.toPx()))
        }

        for (i in 0 until count) {
            drawLine(
                color = axisColor,
                start = center,
                end = point(i, radius),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val data = Path()
        metrics.forEachIndexed { index, metric ->
            val p = point(index, radius * metric.value.coerceIn(0f, 1f))
            if (index == 0) data.moveTo(p.x, p.y) else data.lineTo(p.x, p.y)
        }
        data.close()
        drawPath(data, color = dataColor.copy(alpha = 0.16f))
        drawPath(data, color = dataColor, style = Stroke(width = 2.dp.toPx()))
        metrics.forEachIndexed { index, metric ->
            if (metric.value > 0.02f) {
                drawCircle(
                    color = dataColor,
                    radius = 3.dp.toPx(),
                    center = point(index, radius * metric.value.coerceIn(0f, 1f)),
                )
            }
        }

        val paint = android.graphics.Paint().apply {
            color = labelColor
            textSize = labelSize.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        metrics.forEachIndexed { index, metric ->
            val p = point(index, radius + 24.dp.toPx())
            val label = "${metric.label} ${(metric.value.coerceIn(0f, 1f) * 100).toInt()}%"
            drawContext.canvas.nativeCanvas.drawText(label, p.x, p.y + paint.textSize / 3f, paint)
        }
    }
}
