package com.cfsm.monitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import com.cfsm.monitor.BuildConfig
import com.cfsm.monitor.MonitorViewModel
import com.cfsm.monitor.ui.components.KeyValueRow
import com.cfsm.monitor.ui.components.SectionCard
import com.cfsm.monitor.ui.theme.metricPalette

@Composable
fun SettingsScreen(vm: MonitorViewModel) {
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
                .padding(start = 4.dp, end = 12.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = vm::goHome) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = "连接") {
                KeyValueRow(label = "站点地址", value = vm.conn.baseUrl.ifBlank { "--" })
                Spacer(Modifier.height(7.dp))
                KeyValueRow(label = "认证方式", value = authLabel(vm))
                Spacer(Modifier.height(7.dp))
                KeyValueRow(
                    label = "实时推送",
                    value = if (vm.wsConnected) "已连接" else "未连接",
                    valueColor = if (vm.wsConnected) palette.ok else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(7.dp))
                KeyValueRow(label = "服务端版本", value = vm.config?.version?.ifBlank { "--" } ?: "--")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip(text = "修改连接", onClick = vm::goSetup)
                    ActionChip(text = "立即刷新", onClick = vm::reload)
                }
            }

            SectionCard(title = "外观") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionChip(
                        text = "跟随系统",
                        selected = vm.themeMode == "auto",
                        onClick = { vm.setTheme("auto") },
                    )
                    OptionChip(
                        text = "深色",
                        selected = vm.themeMode == "dark",
                        onClick = { vm.setTheme("dark") },
                    )
                    OptionChip(
                        text = "浅色",
                        selected = vm.themeMode == "light",
                        onClick = { vm.setTheme("light") },
                    )
                }
            }

            SectionCard(title = "自动刷新") {
                Text(
                    text = "按间隔拉取最新指标，是实时推送之外的保底。默认 5 秒，确保网速 / CPU 持续跳动；即使 WebSocket 连不上也会生效。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionChip(
                        text = "5 秒",
                        selected = vm.refreshSeconds == 5,
                        onClick = { vm.setRefreshInterval(5) },
                    )
                    OptionChip(
                        text = "10 秒",
                        selected = vm.refreshSeconds == 10,
                        onClick = { vm.setRefreshInterval(10) },
                    )
                    OptionChip(
                        text = "30 秒",
                        selected = vm.refreshSeconds == 30,
                        onClick = { vm.setRefreshInterval(30) },
                    )
                    OptionChip(
                        text = "关闭",
                        selected = vm.refreshSeconds == 0,
                        onClick = { vm.setRefreshInterval(0) },
                    )
                }
            }

            SectionCard(title = "关于") {
                KeyValueRow(label = "客户端", value = "CF 监控 ${BuildConfig.VERSION_NAME}（Android）")
                Spacer(Modifier.height(7.dp))
                KeyValueRow(label = "协议", value = "CF-Server-Monitor REST + WebSocket")
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "本应用只是 CF-Server-Monitor 的只读客户端：数据全部来自你自己的 Worker，" +
                        "不包含探针安装、后台管理或远程控制能力。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { vm.disconnect() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "断开连接并清除本地数据",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Default),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun authLabel(vm: MonitorViewModel): String = when {
    vm.conn.appPassword.isNotBlank() -> "App 访问口令"
    vm.conn.jwt.isNotBlank() -> "管理员登录（JWT）"
    vm.conn.username.isNotBlank() -> "管理员账号（待登录）"
    else -> "公开访问"
}

@Composable
private fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent
            )
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun ActionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
