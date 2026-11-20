package com.cfsm.monitor.data

import org.json.JSONArray
import org.json.JSONObject

/** 单条延迟/丢包。null 表示站点未配置或本轮未采样（前端不显示）。 */
data class Latency(val ms: Double, val timeout: Boolean) {
    val isBad: Boolean get() = timeout || ms >= 200
    val isWarn: Boolean get() = !timeout && ms >= 80

    companion object {
        val TIMEOUT = Latency(-1.0, true)
    }
}

data class GpuInfo(
    val name: String = "",
    val load: Double? = null,
    val memUsed: Double? = null,
    val memTotal: Double? = null,
)

data class ServerInfo(
    val id: String = "",
    val name: String = "",
    val group: String = "",
    val tags: String = "",
    val price: String = "",
    val currency: String = "¥",
    val billingCycle: String = "",
    val expireDate: String = "",
    val trafficLimit: String = "",
    val isHidden: Boolean = false,
    val sortOrder: Int = 0,

    val cpu: Double? = null,
    val cpuCores: Double? = null,
    val cpuInfo: String = "",
    val loadAvg: String = "",
    val gpus: List<GpuInfo> = emptyList(),

    val ramTotal: Double = 0.0,
    val ramUsed: Double = 0.0,
    val swapTotal: Double = 0.0,
    val swapUsed: Double = 0.0,
    val diskTotal: Double = 0.0,
    val diskUsed: Double = 0.0,

    val diskReadBps: Double = 0.0,
    val diskWriteBps: Double = 0.0,
    val diskReadIops: Double = 0.0,
    val diskWriteIops: Double = 0.0,
    val diskAwaitMs: Double = 0.0,
    val diskUtil: Double = 0.0,
    val hasDiskIo: Boolean = false,

    val netInSpeed: Double = 0.0,
    val netOutSpeed: Double = 0.0,
    val netRx: Double = 0.0,
    val netTx: Double = 0.0,
    val netRxMonthly: Double = 0.0,
    val netTxMonthly: Double = 0.0,

    val processes: Double? = null,
    val tcpConn: Double? = null,
    val udpConn: Double? = null,

    val pingCt: Latency? = null,
    val pingCu: Latency? = null,
    val pingCm: Latency? = null,
    val pingBd: Latency? = null,
    val lossCt: Double? = null,
    val lossCu: Double? = null,
    val lossCm: Double? = null,
    val lossBd: Double? = null,

    val os: String = "",
    val arch: String = "",
    val kernelVersion: String = "",
    val agentVersion: String = "",
    val region: String = "",
    val regionName: String = "",
    val ipv4: Boolean = false,
    val ipv6: Boolean = false,
    val bootTime: Long = 0L,
    val lastUpdated: Long = 0L,
) {
    val ramPercent: Float
        get() = if (ramTotal <= 0) 0f else (ramUsed / ramTotal * 100.0).toFloat().coerceIn(0f, 100f)

    val swapPercent: Float
        get() = if (swapTotal <= 0) 0f else (swapUsed / swapTotal * 100.0).toFloat().coerceIn(0f, 100f)

    val diskPercent: Float
        get() = if (diskTotal <= 0) 0f else (diskUsed / diskTotal * 100.0).toFloat().coerceIn(0f, 100f)

    val cpuPercent: Float
        get() = (cpu ?: 0.0).toFloat().coerceIn(0f, 100f)

    val monthlyTotal: Double get() = netRxMonthly + netTxMonthly

    val tagList: List<String>
        get() = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    val hasLatency: Boolean
        get() = pingCt != null || pingCu != null || pingCm != null || pingBd != null

    fun isOnlineAt(now: Long): Boolean = lastUpdated > 0L && now - lastUpdated < OFFLINE_AFTER_MS

    companion object {
        const val OFFLINE_AFTER_MS = 300_000L
    }
}

data class Stats(
    val total: Int = 0,
    val online: Int = 0,
    val offline: Int = 0,
    val speedIn: Double = 0.0,
    val speedOut: Double = 0.0,
    val netRx: Double = 0.0,
    val netTx: Double = 0.0,
)

data class SysConfig(
    val showPrice: Boolean = false,
    val showExpire: Boolean = true,
    val showTraffic: Boolean = true,
    val showThreeNet: Boolean = false,
    val displayMode: String = "bar",
)

data class SiteConfig(
    val version: String = "",
    val isPublic: Boolean = true,
    val siteTitle: String = "服务器监控",
    val displayMode: String = "bar",
    val preferredTheme: String = "auto",
    val ctName: String = "电信",
    val cuName: String = "联通",
    val cmName: String = "移动",
    val bdName: String = "BGP",
    val turnstileEnabled: Boolean = false,
    val longHistoryPoints: Int = 120,
    val wsTimeoutMinutes: Int = 0,
    val latencyPoints: Int = 20,
    val latencyHours: Double = 2.0,
)

/** 采样回放里的一条样本：时间戳 + 增量指标。 */
data class SampleUpdate(val ts: Long, val data: JSONObject)

/** 单台服务器最近一次批量上报的采样回放。 */
data class ReportUpdate(
    val serverId: String = "",
    val reportTs: Long = 0L,
    val samples: List<SampleUpdate> = emptyList(),
)

data class ServersPayload(
    val servers: List<ServerInfo> = emptyList(),
    val stats: Stats = Stats(),
    val sysConfig: SysConfig = SysConfig(),
    /** REST 快照附带的最新采样回放，用于在新页面/断线时让指标继续跳动。 */
    val reports: List<ReportUpdate> = emptyList(),
)

data class HistoryPoint(
    val ts: Long = 0L,
    val cpu: Double? = null,
    val ramUsed: Double? = null,
    val ramTotal: Double? = null,
    val diskUsed: Double? = null,
    val diskTotal: Double? = null,
    val netIn: Double? = null,
    val netOut: Double? = null,
    val diskReadBps: Double? = null,
    val diskWriteBps: Double? = null,
    val diskReadIops: Double? = null,
    val diskWriteIops: Double? = null,
    val diskAwaitMs: Double? = null,
    val diskUtil: Double? = null,
    val loadAvg: String? = null,
    val pingCt: Double? = null,
    val pingCu: Double? = null,
    val pingCm: Double? = null,
    val pingBd: Double? = null,
    val lossCt: Double? = null,
    val lossCu: Double? = null,
    val lossCm: Double? = null,
    val lossBd: Double? = null,
) {
    val ramPercent: Double?
        get() {
            val used = ramUsed ?: return null
            val total = ramTotal ?: return null
            if (total <= 0) return null
            return used / total * 100.0
        }

    val diskPercent: Double?
        get() {
            val used = diskUsed ?: return null
            val total = diskTotal ?: return null
            if (total <= 0) return null
            return used / total * 100.0
        }
}

// ---------------------------------------------------------------- 解析

private fun parseLatency(o: JSONObject, key: String, base: Latency?): Latency? {
    if (!o.has(key)) return base
    return when (val v = o.opt(key)) {
        null, JSONObject.NULL -> Latency.TIMEOUT
        is Boolean -> if (v) base else null
        is Number -> Latency(v.toDouble(), false)
        is String -> v.trim().toDoubleOrNull()?.let { Latency(it, false) }
        else -> base
    }
}

private fun parseLoss(o: JSONObject, key: String, base: Double?): Double? {
    if (!o.has(key)) return base
    return when (val v = o.opt(key)) {
        null, JSONObject.NULL -> base
        is Boolean -> if (v) base else null
        else -> v.asDoubleOrNull() ?: base
    }
}

private fun parseGpus(raw: Any?): List<GpuInfo> {
    val array: JSONArray = when (raw) {
        is JSONArray -> raw
        is String -> {
            val text = raw.trim()
            if (text.isEmpty() || text == "null" || text == "[]") return emptyList()
            runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
        }
        else -> return emptyList()
    }
    val result = ArrayList<GpuInfo>(array.length())
    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        result.add(
            GpuInfo(
                name = item.text("name"),
                load = item.number("info") ?: item.number("load"),
                memUsed = item.number("mem_used"),
                memTotal = item.number("mem_total"),
            )
        )
    }
    return result
}

/** Agent 高频采样 payload 里的磁盘 IO 扁平字段名。 */
private val DISK_FLAT_COLUMNS = arrayOf(
    "disk_read_bps", "disk_write_bps", "disk_read_iops",
    "disk_write_iops", "disk_await_ms", "disk_util",
)

fun parseServer(o: JSONObject, base: ServerInfo = ServerInfo()): ServerInfo {
    fun text(key: String, cur: String): String = if (o.has(key)) o.text(key) else cur
    fun num(key: String, cur: Double?): Double? = if (o.has(key)) o.number(key) else cur
    fun num0(key: String, cur: Double): Double = if (o.has(key)) (o.number(key) ?: cur) else cur
    fun millis(key: String, cur: Long): Long = if (o.has(key)) (o.millis(key) ?: cur) else cur
    fun flag(key: String, cur: Boolean): Boolean = if (o.has(key)) o.flag(key, cur) else cur

    val disk = o.optJSONObject("disk")
    // 磁盘 IO 有两种来源：REST / 报告级采样里是嵌套 disk:{read_bps...}；
    // Agent 高频采样的 payload 里是扁平 disk_read_bps / disk_write_bps 等
    // （服务端 DISK_IO_FIELD_TO_COLUMN 映射）。两种都要认，否则详情页 IO 不跳。
    fun diskNum(field: String, column: String, cur: Double): Double {
        val nested = disk?.number(field)
        val flat = if (o.has(column)) o.number(column) else null
        return nested ?: flat ?: cur
    }
    val diskFieldsPresent = disk != null || DISK_FLAT_COLUMNS.any { o.has(it) }
    val hasDiskIo = base.hasDiskIo || diskFieldsPresent

    return base.copy(
        id = text("id", base.id),
        name = text("name", base.name),
        group = text("server_group", base.group),
        tags = text("tags", base.tags),
        price = text("price", base.price),
        currency = text("currency", base.currency).ifBlank { "¥" },
        billingCycle = text("billing_cycle", base.billingCycle),
        expireDate = text("expire_date", base.expireDate),
        trafficLimit = text("traffic_limit", base.trafficLimit),
        isHidden = text("is_hidden", if (base.isHidden) "1" else "0") == "1",
        sortOrder = (o.number("sort_order") ?: base.sortOrder.toDouble()).toInt(),

        cpu = num("cpu", base.cpu),
        cpuCores = num("cpu_cores", base.cpuCores),
        cpuInfo = text("cpu_info", base.cpuInfo),
        loadAvg = text("load_avg", base.loadAvg),
        gpus = if (o.has("gpu_info")) parseGpus(o.opt("gpu_info")) else base.gpus,

        ramTotal = num0("ram_total", base.ramTotal),
        ramUsed = num0("ram_used", base.ramUsed),
        swapTotal = num0("swap_total", base.swapTotal),
        swapUsed = num0("swap_used", base.swapUsed),
        diskTotal = num0("disk_total", base.diskTotal),
        diskUsed = num0("disk_used", base.diskUsed),

        diskReadBps = diskNum("read_bps", "disk_read_bps", base.diskReadBps),
        diskWriteBps = diskNum("write_bps", "disk_write_bps", base.diskWriteBps),
        diskReadIops = diskNum("read_iops", "disk_read_iops", base.diskReadIops),
        diskWriteIops = diskNum("write_iops", "disk_write_iops", base.diskWriteIops),
        diskAwaitMs = diskNum("await_ms", "disk_await_ms", base.diskAwaitMs),
        diskUtil = diskNum("util", "disk_util", base.diskUtil),
        hasDiskIo = hasDiskIo,

        netInSpeed = num0("net_in_speed", base.netInSpeed),
        netOutSpeed = num0("net_out_speed", base.netOutSpeed),
        netRx = num0("net_rx", base.netRx),
        netTx = num0("net_tx", base.netTx),
        netRxMonthly = num0("net_rx_monthly", base.netRxMonthly),
        netTxMonthly = num0("net_tx_monthly", base.netTxMonthly),

        processes = num("processes", base.processes),
        tcpConn = num("tcp_conn", base.tcpConn),
        udpConn = num("udp_conn", base.udpConn),

        pingCt = parseLatency(o, "ping_ct", base.pingCt),
        pingCu = parseLatency(o, "ping_cu", base.pingCu),
        pingCm = parseLatency(o, "ping_cm", base.pingCm),
        pingBd = parseLatency(o, "ping_bd", base.pingBd),
        lossCt = parseLoss(o, "loss_ct", base.lossCt),
        lossCu = parseLoss(o, "loss_cu", base.lossCu),
        lossCm = parseLoss(o, "loss_cm", base.lossCm),
        lossBd = parseLoss(o, "loss_bd", base.lossBd),

        os = text("os", base.os),
        arch = text("arch", base.arch),
        kernelVersion = text("kernel_version", base.kernelVersion),
        agentVersion = text("agent_version", base.agentVersion),
        region = text("region", base.region).uppercase(),
        regionName = text("region_name", base.regionName),
        ipv4 = text("ip_v4", if (base.ipv4) "1" else "0") == "1",
        ipv6 = text("ip_v6", if (base.ipv6) "1" else "0") == "1",
        bootTime = millis("boot_time", base.bootTime),
        lastUpdated = millis("last_updated", base.lastUpdated),
    )
}

fun parseStats(o: JSONObject?): Stats {
    if (o == null) return Stats()
    return Stats(
        total = (o.number("total") ?: 0.0).toInt(),
        online = (o.number("online") ?: 0.0).toInt(),
        offline = (o.number("offline") ?: 0.0).toInt(),
        speedIn = o.number("globalSpeedIn") ?: 0.0,
        speedOut = o.number("globalSpeedOut") ?: 0.0,
        netRx = o.number("globalNetRx") ?: 0.0,
        netTx = o.number("globalNetTx") ?: 0.0,
    )
}

fun parseSysConfig(o: JSONObject?): SysConfig {
    if (o == null) return SysConfig()
    val showTf = if (o.has("show_tf")) o.flag("show_tf", true) else true
    return SysConfig(
        showPrice = o.flag("show_price", false),
        showExpire = o.flag("show_expire", true),
        showTraffic = showTf,
        showThreeNet = o.flag("show_three_net_details", false),
        displayMode = o.text("display_mode").ifBlank { "bar" },
    )
}

fun parseSiteConfig(o: JSONObject): SiteConfig {
    val latency = o.optJSONObject("latency_window")
    return SiteConfig(
        version = o.text("version"),
        isPublic = o.flag("is_public", true),
        siteTitle = o.text("site_title").ifBlank { "服务器监控" },
        displayMode = o.text("display_mode").ifBlank { "bar" },
        preferredTheme = o.text("preferred_theme").ifBlank { "auto" },
        ctName = o.text("custom_ct_name").ifBlank { "电信" },
        cuName = o.text("custom_cu_name").ifBlank { "联通" },
        cmName = o.text("custom_cm_name").ifBlank { "移动" },
        bdName = o.text("custom_bd_name").ifBlank { "BGP" },
        turnstileEnabled = o.flag("turnstile_enabled", false),
        longHistoryPoints = (o.number("long_history_points") ?: 120.0).toInt().coerceIn(1, 500),
        wsTimeoutMinutes = (o.number("frontend_ws_timeout_minutes") ?: 0.0).toInt(),
        latencyPoints = (latency?.number("points") ?: 20.0).toInt(),
        latencyHours = latency?.number("hours") ?: 2.0,
    )
}

/** 解析 batchUpdate / latestReportUpdates 共用的 samples 结构。 */
fun parseReportUpdates(array: org.json.JSONArray?): List<ReportUpdate> {
    if (array == null) return emptyList()
    val result = ArrayList<ReportUpdate>()
    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        val serverId = item.text("serverId")
        if (serverId.isBlank()) continue
        val samples = ArrayList<SampleUpdate>()
        val rawSamples = item.optJSONArray("samples") ?: continue
        for (j in 0 until rawSamples.length()) {
            val sample = rawSamples.optJSONObject(j) ?: continue
            // 服务端 toBroadcastSamples 在高频采样点用的是 payload，最后一条报告级
            // 采样可能用 data；内部归一化路径还会用 metrics。三种都要兼容，否则
            // 采样会被整条跳过，表现为 WS 连着但 cpu / 网速纹丝不动。
            val data = sample.optJSONObject("data")
                ?: sample.optJSONObject("payload")
                ?: sample.optJSONObject("metrics")
                ?: continue
            val ts = sample.opt("ts").asLongOrNull()
                ?: sample.opt("timestamp").asLongOrNull()
                ?: data.opt("sample_timestamp").asLongOrNull()
                ?: data.opt("timestamp").asLongOrNull()
                ?: data.opt("last_updated").asLongOrNull()
                ?: data.opt("report_timestamp").asLongOrNull()
                ?: 0L
            samples.add(SampleUpdate(ts, data))
        }
        samples.sortBy { it.ts }
        result.add(
            ReportUpdate(
                serverId = serverId,
                reportTs = item.opt("reportTs").asLongOrNull() ?: 0L,
                samples = samples,
            )
        )
    }
    return result
}

fun parseServersPayload(o: JSONObject): ServersPayload {
    val list = ArrayList<ServerInfo>()
    val array = o.optJSONArray("servers")
    if (array != null) {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            list.add(parseServer(item))
        }
    }
    return ServersPayload(
        servers = list,
        stats = parseStats(o.optJSONObject("stats")),
        sysConfig = parseSysConfig(o.optJSONObject("sysConfig")),
        reports = parseReportUpdates(o.optJSONArray("latestReportUpdates")),
    )
}

fun parseHistory(text: String): List<HistoryPoint> {
    val array = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
    val result = ArrayList<HistoryPoint>(array.length())
    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        result.add(
            HistoryPoint(
                ts = item.millis("timestamp") ?: 0L,
                cpu = item.number("cpu"),
                ramUsed = item.number("ram_used"),
                ramTotal = item.number("ram_total"),
                diskUsed = item.number("disk_used"),
                diskTotal = item.number("disk_total"),
                netIn = item.number("net_in_speed"),
                netOut = item.number("net_out_speed"),
                diskReadBps = item.number("disk_read_bps"),
                diskWriteBps = item.number("disk_write_bps"),
                diskReadIops = item.number("disk_read_iops"),
                diskWriteIops = item.number("disk_write_iops"),
                diskAwaitMs = item.number("disk_await_ms"),
                diskUtil = item.number("disk_util"),
                loadAvg = if (item.has("load_avg")) item.text("load_avg") else null,
                pingCt = item.number("ping_ct"),
                pingCu = item.number("ping_cu"),
                pingCm = item.number("ping_cm"),
                pingBd = item.number("ping_bd"),
                lossCt = item.number("loss_ct"),
                lossCu = item.number("loss_cu"),
                lossCm = item.number("loss_cm"),
                lossBd = item.number("loss_bd"),
            )
        )
    }
    return result
}
