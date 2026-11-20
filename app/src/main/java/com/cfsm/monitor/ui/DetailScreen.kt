package com.cfsm.monitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cfsm.monitor.HistoryRange
import com.cfsm.monitor.MonitorViewModel
import com.cfsm.monitor.data.HistoryPoint
import com.cfsm.monitor.data.ServerInfo
import com.cfsm.monitor.ui.components.BigNumber
import com.cfsm.monitor.ui.components.ChartSeries
import com.cfsm.monitor.ui.components.KeyValueRow
import com.cfsm.monitor.ui.components.LatencyCell
import com.cfsm.monitor.ui.components.LineChart
import com.cfsm.monitor.ui.components.MetricRing
import com.cfsm.monitor.ui.components.Pill
import com.cfsm.monitor.ui.components.SectionCard
import com.cfsm.monitor.ui.components.StatusDot
import com.cfsm.monitor.ui.theme.metricPalette


@Composable
fun DetailScreen(vm: MonitorViewModel) {
    val server = vm.detailServer
    val now = vm.now
    val palette = metricPalette

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = vm::goHome) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server?.name?.ifBlank { "未命名" } ?: "服务器详情",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (server != null && server.region.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Pill(
                            text = regionLabel(server.region),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (server != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(
                            color = if (server.isOnlineAt(now)) palette.ok else MaterialTheme.colorScheme.outline,
                            size = 7.dp,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = (if (server.isOnlineAt(now)) "在线" else "离线") +
                                " · " + relativeTime(server.lastUpdated, now),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            IconButton(onClick = { vm.reloadHistory() }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "刷新",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (server == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "没有这台服务器的数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.navigationBarsPadding(),
            ) {
                item { HeroCard(server, now) }
                item { NetworkCard(server) }
                if (server.hasDiskIo) {
                    item { DiskIOCard(server, vm.history) }
                }
                if (server.hasLatency) {
                    item { LatencyCard(server, vm.history, vm.historyRange) }
                }
                item { HistoryCard(vm, vm.history, vm.historyRange, vm.historyLoading, vm.historyError) }
                item { InfoCard(server, now) }
            }
        }
    }
}

@Composable
private fun HeroCard(server: ServerInfo, now: Long) {
    val palette = metricPalette
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                MetricRing(
                    percent = server.cpuPercent,
                    label = "CPU",
                    value = formatPercent(server.cpuPercent),
                    color = palette.cpu,
                    size = 84.dp,
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                MetricRing(
                    percent = server.ramPercent,
                    label = "内存",
                    value = fixed(server.ramPercent.toDouble(), 0) + "%",
                    color = palette.ram,
                    size = 84.dp,
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                MetricRing(
                    percent = server.diskPercent,
                    label = "磁盘",
                    value = fixed(server.diskPercent.toDouble(), 0) + "%",
                    color = palette.disk,
                    size = 84.dp,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            BigNumber(
                value = server.loadAvg.split(" ").firstOrNull { it.isNotBlank() } ?: "--",
                unit = "",
                label = "负载",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.3f),
            )
            BigNumber(
                value = formatUptime(server.bootTime, now),
                unit = "",
                label = "运行时间",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.5f),
            )
            BigNumber(
                value = formatCount(server.processes),
                unit = "",
                label = "进程",
                color = palette.cpu,
                modifier = Modifier.weight(1f),
            )
            BigNumber(
                value = formatCount(
                    (server.tcpConn ?: 0.0) + (server.udpConn ?: 0.0)
                ),
                unit = "",
                label = "连接",
                color = palette.latency,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NetworkCard(server: ServerInfo) {
    val palette = metricPalette
    SectionCard(title = "网络") {
        Row(modifier = Modifier.fillMaxWidth()) {
            BigNumber(
                value = formatBytes(server.netInSpeed),
                unit = "/s",
                label = "实时下行",
                color = palette.netIn,
                modifier = Modifier.weight(1f),
            )
            BigNumber(
                value = formatBytes(server.netOutSpeed),
                unit = "/s",
                label = "实时上行",
                color = palette.netOut,
                modifier = Modifier.weight(1f),
            )
            BigNumber(
                value = formatBytes(server.monthlyTotal),
                unit = "",
                label = "本月流量",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        KeyValueRow(label = "累计下行", value = formatBytes(server.netRx))
        Spacer(Modifier.height(6.dp))
        KeyValueRow(label = "累计上行", value = formatBytes(server.netTx))
        Spacer(Modifier.height(6.dp))
        KeyValueRow(label = "本月下行", value = formatBytes(server.netRxMonthly))
        Spacer(Modifier.height(6.dp))
        KeyValueRow(label = "本月上行", value = formatBytes(server.netTxMonthly))
        if (server.trafficLimit.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            KeyValueRow(label = "流量上限", value = server.trafficLimit)
        }
    }
}

@Composable
private fun DiskIOCard(server: ServerInfo, history: List<HistoryPoint>) {
    val palette = metricPalette
    // 磁盘 IO 是低频指标：WS 通常不推它，快照值更新也慢。历史里带每个采样点的
    // 磁盘字段，取最新一条作为展示值，随历史定时刷新而持续跳动；没有历史时回退快照。
    val latest = history.lastOrNull { it.diskReadBps != null || it.diskWriteBps != null }
    val readBps = latest?.diskReadBps ?: server.diskReadBps
    val writeBps = latest?.diskWriteBps ?: server.diskWriteBps
    val util = latest?.diskUtil ?: server.diskUtil
    val readIops = latest?.diskReadIops ?: server.diskReadIops
    val writeIops = latest?.diskWriteIops ?: server.diskWriteIops
    val awaitMs = latest?.diskAwaitMs ?: server.diskAwaitMs
    SectionCard(title = "磁盘 IO") {
        Row(modifier = Modifier.fillMaxWidth()) {
            BigNumber(
                value = formatBytes(readBps),
                unit = "/s",
                label = "读取",
                color = palette.netIn,
                modifier = Modifier.weight(1f),
            )
            BigNumber(
                value = formatBytes(writeBps),
                unit = "/s",
                label = "写入",
                color = palette.netOut,
                modifier = Modifier.weight(1f),
            )
            BigNumber(
                value = fixed(util, 1),
                unit = "%",
                label = "繁忙度",
                color = palette.disk,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        KeyValueRow(label = "读 IOPS", value = formatCount(readIops))
        Spacer(Modifier.height(6.dp))
        KeyValueRow(label = "写 IOPS", value = formatCount(writeIops))
        Spacer(Modifier.height(6.dp))
        KeyValueRow(label = "平均等待", value = fixed(awaitMs, 2) + " ms")
    }
}

@Composable
private fun LatencyCard(server: ServerInfo, history: List<HistoryPoint>, range: HistoryRange) {
    val palette = metricPalette
    SectionCard(title = "网络延迟") {
        Row(modifier = Modifier.fillMaxWidth()) {
            LatencyCell(
                label = "电信",
                latency = server.pingCt,
                loss = server.lossCt,
                modifier = Modifier.weight(1f),
            )
            LatencyCell(
                label = "联通",
                latency = server.pingCu,
                loss = server.lossCu,
                modifier = Modifier.weight(1f),
            )
            LatencyCell(
                label = "移动",
                latency = server.pingCm,
                loss = server.lossCm,
                modifier = Modifier.weight(1f),
            )
            LatencyCell(
                label = "BGP",
                latency = server.pingBd,
                loss = server.lossBd,
                modifier = Modifier.weight(1f),
            )
        }

        val series = listOf(
            ChartSeries("电信", palette.cpu, history.map { it.pingCt?.toFloat() }, fill = false),
            ChartSeries("联通", palette.ram, history.map { it.pingCu?.toFloat() }, fill = false),
            ChartSeries("移动", palette.disk, history.map { it.pingCm?.toFloat() }, fill = false),
            ChartSeries("BGP", palette.netIn, history.map { it.pingBd?.toFloat() }, fill = false),
        )
        if (series.any { item -> item.values.any { it != null } }) {
            Spacer(Modifier.height(14.dp))
            LineChart(
                series = series,
                timestamps = history.map { it.ts },
                height = 110.dp,
                formatY = { value -> value.toInt().toString() + "ms" },
                xLabelFormatter = { ts -> formatClock(ts, withDate = range == HistoryRange.Week) },
            )
        }
    }
}

@Composable
private fun HistoryCard(
    vm: MonitorViewModel,
    history: List<HistoryPoint>,
    range: HistoryRange,
    loading: Boolean,
    error: String?,
) {
    val palette = metricPalette
    SectionCard(
        title = "历史趋势",
        trailing = {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.6.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        RangeChips(selected = range, onSelect = vm::selectRange)

        Spacer(Modifier.height(14.dp))

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            return@SectionCard
        }

        if (history.isEmpty()) {
            Text(
                text = if (loading) "正在加载…" else "这段时间没有历史数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }

        val timestamps = history.map { it.ts }
        val xLabel: (Long) -> String = { ts -> formatClock(ts, withDate = range == HistoryRange.Week) }

        Text(
            text = "CPU / 内存",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        LineChart(
            series = listOf(
                ChartSeries("CPU", palette.cpu, history.map { it.cpu?.toFloat() }),
                ChartSeries("内存", palette.ram, history.map { it.ramPercent?.toFloat() }),
            ),
            timestamps = timestamps,
            height = 120.dp,
            fixedMax = 100f,
            formatY = { value -> value.toInt().toString() + "%" },
            xLabelFormatter = xLabel,
        )

        Spacer(Modifier.height(18.dp))
        Text(
            text = "网络速率",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        LineChart(
            series = listOf(
                ChartSeries("下行", palette.netIn, history.map { it.netIn?.toFloat() }),
                ChartSeries("上行", palette.netOut, history.map { it.netOut?.toFloat() }),
            ),
            timestamps = timestamps,
            height = 120.dp,
            formatY = { value -> formatBytes(value.toDouble(), 0) },
            xLabelFormatter = xLabel,
        )

        // 磁盘 IO 吞吐：只有历史采样点里带磁盘字段时才显示。
        // 磁盘 IO 属于低频指标，WS 高频推送通常不带它，靠历史曲线才能看到持续变化。
        val hasDiskIoHistory = history.any {
            it.diskReadBps != null || it.diskWriteBps != null
        }
        if (hasDiskIoHistory) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "磁盘 IO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            LineChart(
                series = listOf(
                    ChartSeries("读取", palette.netIn, history.map { it.diskReadBps?.toFloat() }),
                    ChartSeries("写入", palette.netOut, history.map { it.diskWriteBps?.toFloat() }),
                ),
                timestamps = timestamps,
                height = 120.dp,
                formatY = { value -> formatBytes(value.toDouble(), 0) },
                xLabelFormatter = xLabel,
            )
        }
    }
}

@Composable
private fun RangeChips(selected: HistoryRange, onSelect: (HistoryRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HistoryRange.entries.forEach { range ->
            val active = range == selected
            val borderColor = if (active) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outline
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent
                    )
                    .border(1.dp, borderColor, RoundedCornerShape(9.dp))
                    .clickable { onSelect(range) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = range.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun InfoCard(server: ServerInfo, now: Long) {
    SectionCard(title = "系统信息") {
        KeyValueRow(label = "系统", value = shortOs(server.os))
        Spacer(Modifier.height(7.dp))
        KeyValueRow(label = "内核", value = server.kernelVersion.ifBlank { "--" })
        Spacer(Modifier.height(7.dp))
        KeyValueRow(label = "架构", value = server.arch.ifBlank { "--" })
        Spacer(Modifier.height(7.dp))
        KeyValueRow(label = "CPU", value = server.cpuInfo.ifBlank { "--" })
        Spacer(Modifier.height(7.dp))
        KeyValueRow(
            label = "核心数",
            value = server.cpuCores?.let { it.toInt().toString() + " 核" } ?: "--",
        )
        if (server.gpus.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            KeyValueRow(
                label = "GPU",
                value = server.gpus.joinToString("；") { gpu ->
                    val load = gpu.load?.let { " " + fixed(it, 0) + "%" } ?: ""
                    gpu.name + load
                },
            )
        }
        Spacer(Modifier.height(7.dp))
        KeyValueRow(label = "网络栈", value = buildString {
            append(if (server.ipv4) "IPv4" else "无 IPv4")
            append(" / ")
            append(if (server.ipv6) "IPv6" else "无 IPv6")
        })
        Spacer(Modifier.height(7.dp))
        KeyValueRow(label = "探针版本", value = server.agentVersion.ifBlank { "--" })
        Spacer(Modifier.height(7.dp))
        KeyValueRow(label = "启动时间", value = if (server.bootTime > 0) formatClock(server.bootTime, true) else "--")
        Spacer(Modifier.height(7.dp))
        KeyValueRow(
            label = "运行时长",
            value = formatUptime(server.bootTime, now),
        )
        if (server.group.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            KeyValueRow(label = "分组", value = server.group)
        }
        if (server.tagList.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            KeyValueRow(label = "标签", value = server.tagList.joinToString(" / "))
        }
        if (server.expireDate.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            val days = daysUntil(server.expireDate)
            KeyValueRow(
                label = "到期",
                value = server.expireDate + when {
                    days == null -> ""
                    days < 0 -> "（已过期）"
                    else -> "（剩 $days 天）"
                },
            )
        }
        if (server.price.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            val cycle = billingCycleLabel(server.billingCycle)
            KeyValueRow(
                label = "价格",
                value = server.currency + server.price + if (cycle.isNotBlank()) " / $cycle" else "",
            )
        }
        Spacer(Modifier.height(7.dp))
        KeyValueRow(
            label = "磁盘用量",
            value = formatBytes(server.diskUsed * MB) + " / " + formatBytes(server.diskTotal * MB),
        )
        Spacer(Modifier.height(7.dp))
        KeyValueRow(
            label = "内存用量",
            value = formatBytes(server.ramUsed * MB) + " / " + formatBytes(server.ramTotal * MB),
        )
        if (server.swapTotal > 0) {
            Spacer(Modifier.height(7.dp))
            KeyValueRow(
                label = "Swap",
                value = formatBytes(server.swapUsed * MB) + " / " + formatBytes(server.swapTotal * MB),
            )
        }
        Spacer(Modifier.height(7.dp))
        KeyValueRow(
            label = "TCP / UDP",
            value = formatCount(server.tcpConn) + " / " + formatCount(server.udpConn),
        )
    }
}
