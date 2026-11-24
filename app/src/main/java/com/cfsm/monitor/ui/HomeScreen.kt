package com.cfsm.monitor.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cfsm.monitor.MonitorViewModel
import com.cfsm.monitor.data.ServerInfo
import com.cfsm.monitor.data.Stats
import com.cfsm.monitor.data.SysConfig
import com.cfsm.monitor.ui.components.LatencyBadge
import com.cfsm.monitor.ui.components.MetricColumn
import com.cfsm.monitor.ui.components.MetricRing
import com.cfsm.monitor.ui.components.Pill
import com.cfsm.monitor.ui.components.Sparkline
import com.cfsm.monitor.ui.components.StatusDot
import com.cfsm.monitor.ui.theme.MetricPalette
import com.cfsm.monitor.ui.theme.UiStyle
import com.cfsm.monitor.ui.theme.metricPalette
import com.cfsm.monitor.ui.theme.skin

@Composable
fun HomeScreen(vm: MonitorViewModel) {
    val servers = vm.servers
    val now = vm.now
    val sys = vm.sysConfig
    val sk = skin
    var regionFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.startAutoRefresh()
        vm.startRealtime()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .let { base ->
                if (sk.pageBrush != null) base.background(sk.pageBrush)
                else base.background(MaterialTheme.colorScheme.background)
            }
    ) {
        HomeHeader(vm, now)

        RegionFilterBar(servers, regionFilter, onSelect = { regionFilter = it })

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

                else -> {
                    val filtered = if (regionFilter == null) servers else servers.filter { it.region == regionFilter }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.navigationBarsPadding(),
                    ) {
                        items(items = filtered, key = { it.id }) { server ->
                            ServerCard(
                                server = server,
                                sys = sys,
                                now = now,
                                cpuHistory = vm.cpuHistory[server.id].orEmpty(),
                                onClick = { vm.openDetail(server.id) },
                            )
                        }
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

/** 标题右侧的实时呼吸灯：连接时呼吸闪烁，断开时灰点静止。 */
@Composable
private fun BreathingDot(active: Boolean) {
    val palette = metricPalette
    val color = if (active) palette.ok else MaterialTheme.colorScheme.outline
    if (!active) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color, CircleShape),
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "breath")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "breathAlpha",
    )
    Box(
        modifier = Modifier
            .size(9.dp)
            .alpha(alpha)
            .background(color, CircleShape),
    )
}

@Composable
private fun HomeHeader(vm: MonitorViewModel, now: Long) {
    val sk = skin
    val palette = metricPalette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = vm.siteTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            BreathingDot(active = vm.wsConnected)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = vm::reload, modifier = Modifier.size(36.dp)) {
                if (vm.refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
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
            IconButton(onClick = vm::goSettings, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        CapsuleBar(vm.stats, palette)
    }
}

/** 紧凑悬浮数据胶囊条：在线 / 总下行 / 总上行合并为一条。 */
@Composable
private fun CapsuleBar(stats: Stats, palette: MetricPalette) {
    val sk = skin
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(sk.capsuleBg)
            .border(1.dp, sk.capsuleBorder, shape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "在线 ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${stats.online}/${stats.total}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (stats.offline > 0) palette.warn else palette.ok,
        )
        CapsuleDivider()
        Text(
            text = "↓ ",
            style = MaterialTheme.typography.labelSmall,
            color = palette.netIn,
        )
        Text(
            text = formatSpeed(stats.speedIn),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
        )
        CapsuleDivider()
        Text(
            text = "↑ ",
            style = MaterialTheme.typography.labelSmall,
            color = palette.netOut,
        )
        Text(
            text = formatSpeed(stats.speedOut),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CapsuleDivider() {
    Spacer(Modifier.width(9.dp))
    Text(
        text = "·",
        style = MaterialTheme.typography.labelSmall,
        color = skin.meta,
    )
    Spacer(Modifier.width(9.dp))
}

/** 地区分类筛选条：全部 / US / CN / HK，带旗帜与数量。 */
@Composable
private fun RegionFilterBar(
    servers: List<ServerInfo>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val counts = LinkedHashMap<String, Int>()
    servers.forEach { s ->
        val region = s.region.trim().uppercase()
        if (region.isNotBlank()) counts.merge(region, 1, Int::plus)
    }
    if (counts.size < 2) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp),
    ) {
        RegionChip(text = "全部 ${servers.size}", selected = selected == null) { onSelect(null) }
        counts.entries
            .sortedByDescending { it.value }
            .forEach { (region, count) ->
                RegionChip(text = "${flagEmoji(region)} ${regionLabel(region)} $count", selected = selected == region) {
                    onSelect(region)
                }
            }
    }
}

@Composable
private fun RegionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val sk = skin
    val shape = RoundedCornerShape(999.dp)
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else sk.capsuleBg
    val border = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else sk.capsuleBorder
    val textColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
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
    cpuHistory: List<Float>,
    onClick: () -> Unit,
) {
    val palette = metricPalette
    val sk = skin
    val style = sk.style
    val online = server.isOnlineAt(now)
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(sk.cardBg)
            .border(1.dp, sk.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // ---- 头部：状态点 + 国旗 + 名称 + IP 版本徽章 + 到期倒计时 ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(
                color = if (online) palette.ok else MaterialTheme.colorScheme.outline,
                size = 9.dp,
            )
            Spacer(Modifier.width(8.dp))
            val flag = flagEmoji(server.region)
            if (flag.isNotBlank()) {
                Text(
                    text = flag,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = server.name.ifBlank { "未命名" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            val ipBadge = when {
                server.ipv4 && server.ipv6 -> "IPv4/6"
                server.ipv4 -> "IPv4"
                server.ipv6 -> "IPv6"
                else -> ""
            }
            if (ipBadge.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Pill(text = ipBadge, color = MaterialTheme.colorScheme.primary)
            }
            val dueDays = if (sys.showExpire && server.expireDate.isNotBlank()) {
                daysUntil(server.expireDate)
            } else {
                null
            }
            if (dueDays != null && dueDays in 0..15) {
                Spacer(Modifier.width(6.dp))
                Pill(
                    text = "剩 $dueDays 天",
                    color = if (dueDays <= 7) palette.bad else palette.warn,
                )
            }
            Spacer(Modifier.weight(1f))
            if (!online) {
                Text(
                    text = "离线",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.bad,
                )
            }
        }

        // ---- 去噪副标题：系统 · 核数 · 开机时间 · 价格 · 到期 ----
        val subtitle = buildSubtitleCompact(server, sys, now)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = sk.meta,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(10.dp))

        // ---- 指标区：Bento 环形 / 三栏细条（终端风带 Sparkline）----
        when (style) {
            UiStyle.BENTO -> MetricsRingRow(server, online)
            else -> MetricsGridColumn(server, online, style == UiStyle.CYBER, cpuHistory)
        }

        Spacer(Modifier.height(10.dp))

        // ---- 网速行 ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            SpeedLabel("↓", formatSpeed(server.netInSpeed), palette.netIn)
            Spacer(Modifier.width(12.dp))
            SpeedLabel("↑", formatSpeed(server.netOutSpeed), palette.netOut)
            Spacer(Modifier.weight(1f))
            if (sys.showTraffic) {
                Text(
                    text = "月流量 " + formatBytes(server.monthlyTotal),
                    style = MaterialTheme.typography.labelSmall,
                    color = sk.meta,
                    maxLines = 1,
                )
            }
        }

        // ---- 三网延迟胶囊 ----
        if (server.hasLatency) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LatencyBadge("电信", server.pingCt, Modifier.weight(1f))
                LatencyBadge("联通", server.pingCu, Modifier.weight(1f))
                LatencyBadge("移动", server.pingCm, Modifier.weight(1f))
                LatencyBadge("BGP", server.pingBd, Modifier.weight(1f))
            }
        }

    }
}

/** 三栏网格指标：CPU / 内存 / 磁盘并排，卡片高度砍半。 */
@Composable
private fun MetricsGridColumn(server: ServerInfo, online: Boolean, cyber: Boolean, cpuHistory: List<Float>) {
    val palette = metricPalette
    val accentStyle = skin.style == UiStyle.GLASS || cyber
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricColumn(
            label = "CPU",
            percentText = formatPercent(server.cpuPercent),
            percent = server.cpuPercent,
            color = palette.cpu,
            modifier = Modifier.weight(1f),
            extraText = loadTriple(server.loadAvg),
            warn = accentStyle && server.cpuPercent > 80f,
            dimmed = !online,
        )
        MetricColumn(
            label = "内存",
            percentText = formatPercent(server.ramPercent),
            percent = server.ramPercent,
            color = palette.ram,
            modifier = Modifier.weight(1f),
            capacityText = formatUsage(server.ramUsed * MB, server.ramTotal * MB),
            warn = accentStyle && server.ramPercent > 80f,
            dimmed = !online,
        )
        MetricColumn(
            label = "磁盘",
            percentText = formatPercent(server.diskPercent),
            percent = server.diskPercent,
            color = palette.disk,
            modifier = Modifier.weight(1f),
            capacityText = formatUsage(server.diskUsed * MB, server.diskTotal * MB),
            warn = accentStyle && server.diskPercent > 80f,
            dimmed = !online,
        )
    }
    if (cyber && cpuHistory.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Sparkline(
            values = cpuHistory,
            color = if (server.cpuPercent > 80f) palette.warn else palette.cpu,
            dimmed = !online,
        )
    }
}

/** Bento 风格：三列环形图。 */
@Composable
private fun MetricsRingRow(server: ServerInfo, online: Boolean) {
    val palette = metricPalette
    val sk = skin
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RingCell(
            label = "CPU",
            percent = server.cpuPercent,
            value = formatPercent(server.cpuPercent),
            capacity = loadTriple(server.loadAvg).ifBlank { "负载 --" },
            color = palette.cpu,
            online = online,
            modifier = Modifier.weight(1f),
        )
        RingCell(
            label = "内存",
            percent = server.ramPercent,
            value = formatPercent(server.ramPercent),
            capacity = formatUsage(server.ramUsed * MB, server.ramTotal * MB),
            color = palette.ram,
            online = online,
            modifier = Modifier.weight(1f),
        )
        RingCell(
            label = "磁盘",
            percent = server.diskPercent,
            value = formatPercent(server.diskPercent),
            capacity = formatUsage(server.diskUsed * MB, server.diskTotal * MB),
            color = palette.disk,
            online = online,
            modifier = Modifier.weight(1f),
        )
    }
    // 消除未使用警告：环形风格下卡片底色直接来自 skin。
    @Suppress("UNUSED_EXPRESSION") sk
}

@Composable
private fun RingCell(
    label: String,
    percent: Float,
    value: String,
    capacity: String,
    color: Color,
    online: Boolean,
    modifier: Modifier = Modifier,
) {
    val sk = skin
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MetricRing(
            percent = percent,
            label = "",
            value = value,
            color = color,
            size = 52.dp,
            strokeWidth = 6.dp,
            dimmed = !online,
            compact = true,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = sk.meta,
        )
        Text(
            text = capacity,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
            ),
            color = sk.footer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
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

/** 1/5/15 分钟负载三元组，数据缺失返回空串。 */
private fun loadTriple(loadAvg: String): String {
    val parts = loadAvg.trim().split(Regex("\\s+")).filter { it.isNotBlank() && it != "-" }
    return parts.joinToString(" ")
}

/** 第二行：系统简称 · 核数 · 开机时长 · 价格 · 到期日期（不带文字标签）。 */
private fun buildSubtitleCompact(server: ServerInfo, sys: SysConfig, now: Long): String {
    val parts = ArrayList<String>(5)
    if (server.os.isNotBlank()) parts.add(shortOs(server.os))
    val cores = server.cpuCores
    if (cores != null && cores > 0) parts.add("${cores.toInt()}C")
    val up = shortUptime(server.bootTime, now)
    if (up.isNotBlank()) parts.add(up)
    if (sys.showPrice && server.price.isNotBlank()) {
        val cycle = billingCycleLabel(server.billingCycle)
        parts.add(server.currency + server.price + if (cycle.isNotBlank()) "/$cycle" else "")
    }
    if (sys.showExpire && server.expireDate.isNotBlank()) {
        parts.add(server.expireDate)
    }
    return parts.joinToString(" · ")
}

/** 开机时长的紧凑写法：25 天 / 8 小时 / 12 分。 */
private fun shortUptime(bootTime: Long, now: Long): String {
    if (bootTime <= 0L) return ""
    val total = (now - bootTime) / 1000L
    if (total <= 0L) return ""
    val days = total / 86_400
    val hours = (total % 86_400) / 3600
    return when {
        days > 0 -> "$days 天"
        hours > 0 -> "$hours 小时"
        else -> "${total / 60} 分"
    }
}
