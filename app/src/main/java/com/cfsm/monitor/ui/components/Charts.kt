package com.cfsm.monitor.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cfsm.monitor.data.Latency
import com.cfsm.monitor.ui.formatClock
import com.cfsm.monitor.ui.theme.metricPalette
import java.util.Locale

data class ChartSeries(
    val name: String,
    val color: Color,
    val values: List<Float?>,
    val fill: Boolean = true,
)

@Composable
fun LineChart(
    series: List<ChartSeries>,
    timestamps: List<Long>,
    modifier: Modifier = Modifier,
    height: Dp = 130.dp,
    fixedMax: Float? = null,
    formatY: (Float) -> String = { value ->
        if (value >= 1000f) (value / 1000f).toInt().toString() + "k" else value.toInt().toString()
    },
    xLabelFormatter: (Long) -> String = { ts -> formatClock(ts) },
) {
    val pointCount = series.maxOfOrNull { it.values.size } ?: 0
    val flat = series.flatMap { it.values }.filterNotNull()
    val hasData = pointCount > 1 && flat.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        if (series.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                series.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(item.color, CircleShape)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        if (!hasData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        val maxValue = fixedMax ?: ((flat.maxOrNull() ?: 1f) * 1.15f).coerceAtLeast(1f)
        val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        val paint = remember { Paint().apply { isAntiAlias = true } }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val labelWidth = 40.dp.toPx()
            val bottomPad = 16.dp.toPx()
            val topPad = 6.dp.toPx()
            val chartWidth = (size.width - labelWidth).coerceAtLeast(1f)
            val chartHeight = (size.height - bottomPad - topPad).coerceAtLeast(1f)

            paint.textSize = 9.sp.toPx()
            paint.color = labelColor.toArgb()

            val steps = 3
            for (i in 0..steps) {
                val ratio = i.toFloat() / steps
                val y = topPad + chartHeight * ratio
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1f,
                )
                val labelValue = maxValue * (1f - ratio)
                paint.textAlign = Paint.Align.LEFT
                drawContext.canvas.nativeCanvas.drawText(
                    formatY(labelValue),
                    chartWidth + 6.dp.toPx(),
                    y + 3.dp.toPx(),
                    paint,
                )
            }

            series.forEach { item ->
                drawSeries(
                    values = item.values,
                    color = item.color,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight,
                    topPad = topPad,
                    maxValue = maxValue,
                    filled = item.fill,
                )
            }

            paint.textAlign = Paint.Align.LEFT
            val firstLabel = timestamps.firstOrNull()?.let { xLabelFormatter(it) }.orEmpty()
            val lastLabel = timestamps.lastOrNull()?.let { xLabelFormatter(it) }.orEmpty()
            val midLabel = timestamps.getOrNull(timestamps.size / 2)?.let { xLabelFormatter(it) }.orEmpty()
            if (firstLabel.isNotEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(
                    firstLabel, 0f, size.height - 2.dp.toPx(), paint
                )
            }
            if (midLabel.isNotEmpty()) {
                paint.textAlign = Paint.Align.CENTER
                drawContext.canvas.nativeCanvas.drawText(
                    midLabel, chartWidth / 2f, size.height - 2.dp.toPx(), paint
                )
            }
            if (lastLabel.isNotEmpty()) {
                paint.textAlign = Paint.Align.RIGHT
                drawContext.canvas.nativeCanvas.drawText(
                    lastLabel, chartWidth, size.height - 2.dp.toPx(), paint
                )
            }
        }
    }
}

private fun DrawScope.drawSeries(
    values: List<Float?>,
    color: Color,
    chartWidth: Float,
    chartHeight: Float,
    topPad: Float,
    maxValue: Float,
    filled: Boolean,
) {
    val count = values.size
    if (count < 2) return

    fun xAt(index: Int): Float = chartWidth * index.toFloat() / (count - 1).toFloat()
    fun yAt(value: Float): Float =
        topPad + chartHeight - (value / maxValue).coerceIn(0f, 1f) * chartHeight

    val line = Path()
    val area = Path()
    var started = false
    var lastX = 0f
    var lastY = 0f

    values.forEachIndexed { index, raw ->
        if (raw == null) {
            started = false
            return@forEachIndexed
        }
        val x = xAt(index)
        val y = yAt(raw)
        if (started) {
            line.lineTo(x, y)
        } else {
            // 遇到断点（null）后重新起笔
            line.moveTo(x, y)
            started = true
        }
        lastX = x
        lastY = y
    }

    if (filled && values.none { it == null }) {
        val firstX = xAt(0)
        val firstY = yAt(values[0] ?: 0f)
        area.moveTo(firstX, topPad + chartHeight)
        area.lineTo(firstX, firstY)
        for (index in 1 until count) {
            val value = values[index] ?: continue
            area.lineTo(xAt(index), yAt(value))
        }
        area.lineTo(lastX, topPad + chartHeight)
        area.close()
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f)),
                startY = topPad,
                endY = topPad + chartHeight,
            ),
        )
    }

    drawPath(
        path = line,
        color = color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    if (values.last() != null) {
        drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(lastX, lastY))
    }
}

@Composable
fun LatencyCell(
    label: String,
    latency: Latency?,
    loss: Double?,
    modifier: Modifier = Modifier,
) {
    val palette = metricPalette
    val valueColor = when {
        latency == null -> MaterialTheme.colorScheme.onSurfaceVariant
        latency.timeout -> palette.bad
        latency.isBad -> palette.bad
        latency.isWarn -> palette.warn
        else -> palette.ok
    }
    val valueText = when {
        latency == null -> "—"
        latency.timeout -> "超时"
        else -> "${latency.ms.toInt()}ms"
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
            ),
            color = valueColor,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        if (loss != null && loss >= 0.5) {
            Text(
                text = "丢包 " + String.format(Locale.US, "%.0f%%", loss),
                style = MaterialTheme.typography.labelSmall,
                color = if (loss >= 5) palette.bad else palette.warn,
                maxLines = 1,
            )
        }
    }
}
