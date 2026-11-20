package com.cfsm.monitor.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

/**
 * /api/ws 实时订阅。
 *
 * - subscribe=all 时服务端按批合并推送 batchUpdate
 * - 一批里包含多条采样：cpu / 内存 / 网速等高频字段分散在各条 sample，
 *   最后一条才额外带报告级字段。必须遍历并合并所有 sample，不能只取最后一条。
 * - subscribe=all 默认不推送，连接成功后要发送带 ids 的 subscribe 消息
 * - 断线后指数退避重连，并定期 ping 保活
 */
class RealtimeClient(
    private val http: OkHttpClient,
    private val onState: (connected: Boolean, message: String?) -> Unit,
    private val onPatch: (serverId: String, data: JSONObject, ts: Long) -> Unit,
) {

    private var socket: WebSocket? = null
    private var scope: CoroutineScope? = null
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null
    private var attempt = 0
    private var stopped = true
    private var conn: Connection = Connection()
    private var ids: List<String> = emptyList()
    private val lock = Mutex()

    fun connect(connection: Connection, subscribeIds: List<String>, coroutineScope: CoroutineScope) {
        conn = connection
        ids = subscribeIds
        scope = coroutineScope
        stopped = false
        attempt = 0
        open()
    }

    /**
     * 更新订阅范围。服务器列表的时机可能晚于 WS 建连（App 前台重连时尤其常见），
     * 因此这里在拿到 id 后会补发一次订阅，避免空订阅导致服务端不推送。
     */
    fun updateIds(subscribeIds: List<String>) {
        ids = subscribeIds
        val current = socket ?: return
        // 用内容签名而非数量：服务器数量不变但 id 变化时也要补发订阅，
        // 否则服务端 serverIds 不含新 id，那几台服务器永远收不到推送。
        if (ids.isNotEmpty() && ids.joinToString(",") != currentIdSignature) {
            sendSubscribe(current)
        }
    }

    private var currentIdSignature = ""

    /**
     * 强制重发订阅。Durable Object 休眠/重启后 attachment 里的 serverIds 会丢失，
     * 此时连接看似正常但服务端不投递。每次 REST 刷新后重发一次可自愈这种"假连接"。
     */
    fun resubscribe() {
        val current = socket ?: return
        if (ids.isNotEmpty()) sendSubscribe(current)
    }

    fun stop() {
        stopped = true
        reconnectJob?.cancel()
        reconnectJob = null
        pingJob?.cancel()
        pingJob = null
        socket?.close(1000, "bye")
        socket = null
        onState(false, null)
    }

    private fun sendSubscribe(webSocket: WebSocket) {
        val array = JSONArray()
        ids.forEach { array.put(it) }
        currentIdSignature = ids.joinToString(",")
        val payload = JSONObject()
            .put("type", "subscribe")
            .put("scope", "all")
            .put("ids", array)
        webSocket.send(payload.toString())
    }

    private fun open() {
        if (stopped) return
        val url = buildWsUrl(conn)
        val builder = Request.Builder()
            .url(url)
            // CF Worker / Turnstile 会按 UA 判断客户端；REST 已经带了 UA，
            // 握手这里也必须带，否则可能被拦截或被当作脚本连接降级。
            .header("User-Agent", ApiClient.USER_AGENT)
            .header("Accept", "application/json")
        if (conn.appPassword.isNotBlank()) builder.header("X-App-Password", conn.appPassword)
        if (conn.jwt.isNotBlank()) builder.header("Authorization", "Bearer ${conn.jwt}")
        currentIdSignature = ""
        socket = http.newWebSocket(builder.build(), listener)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            attempt = 0
            onState(true, null)
            // 有 id 就立刻订阅；没有则等 updateIds 补发。
            if (ids.isNotEmpty()) sendSubscribe(webSocket)
            startPing(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val root = runCatching { JSONObject(text) }.getOrNull() ?: return
            when (root.text("type")) {
                "batchUpdate" -> handleBatch(root)
                "hello", "subscribed", "pong" -> Unit
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (socket !== webSocket) return
            socket = null
            pingJob?.cancel()
            pingJob = null
            onState(false, t.message)
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (socket !== webSocket) return
            socket = null
            pingJob?.cancel()
            pingJob = null
            onState(false, reason.ifBlank { "连接已关闭" })
            scheduleReconnect()
        }
    }

    /** 应用层 ping，防止中间代理因空闲切断连接。 */
    private fun startPing(webSocket: WebSocket) {
        pingJob?.cancel()
        pingJob = scope?.launch {
            while (isActive && !stopped && socket === webSocket) {
                delay(25_000L)
                if (socket !== webSocket) break
                runCatching { webSocket.send("{\"type\":\"ping\"}") }
            }
        }
    }

    private fun handleBatch(root: JSONObject) {
        val updates = root.optJSONArray("updates") ?: return
        for (update in parseReportUpdates(updates)) {
            // 按时间升序逐条下发。cpu / net_in_speed / net_out_speed 只出现在
            // 高频采样点里，必须逐条合并，不能只取最后一条报告级样本。
            for (sample in update.samples) {
                onPatch(update.serverId, sample.data, sample.ts)
            }
        }
    }

    private fun scheduleReconnect() {
        if (stopped) return
        val target = scope ?: return
        reconnectJob?.cancel()
        reconnectJob = target.launch {
            attempt = (attempt + 1).coerceAtMost(6)
            val wait = (1000L * (1 shl (attempt - 1))).coerceAtMost(15_000L)
            delay(wait)
            if (!stopped && isActive) {
                lock.withLock { if (socket == null) open() }
            }
        }
    }
}
