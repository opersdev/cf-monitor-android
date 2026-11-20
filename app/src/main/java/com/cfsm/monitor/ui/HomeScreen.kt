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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cfsm.monitor.MonitorViewModel
import com.cfsm.monitor.data.ServerInfo
import com.cfsm.monitor.data.Stats
import com.cfsm.monitor.data.SysConfig
import com.cfsm.monitor.ui.components.MetricBar
import com.cfsm.monitor.ui.components.StatusDot
import com.cfsm.monitor.ui.components.Pill
import com.cfsm.monitor.ui.theme.metricPalette

@Composable
fun HomeScreen(vm: MonitorViewModel) {
    val servers = vm.servers
    val now = vm.now
    val sys = vm.sysConfig

    LaunchedEffect(Unit) {
        vm.startAutoRefresh()
        vm.startRealtime()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        HomeHeader(vm, now)

        if (vm.error != null && servers.isNotEmpty()) {
            ErrorBanner(vm.error.orEmpty(), onRetry = vm::reload, onSetup = vm::goSetup)
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                servers.isEmpty() && vm.loading -> CenterBox {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                servers.isEmpty() && vm.error != null -> CenterBox {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = vm.error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Row {
                            TextButton(onClick = vm::reload) { Text("重试") }
                            TextButton(onClick = vm::goSetup) { Text("修改连接") }
                        }
                    }
                }

                servers.isEmpty() -> CenterBox {
                    Text(
                        text = "还没有服务器，请先在后台添加并安装探针",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    items(items = servers, key = { it.id }) { server ->
                        ServerCard(
                            server = server,
                            sys = sys,
                            now = now,
                            onClick = { vm.openDetail(server.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun HomeHeader(vm: MonitorViewModel, now: Long) {
    val palette = metricPalette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vm.siteTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(
                        color = if (vm.wsConnected) palette.ok else MaterialTheme.colorScheme.outline,
                        size = 7.dp,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (vm.wsConnected) "实时" else "离线",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (vm.wsConnected) palette.ok else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = " · 更新于 ${relativeTime(vm.lastSync, now)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = vm::reload) {
                if (vm.refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "刷新",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = vm::goSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        StatsRow(vm.stats)
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun StatsRow(stats: Stats) {
    val palette = metricPalette
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCell(
            label = "在线",
            value = "${stats.online}/${stats.total}",
            valueColor = if (stats.offline > 0) palette.warn else palette.ok,
            modifier = Modifier.weight(1f),
        )
        StatCell(
            label = "总下行",
            value = formatSpeed(stats.speedIn),
            valueColor = palette.netIn,
            modifier = Modifier.weight(1f),
        )
        StatCell(
            label = "总上行",
            value = formatSpeed(stats.speedOut),
            valueColor = palette.netOut,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit, onSetup: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onRetry) {
            Text("重试", style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = onSetup) {
            Text("设置", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ServerCard(
    server: ServerInfo,
    sys: SysConfig,
    now: Long,
    onClick: () -> Unit,
) {
    val palette = metricPalette
    val online = server.isOnlineAt(now)
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(
                color = if (online) palette.ok else MaterialTheme.colorScheme.outline,
                size = 9.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = server.name.ifBlank { "未命名" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (server.region.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Pill(
                    text = regionLabel(server.region),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = relativeTime(server.lastUpdated, now),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val subtitle = buildSubtitle(server)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(11.dp))
        MetricBar(
            label = "CPU",
            value = formatPercent(server.cpuPercent),
            percent = server.cpuPercent,
            color = palette.cpu,
            dimmed = !online,
        )
        Spacer(Modifier.height(7.dp))
        MetricBar(
            label = "内存",
            value = formatUsage(server.ramUsed * MB, server.ramTotal * MB),
            percent = server.ramPercent,
            color = palette.ram,
            dimmed = !online,
        )
        Spacer(Modifier.height(7.dp))
        MetricBar(
            label = "磁盘",
            value = formatUsage(server.diskUsed * MB, server.diskTotal * MB),
            percent = server.diskPercent,
            color = palette.disk,
            dimmed = !online,
        )

        Spacer(Modifier.height(11.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SpeedLabel("↓", formatSpeed(server.netInSpeed), palette.netIn)
            Spacer(Modifier.width(12.dp))
            SpeedLabel("↑", formatSpeed(server.netOutSpeed), palette.netOut)
            Spacer(Modifier.weight(1f))
            if (sys.showTraffic) {
                Text(
                    text = "月流量 " + formatBytes(server.monthlyTotal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        if (server.hasLatency) {
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LatencyInline("电信", server.pingCt, palette)
                LatencyInline("联通", server.pingCu, palette)
                LatencyInline("移动", server.pingCm, palette)
                LatencyInline("BGP", server.pingBd, palette)
            }
        }

        val footer = buildFooter(server, sys)
        if (footer.isNotBlank()) {
            Spacer(Modifier.height(9.dp))
            Text(
                text = footer,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


private fun buildSubtitle(server: ServerInfo): String {
    val parts = ArrayList<String>(4)
    if (server.os.isNotBlank()) parts.add(shortOs(server.os))
    if (server.arch.isNotBlank()) parts.add(server.arch)
    val cores = server.cpuCores
    if (cores != null && cores > 0) parts.add("${cores.toInt()} 核")
    if (server.loadAvg.isNotBlank()) parts.add("负载 " + server.loadAvg)
    return parts.joinToString(" · ")
}

private fun buildFooter(server: ServerInfo, sys: SysConfig): String {
    val parts = ArrayList<String>(3)
    if (sys.showPrice && server.price.isNotBlank()) {
        val cycle = billingCycleLabel(server.billingCycle)
        parts.add(server.currency + server.price + if (cycle.isNotBlank()) "/$cycle" else "")
    }
    if (sys.showExpire && server.expireDate.isNotBlank()) {
        val days = daysUntil(server.expireDate)
        val suffix = when {
            days == null -> ""
            days < 0 -> "（已过期）"
            days <= 15 -> "（剩 $days 天）"
            else -> ""
        }
        parts.add("到期 ${server.expireDate}$suffix")
    }
    if (server.trafficLimit.isNotBlank() && sys.showTraffic) {
        parts.add("限额 ${server.trafficLimit}")
    }
    return parts.joinToString("  ·  ")
}

@Composable
private fun SpeedLabel(arrow: String, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = arrow,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun LatencyInline(label: String, latency: com.cfsm.monitor.data.Latency?, palette: com.cfsm.monitor.ui.theme.MetricPalette) {
    val color = when {
        latency == null -> MaterialTheme.colorScheme.outline
        latency.timeout || latency.isBad -> palette.bad
        latency.isWarn -> palette.warn
        else -> palette.ok
    }
    val text = when {
        latency == null -> "—"
        latency.timeout -> "超时"
        else -> "${latency.ms.toInt()}"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
            color = color,
        )
    }
}
