package com.zive.moneymanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One wedge of a [DonutChart]. */
data class DonutSlice(
    val label: String,
    val value: Long,
    val color: Color,
)

/**
 * Proportional breakdown with a free centre for the headline total. Drawn with
 * Canvas rather than a charting library — the shape is simple and this keeps the
 * APK free of a dependency that would only be used twice.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerPrimary: String,
    centerSecondary: String,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 46f,
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1L)
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val progress by animateFloatAsState(
        targetValue = if (slices.isEmpty()) 0f else 1f,
        animationSpec = tween(600),
        label = "donut",
    )

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth.dp / 6)) {
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)

            if (slices.isEmpty()) {
                drawArc(
                    color = emptyColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                return@Canvas
            }

            // A one-degree gap keeps adjacent wedges of similar colour readable.
            val gap = if (slices.size > 1) 1.2f else 0f
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = (slice.value.toFloat() / total) * 360f * progress
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                startAngle += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerSecondary,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = centerPrimary,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

/** Legend rows paired with a [DonutChart]. */
@Composable
fun DonutLegend(
    slices: List<DonutSlice>,
    formatValue: (Long) -> String,
    modifier: Modifier = Modifier,
    onSliceClick: ((DonutSlice) -> Unit)? = null,
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1L)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        slices.forEach { slice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onSliceClick != null) Modifier.clickable { onSliceClick(slice) } else Modifier
                    )
                    .padding(vertical = 2.dp),
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(slice.color)
                )
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                Text(
                    text = "${(slice.value * 100f / total).let { "%.0f".format(it) }}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End,
                )
                Text(
                    text = formatValue(slice.value),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 10.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

/** One column of a [GroupedBarChart]: income and expense for a sub-period. */
data class BarEntry(
    val label: String,
    val income: Long,
    val expense: Long,
)

/**
 * Income and expense side by side across the sub-periods of the selected range,
 * so a month view shows weeks and a year view shows months.
 */
@Composable
fun GroupedBarChart(
    entries: List<BarEntry>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier,
    heightDp: Int = 160,
) {
    val maxValue = entries.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1L) ?: 1L
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(if (entries.size > 16) 1.dp else 6.dp),
        ) {
            entries.forEach { entry ->
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                    ) {
                        Bar(entry.income, maxValue, incomeColor, Modifier.weight(1f))
                        Bar(entry.expense, maxValue, expenseColor, Modifier.weight(1f))
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(gridColor)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(if (entries.size > 16) 1.dp else 6.dp),
        ) {
            entries.forEach { entry ->
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Bar(value: Long, maxValue: Long, color: Color, modifier: Modifier) {
    val fraction by animateFloatAsState(
        targetValue = (value.toFloat() / maxValue).coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "bar",
    )
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .fillMaxWidth()
                // A hairline keeps an empty column visible as a baseline tick.
                .fillMaxHeight(fraction.coerceAtLeast(0.004f))
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(color)
        )
    }
}
