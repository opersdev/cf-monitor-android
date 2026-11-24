package com.cfsm.monitor

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cfsm.monitor.data.ApiClient
import com.cfsm.monitor.data.ApiException
import com.cfsm.monitor.data.ConfigStore
import com.cfsm.monitor.data.Connection
import com.cfsm.monitor.data.HistoryPoint
import com.cfsm.monitor.data.RealtimeClient
import com.cfsm.monitor.data.ServerInfo
import com.cfsm.monitor.data.ServersPayload
import com.cfsm.monitor.data.SiteConfig
import com.cfsm.monitor.data.Stats
import com.cfsm.monitor.data.SysConfig
import com.cfsm.monitor.data.normalizeBaseUrl
import com.cfsm.monitor.data.parseServer
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.cfsm.monitor.ui.theme.UiStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

sealed interface Screen {
    data object Home : Screen
    data object Setup : Screen
    data object Settings : Screen
    data class Detail(val id: String) : Screen
}

enum class HistoryRange(val label: String, val query: String) {
    TenMinutes("10 分", "0.167"),
    OneHour("1 时", "1"),
    SixHours("6 时", "6"),
    Day("24 时", "24"),
    Week("7 天", "168"),
}

class MonitorViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ConfigStore(app)
    private val api = ApiClient()
    private var realtimeClient: RealtimeClient? = null
    private var autoJob: Job? = null
    private var refreshJob: Job? = null
    private var lastAttemptAt = 0L
    /** 详情页历史的上次刷新时间，用于低频采样指标（磁盘 IO）的持续更新。 */
    private var lastHistoryAt = 0L
    /** 每个服务器每个字段最后一次更新的采样时间，防止旧的 WS 样本覆盖新值。 */
    private val fieldTimes = HashMap<String, HashMap<String, Long>>()

    var screen: Screen by mutableStateOf(if (store.baseUrl.isBlank()) Screen.Setup else Screen.Home)
        private set
    var conn: Connection by mutableStateOf(store.connection())
        private set
    var config: SiteConfig? by mutableStateOf(null)
        private set
    var servers: List<ServerInfo> by mutableStateOf(emptyList())
        private set
    var stats: Stats by mutableStateOf(Stats())
        private set
    var sysConfig: SysConfig by mutableStateOf(SysConfig())
        private set
    var loading: Boolean by mutableStateOf(store.baseUrl.isNotBlank())
        private set
    var refreshing: Boolean by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var lastSync: Long by mutableStateOf(0L)
        private set
    var wsConnected: Boolean by mutableStateOf(false)
        private set
    var wsMessage: String? by mutableStateOf(null)
        private set
    var themeMode: String by mutableStateOf(store.themeMode)
        private set

    /** 界面风格（毛玻璃 / Bento / 终端 / 经典），设置页可切换，即时生效。 */
    var uiStyle: UiStyle by mutableStateOf(UiStyle.from(store.uiStyle))
        private set

    /** 每台服务器最近若干个 CPU 采样点，供终端风 Sparkline 展示趋势。 */
    val cpuHistory: SnapshotStateMap<String, List<Float>> = mutableStateMapOf()
    var refreshSeconds: Int by mutableStateOf(store.refreshSeconds)
        private set
    var now: Long by mutableStateOf(System.currentTimeMillis())
        private set

    var detailId: String by mutableStateOf("")
        private set
    var detailLoading: Boolean by mutableStateOf(false)
        private set
    var history: List<HistoryPoint> by mutableStateOf(emptyList())
        private set
    var historyRange: HistoryRange by mutableStateOf(HistoryRange.Day)
        private set
    var historyLoading: Boolean by mutableStateOf(false)
        private set
    var historyError: String? by mutableStateOf(null)
        private set

    val siteTitle: String
        get() = config?.siteTitle?.takeIf { it.isNotBlank() } ?: store.siteTitle.ifBlank { "服务器监控" }

    val detailServer: ServerInfo?
        get() = servers.firstOrNull { it.id == detailId }

    init {
        if (conn.isConfigured) bootstrap()
    }

    // ------------------------------------------------------------ 加载

    private fun bootstrap() {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                ensureSession()
                loadAll()
                startRealtime()
            } catch (t: Throwable) {
                error = friendly(t)
                if (t is ApiException && t.status == 401 && conn.appPassword.isBlank()) {
                    screen = Screen.Setup
                }
            } finally {
                loading = false
            }
        }
    }

    private suspend fun ensureSession() {
        if (conn.appPassword.isNotBlank() || conn.jwt.isNotBlank()) return
        tryRelogin()
    }

    private suspend fun tryRelogin(): Boolean {
        val user = store.username
        val pass = store.password
        if (user.isBlank() || pass.isBlank()) return false
        return runCatching {
            val token = api.login(conn.baseUrl, user, pass)
            store.saveJwt(token)
            conn = conn.copy(jwt = token)
            true
        }.getOrDefault(false)
    }

    private suspend fun loadAll() {
        val cfg = api.fetchConfig(conn)
        config = cfg
        store.siteTitle = cfg.siteTitle
        val payload = api.fetchServers(conn)
        applyPayload(payload)
        lastSync = System.currentTimeMillis()
    }

    private suspend fun loadWithAuthRetry() {
        try {
            loadAll()
        } catch (t: Throwable) {
            if (t is ApiException && t.status == 401 && conn.appPassword.isBlank() && tryRelogin()) {
                loadAll()
            } else {
                throw t
            }
        }
    }

    fun reload() {
        if (refreshJob?.isActive == true) return
        lastAttemptAt = System.currentTimeMillis()
        refreshJob = viewModelScope.launch {
            refreshing = true
            try {
                ensureSession()
                loadWithAuthRetry()
                error = null
            } catch (t: Throwable) {
                error = friendly(t)
            } finally {
                refreshing = false
            }
        }
    }

    fun saveAndConnect(
        rawUrl: String,
        appPassword: String,
        username: String,
        password: String,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            loading = true
            error = null
            val base = normalizeBaseUrl(rawUrl)
            if (base.isBlank()) {
                loading = false
                onResult("请先填写 Worker 地址")
                return@launch
            }
            var token = ""
            try {
                if (appPassword.isBlank() && username.isNotBlank() && password.isNotBlank()) {
                    token = api.login(base, username.trim(), password)
                }
                val candidate = Connection(
                    baseUrl = base,
                    appPassword = appPassword.trim(),
                    jwt = token,
                    username = username.trim(),
                )
                val cfg = api.fetchConfig(candidate)
                val payload = api.fetchServers(candidate)
                store.saveConnection(base, appPassword.trim(), username.trim(), password, token)
                conn = candidate
                config = cfg
                store.siteTitle = cfg.siteTitle
                applyPayload(payload)
                lastSync = System.currentTimeMillis()
                error = null
                screen = Screen.Home
                onResult(null)
                startRealtime()
            } catch (t: Throwable) {
                val message = friendly(t)
                error = message
                onResult(message)
            } finally {
                loading = false
            }
        }
    }

    private fun applyPayload(payload: ServersPayload) {
        val previous = servers.associateBy { it.id }
        servers = payload.servers.map { fresh ->
            val old = previous[fresh.id]
            if (old == null) {
                fresh
            } else {
                fresh.copy(
                    gpus = if (fresh.gpus.isEmpty()) old.gpus else fresh.gpus,
                    hasDiskIo = fresh.hasDiskIo || old.hasDiskIo,
                )
            }
        }
        stats = payload.stats
        sysConfig = payload.sysConfig
        // REST 快照是最新的完整基线：清掉字段时间记录，让随后的 WS 更新重新累计。
        payload.servers.forEach { fieldTimes.remove(it.id) }
        // 回放快照附带的细粒度采样（cpu / 网速只在采样点上报，不进 servers 快照列）。
        // 此时 fieldTimes 已清空，回放值作为本轮基线写入。
        for (report in payload.reports) {
            for (sample in report.samples) {
                applyPatch(report.serverId, sample.data, sample.ts)
            }
        }
        // 回放里的 applyPatch 会逐台重算汇总；这里再算一次以覆盖"无回放"的服务器，
        // 保证总上下行 / 在线数始终与当前列表一致（必须放在回放之后，否则被覆盖）。
        recomputeStats()
        realtimeClient?.updateIds(servers.map { it.id })
        // 每次 REST 刷新后无条件重发订阅，自愈 DO 重启导致的 serverIds 丢失。
        realtimeClient?.resubscribe()
    }

    private fun replaceServer(full: ServerInfo) {
        val index = servers.indexOfFirst { it.id == full.id }
        if (index < 0) return
        val copy = servers.toMutableList()
        copy[index] = full.copy(hasDiskIo = full.hasDiskIo || copy[index].hasDiskIo)
        servers = copy
    }

    private fun applyPatch(serverId: String, data: JSONObject, ts: Long) {
        val index = servers.indexOfFirst { it.id == serverId }
        if (index < 0) return
        val current = servers[index]

        // 只保留比同字段上一次更新更晚的样本，避免乱序 / 重复帧把网速、CPU 打回旧值。
        val times = fieldTimes.getOrPut(serverId) { HashMap() }
        val baseline = times[DATA_TS] ?: 0L
        val filtered = JSONObject()
        val keys = data.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (ts > 0 && ts < baseline) continue
            filtered.put(key, data.opt(key))
        }
        if (ts > 0) times[DATA_TS] = maxOf(ts, baseline)
        if (filtered.length() == 0) return

        val merged = parseServer(filtered, current)
        if (merged == current) return
        val copy = servers.toMutableList()
        copy[index] = merged
        servers = copy
        // 记录 CPU 采样轨迹（终端风 Sparkline 用），窗口 40 个点。
        recordCpuSample(serverId, merged.cpuPercent)
        // WS 已经带来了新指标，同步刷新同步时间与首页汇总。
        lastSync = System.currentTimeMillis()
        recomputeStats()
    }

    private fun recordCpuSample(serverId: String, cpuPercent: Float) {
        if (cpuPercent.isNaN()) return
        val window = 40
        val next = cpuHistory[serverId].orEmpty().toMutableList()
        next.add(cpuPercent)
        while (next.size > window) next.removeAt(0)
        cpuHistory[serverId] = next
    }

    /**
     * 依据当前服务器列表实时重算汇总。服务端 REST 快照的 stats 只在拉取那一刻
     * 计算，WS 推送单机更新后不会同步；若不在每次 patch 后重算，首页的「总上下行」
     * 会一直停在旧值。online/offline 也按各机 lastUpdated 现算，保持与列表一致。
     */
    private fun recomputeStats() {
        if (servers.isEmpty()) return
        var online = 0
        var speedIn = 0.0
        var speedOut = 0.0
        var netRx = 0.0
        var netTx = 0.0
        val nowMs = System.currentTimeMillis()
        for (s in servers) {
            netRx += s.netRx
            netTx += s.netTx
            if (s.isOnlineAt(nowMs)) {
                online++
                speedIn += s.netInSpeed
                speedOut += s.netOutSpeed
            }
        }
        stats = stats.copy(
            total = servers.size,
            online = online,
            offline = servers.size - online,
            speedIn = speedIn,
            speedOut = speedOut,
            netRx = netRx,
            netTx = netTx,
        )
    }

    private companion object {
        const val DATA_TS = "__data_ts__"
    }

    // ------------------------------------------------------------ 实时

    fun startRealtime() {
        if (conn.baseUrl.isBlank()) return
        realtimeClient?.stop()
        realtimeClient = RealtimeClient(
            http = api.http,
            onState = { connected, message ->
                viewModelScope.launch {
                    wsConnected = connected
                    wsMessage = message
                    if (connected) error = null
                }
            },
            onPatch = { id, data, ts ->
                viewModelScope.launch { applyPatch(id, data, ts) }
            },
        )
        realtimeClient?.connect(conn, servers.map { it.id }, viewModelScope)
    }

    fun stopRealtime() {
        realtimeClient?.stop()
        realtimeClient = null
        wsConnected = false
    }

    fun startAutoRefresh() {
        if (autoJob?.isActive == true) return
        autoJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                now = System.currentTimeMillis()
                // 首页和详情页都要持续更新指标；Setup 无需拉取。
                if (screen is Screen.Setup) continue

                // 详情页：每 10 秒自动刷新一次历史。磁盘 IO 等低频指标不在 WS 高频
                // 推送里，靠历史采样点才能持续更新数字与曲线。与刷新间隔设置无关。
                if (screen is Screen.Detail && detailId.isNotBlank()) {
                    if (now - lastHistoryAt >= 10_000L && refreshJob?.isActive != true) {
                        lastHistoryAt = now
                        loadHistory(historyRange)
                    }
                }

                val interval = refreshSeconds
                // 轮询设为 0 时，若 WS 也没连上，仍用 15 秒保底，避免彻底不跳。
                val effective = if (interval > 0) interval else if (!wsConnected) 15 else 0
                if (effective <= 0) continue
                if (refreshJob?.isActive == true) continue
                if (now - lastAttemptAt >= effective * 1000L) reload()
            }
        }
    }

    fun stopAutoRefresh() {
        autoJob?.cancel()
        autoJob = null
    }

    fun onAppForeground() {
        if (!conn.isConfigured) return
        startAutoRefresh()
        startRealtime()
    }

    fun onAppBackground() {
        stopAutoRefresh()
        stopRealtime()
    }

    // ------------------------------------------------------------ 导航

    fun openDetail(id: String) {
        detailId = id
        history = emptyList()
        historyError = null
        lastHistoryAt = System.currentTimeMillis()
        screen = Screen.Detail(id)
        viewModelScope.launch {
            detailLoading = true
            try {
                replaceServer(api.fetchServer(conn, id))
            } catch (_: Throwable) {
                // 详情接口失败时退化为列表里已有的数据
            } finally {
                detailLoading = false
            }
            loadHistory(historyRange)
        }
    }

    fun selectRange(range: HistoryRange) {
        if (range == historyRange && history.isNotEmpty()) return
        historyRange = range
        viewModelScope.launch { loadHistory(range) }
    }

    fun reloadHistory() {
        viewModelScope.launch { loadHistory(historyRange) }
    }

    private suspend fun loadHistory(range: HistoryRange) {
        val id = detailId
        if (id.isBlank()) return
        historyLoading = true
        historyError = null
        try {
            history = api.fetchHistory(conn, id, range.query)
        } catch (t: Throwable) {
            history = emptyList()
            historyError = friendly(t)
        } finally {
            historyLoading = false
        }
    }

    fun goHome() {
        screen = Screen.Home
        detailId = ""
        history = emptyList()
        historyError = null
    }

    fun goSettings() {
        screen = Screen.Settings
    }

    fun goSetup() {
        screen = Screen.Setup
    }

    fun dismissError() {
        error = null
    }

    // ------------------------------------------------------------ 偏好

    fun setTheme(mode: String) {
        themeMode = mode
        store.themeMode = mode
    }

    fun applyUiStyle(style: UiStyle) {
        uiStyle = style
        store.uiStyle = style.id
    }

    fun setRefreshInterval(seconds: Int) {
        refreshSeconds = seconds
        store.refreshSeconds = seconds
    }

    fun disconnect() {
        stopRealtime()
        stopAutoRefresh()
        store.clearConnection()
        conn = Connection()
        config = null
        servers = emptyList()
        stats = Stats()
        history = emptyList()
        lastSync = 0L
        error = null
        screen = Screen.Setup
    }

    override fun onCleared() {
        stopRealtime()
        stopAutoRefresh()
        super.onCleared()
    }

    // ------------------------------------------------------------ 错误文案

    private fun friendly(t: Throwable): String = when (t) {
        is ApiException -> t.message ?: "请求失败"
        is UnknownHostException -> "域名解析失败，请检查地址是否填写正确"
        is SocketTimeoutException -> "连接超时：请检查网络，或确认 Worker 是否可访问"
        is SSLException -> "TLS 握手失败：请确认使用 https:// 且证书有效"
        else -> t.message?.takeIf { it.isNotBlank() } ?: "未知错误"
    }
}
