package com.example.workouttracker.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChartDataPoint(
    val date: Long,
    val estimatedOneRepMax: Double,
    val workingWeight: Double
)

@Composable
fun DualAxisProgressChart(
    dataPoints: List<ChartDataPoint>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет данных для отображения",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth()) {
        ChartLegend(primaryColor = primaryColor, tertiaryColor = tertiaryColor)

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(start = 48.dp, end = 48.dp, bottom = 32.dp, top = 8.dp)
        ) {
            val sorted = dataPoints.sortedBy { it.date }
            val chartWidth = size.width
            val chartHeight = size.height

            if (sorted.size == 1) {
                val point = sorted[0]
                val cx = chartWidth / 2f
                val cy = chartHeight / 2f
                drawCircle(primaryColor, radius = 6.dp.toPx(), center = Offset(cx, cy - 10))
                drawCircle(tertiaryColor, radius = 6.dp.toPx(), center = Offset(cx, cy + 10))
                return@Canvas
            }

            val maxOneRM = sorted.maxOf { it.estimatedOneRepMax }.coerceAtLeast(1.0)
            val minOneRM = sorted.minOf { it.estimatedOneRepMax }.coerceAtLeast(0.0)
            val maxWorkW = sorted.maxOf { it.workingWeight }.coerceAtLeast(1.0)
            val minWorkW = sorted.minOf { it.workingWeight }.coerceAtLeast(0.0)

            val oneRMRange = (maxOneRM - minOneRM).coerceAtLeast(1.0)
            val workWRange = (maxWorkW - minWorkW).coerceAtLeast(1.0)
            val paddingFactor = 0.1
            val oneRMBottom = minOneRM - oneRMRange * paddingFactor
            val oneRMTop = maxOneRM + oneRMRange * paddingFactor
            val workWBottom = minWorkW - workWRange * paddingFactor
            val workWTop = maxWorkW + workWRange * paddingFactor

            val minDate = sorted.first().date.toFloat()
            val maxDate = sorted.last().date.toFloat()
            val dateRange = (maxDate - minDate).coerceAtLeast(1f)

            // Grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = chartHeight * i / gridLines
                drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), strokeWidth = 1f)
            }

            // 1RM line (primary)
            drawDataLine(
                sorted = sorted,
                valueSelector = { it.estimatedOneRepMax },
                minDate = minDate,
                dateRange = dateRange,
                valueBottom = oneRMBottom,
                valueTop = oneRMTop,
                chartWidth = chartWidth,
                chartHeight = chartHeight,
                color = primaryColor,
                dotRadius = 4.dp.toPx()
            )

            // Working weight line (tertiary)
            drawDataLine(
                sorted = sorted,
                valueSelector = { it.workingWeight },
                minDate = minDate,
                dateRange = dateRange,
                valueBottom = workWBottom,
                valueTop = workWTop,
                chartWidth = chartWidth,
                chartHeight = chartHeight,
                color = tertiaryColor,
                dotRadius = 4.dp.toPx()
            )

            // Y-axis labels (left = 1RM, right = working weight)
            val labelPaint = android.graphics.Paint().apply {
                textSize = with(density) { 10.dp.toPx() }
                color = textColor.hashCode()
                isAntiAlias = true
            }
            for (i in 0..gridLines) {
                val y = chartHeight * i / gridLines
                val oneRMVal = oneRMTop - (oneRMTop - oneRMBottom) * i / gridLines
                val workWVal = workWTop - (workWTop - workWBottom) * i / gridLines

                drawContext.canvas.nativeCanvas.drawText(
                    "%.0f".format(oneRMVal),
                    -44.dp.toPx(),
                    y + 4.dp.toPx(),
                    labelPaint
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "%.0f".format(workWVal),
                    chartWidth + 4.dp.toPx(),
                    y + 4.dp.toPx(),
                    labelPaint
                )
            }

            // X-axis date labels
            val dateFormatter = SimpleDateFormat("dd.MM", Locale("ru"))
            val labelCount = (sorted.size).coerceAtMost(5)
            val step = (sorted.size - 1).coerceAtLeast(1) / labelCount.coerceAtLeast(1)
            for (i in sorted.indices step step.coerceAtLeast(1)) {
                val x = (sorted[i].date.toFloat() - minDate) / dateRange * chartWidth
                val label = dateFormatter.format(Date(sorted[i].date))
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x - 12.dp.toPx(),
                    chartHeight + 16.dp.toPx(),
                    labelPaint
                )
            }
        }
    }
}

private fun DrawScope.drawDataLine(
    sorted: List<ChartDataPoint>,
    valueSelector: (ChartDataPoint) -> Double,
    minDate: Float,
    dateRange: Float,
    valueBottom: Double,
    valueTop: Double,
    chartWidth: Float,
    chartHeight: Float,
    color: Color,
    dotRadius: Float
) {
    val valueRange = (valueTop - valueBottom).coerceAtLeast(1.0)
    val points = sorted.map { point ->
        val x = (point.date.toFloat() - minDate) / dateRange * chartWidth
        val y = chartHeight - ((valueSelector(point) - valueBottom) / valueRange * chartHeight).toFloat()
        Offset(x, y)
    }

    // Draw line
    if (points.size >= 2) {
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        drawPath(path, color, style = Stroke(width = 2.5f))
    }

    // Draw dots
    for (point in points) {
        drawCircle(color, radius = dotRadius, center = point)
    }
}

@Composable
private fun ChartLegend(primaryColor: Color, tertiaryColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(primaryColor)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "1RM (кг)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(16.dp))
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(tertiaryColor)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Рабочий вес (кг)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
