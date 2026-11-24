package com.cfsm.monitor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cfsm.monitor.ui.theme.metricPalette
import com.cfsm.monitor.ui.theme.skin

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (title != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                trailing?.invoke()
            }
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

@Composable
fun MetricBar(
    label: String,
    value: String,
    percent: Float,
    color: Color,
    modifier: Modifier = Modifier,
    labelWidth: Dp = 32.dp,
    valueWidth: Dp = 92.dp,
    dimmed: Boolean = false,
) {
    val track = metricPalette.track
    val fill = if (dimmed) color.copy(alpha = 0.35f) else color
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(labelWidth),
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(track),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percent / 100f).coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(fill),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(valueWidth),
        )
    }
}

@Composable
fun MetricRing(
    percent: Float,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 86.dp,
    strokeWidth: Dp = 7.dp,
    dimmed: Boolean = false,
    compact: Boolean = false,
) {
    val track = metricPalette.track
    val clamped = (percent / 100f).coerceIn(0f, 1f)
    val ringColor = if (dimmed) color.copy(alpha = 0.35f) else color
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (clamped > 0.001f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = if (compact) {
                    MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
                },
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatusDot(color: Color, size: Dp = 10.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size / 2.2f)
                .background(color, CircleShape),
        )
    }
}

@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    background: Color = color.copy(alpha = 0.12f),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
fun BigNumber(
    value: String,
    unit: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = color,
            )
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 三栏指标列：顶部百分比大字 + 细进度条 + 底部容量小字。
 * 用于首页卡片的三栏网格布局（省空间核心改造）。
 */
@Composable
fun MetricColumn(
    label: String,
    percentText: String,
    percent: Float,
    color: Color,
    modifier: Modifier = Modifier,
    capacityText: String = "",
    extraText: String = "",
    warn: Boolean = false,
    dimmed: Boolean = false,
) {
    val sk = skin
    val fill = when {
        dimmed -> color.copy(alpha = 0.35f)
        warn -> metricPalette.warn
        else -> color
    }
    Column(modifier = modifier) {
        Text(
            text = percentText,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            ),
            color = when {
                dimmed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                warn -> metricPalette.warn
                else -> MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = sk.meta,
        )
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(metricPalette.track),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percent / 100f).coerceIn(0.02f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(fill),
            )
        }
        if (capacityText.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = capacityText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                ),
                color = sk.footer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (extraText.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = extraText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                ),
                color = sk.meta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 迷你趋势折线（Canvas 自绘，无第三方库），赛博终端风格使用。 */
@Composable
fun Sparkline(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    if (values.size < 2) return
    val line = if (dimmed) color.copy(alpha = 0.35f) else color
    Canvas(modifier = modifier.fillMaxWidth().height(18.dp)) {
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val step = size.width / (values.size - 1)
        var previous: Offset? = null
        values.forEachIndexed { index, v ->
            val x = index * step
            val y = (size.height - 3f) - ((v - min) / range) * (size.height - 6f)
            val current = Offset(x, y)
            previous?.let { p ->
                drawLine(
                    color = line,
                    start = p,
                    end = current,
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            previous = current
        }
    }
}

/**
 * 三网延迟徽章：淡色底纹胶囊，按延迟自动着色
 * （<50ms 绿 · 50~150ms 蓝 · >150ms 橙，超时红）。
 */
@Composable
fun LatencyBadge(
    label: String,
    latency: com.cfsm.monitor.data.Latency?,
    modifier: Modifier = Modifier,
) {
    val palette = metricPalette
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    val valueText: String
    val valueColor: Color
    val bgColor: Color
    when {
        latency == null -> {
            valueText = "—"
            valueColor = neutral
            bgColor = neutral.copy(alpha = 0.08f)
        }
        latency.timeout -> {
            valueText = "超时"
            valueColor = palette.bad
            bgColor = palette.bad.copy(alpha = 0.14f)
        }
        latency.isBad -> {
            valueText = "${latency.ms.toInt()}"
            valueColor = palette.bad
            bgColor = palette.bad.copy(alpha = 0.14f)
        }
        else -> {
            val ms = latency.ms
            val c = when {
                ms < 50f -> palette.ok
                ms <= 150f -> palette.latency
                else -> palette.warn
            }
            valueText = "${ms.toInt()}"
            valueColor = c
            bgColor = c.copy(alpha = 0.12f)
        }
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = neutral,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
            ),
            color = valueColor,
        )
    }
}
