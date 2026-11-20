package com.cfsm.monitor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(
    val status: Int,
    message: String,
    val rawCode: String = "",
) : IOException(message)

/** 一次连接所需的全部凭据。 */
data class Connection(
    val baseUrl: String = "",
    val appPassword: String = "",
    val jwt: String = "",
    val username: String = "",
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
}

/** 把用户随手输入的地址规范成 https://host 形式。 */
fun normalizeBaseUrl(raw: String): String {
    var text = raw.trim()
    if (text.isEmpty()) return ""
    if (text.startsWith("http://", true) || text.startsWith("https://", true)) {
        // 已经是完整地址
    } else {
        text = "https://$text"
    }
    text = text.trimEnd('/')
    val lower = text.lowercase()
    val adminIndex = lower.indexOf("/admin")
    if (adminIndex > 8) {
        text = text.substring(0, adminIndex)
    }
    return text.trimEnd('/')
}

fun buildWsUrl(conn: Connection): String {
    val wsBase = when {
        conn.baseUrl.startsWith("https://", true) -> "wss://" + conn.baseUrl.substring(8)
        conn.baseUrl.startsWith("http://", true) -> "ws://" + conn.baseUrl.substring(7)
        else -> "wss://" + conn.baseUrl
    }
    val token = if (conn.jwt.isNotBlank()) "&token=${conn.jwt}" else ""
    return "$wsBase/api/ws?subscribe=all$token"
}

class ApiClient {

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun Request.Builder.withAuth(conn: Connection): Request.Builder {
        header("Accept", "application/json")
        header("User-Agent", USER_AGENT)
        if (conn.appPassword.isNotBlank()) header("X-App-Password", conn.appPassword)
        if (conn.jwt.isNotBlank()) header("Authorization", "Bearer ${conn.jwt}")
        return this
    }

    private fun execute(request: Request): String {
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw mapError(response.code, body)
            return body
        }
    }

    suspend fun fetchConfig(conn: Connection): SiteConfig = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${conn.baseUrl}/api/config")
            .withAuth(conn)
            .get()
            .build()
        val body = execute(request)
        val json = runCatching { JSONObject(body) }.getOrElse {
            throw ApiException(-1, "站点返回的不是 JSON，可能地址填错或命中了错误页面")
        }
        parseSiteConfig(json)
    }

    suspend fun fetchServers(conn: Connection): ServersPayload = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${conn.baseUrl}/api/servers")
            .withAuth(conn)
            .get()
            .build()
        val body = execute(request)
        val json = runCatching { JSONObject(body) }.getOrElse {
            throw ApiException(-1, "响应解析失败")
        }
        parseServersPayload(json)
    }

    suspend fun fetchServer(conn: Connection, id: String): ServerInfo = withContext(Dispatchers.IO) {
        val url = "${conn.baseUrl}/api/server".toHttpUrlOrNull()
            ?: throw ApiException(-1, "站点地址不合法")
        val full = url.newBuilder().addQueryParameter("id", id).build()
        val request = Request.Builder().url(full).withAuth(conn).get().build()
        val body = execute(request)
        val json = runCatching { JSONObject(body) }.getOrElse {
            throw ApiException(-1, "响应解析失败")
        }
        parseServer(json)
    }

    suspend fun fetchHistory(conn: Connection, id: String, hours: String): List<HistoryPoint> =
        withContext(Dispatchers.IO) {
            val url = "${conn.baseUrl}/api/history/all".toHttpUrlOrNull()
                ?: throw ApiException(-1, "站点地址不合法")
            val full = url.newBuilder()
                .addQueryParameter("id", id)
                .addQueryParameter("hours", hours)
                .build()
            val request = Request.Builder().url(full).withAuth(conn).get().build()
            parseHistory(execute(request))
        }

    suspend fun login(baseUrl: String, username: String, password: String): String =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("action", "login")
                .put("username", username)
                .put("password", password)
                .toString()
            val request = Request.Builder()
                .url("$baseUrl/admin/api")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .post(payload.toRequestBody(jsonMedia))
                .build()
            val body = execute(request)
            val json = runCatching { JSONObject(body) }.getOrNull()
                ?: throw ApiException(-1, "登录响应无法解析")
            val token = json.text("token")
            if (token.isBlank()) throw ApiException(401, "登录失败：账号或密码不正确")
            if (json.has("success") && !json.flag("success", false)) {
                throw ApiException(401, "登录失败：账号或密码不正确")
            }
            token
        }

    private fun mapError(status: Int, body: String): ApiException {
        val raw = runCatching {
            val json = JSONObject(body)
            when {
                json.has("error") -> json.text("error")
                json.has("message") -> json.text("message")
                else -> ""
            }
        }.getOrDefault("")

        val message = when (status) {
            400 -> when {
                raw.contains("API_SECRET", true) ->
                    "Worker 没有读到 API_SECRET，请先在 Cloudflare 环境变量中配置并重新部署"
                raw.contains("missingCredentials", true) -> "请填写管理员账号和密码"
                else -> "请求参数有误" + if (raw.isBlank()) "" else "（$raw）"
            }
            401 -> "未授权：站点未开启公开访问，请在后台「安全设置 → App 访问口令」设置口令后填入本 App"
            403 -> "被 Cloudflare 人机验证拦截：请在后台「安全设置 → App 访问口令」设置口令，本 App 用口令访问可绕过 Turnstile"
            404 -> "服务器不存在，或在前台被隐藏"
            409 -> "数据库需要升级：请登录后台「数据库管理 → 升级数据库」"
            426 -> "WebSocket 升级失败"
            429 -> "请求过于频繁，已被站点限流"
            503 -> "站点未启用实时推送（未绑定 Durable Object）"
            else -> if (raw.isNotBlank()) raw else "请求失败（HTTP $status）"
        }
        return ApiException(status, message, raw)
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/126.0.0.0 Mobile Safari/537.36 CFServerMonitorApp/1.0"
    }
}
