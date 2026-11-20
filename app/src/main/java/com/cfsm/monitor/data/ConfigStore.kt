package com.cfsm.monitor.data

import android.content.Context

/** 本地保存连接信息与偏好（SharedPreferences，够用且无额外依赖）。 */
class ConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences("cfsm_prefs", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_BASE, value).apply()

    var appPassword: String
        get() = prefs.getString(KEY_APP_PASS, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_APP_PASS, value).apply()

    var username: String
        get() = prefs.getString(KEY_USER, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_USER, value).apply()

    var password: String
        get() = prefs.getString(KEY_PASS, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_PASS, value).apply()

    var jwt: String
        get() = prefs.getString(KEY_JWT, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_JWT, value).apply()

    /** auto / dark / light */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, "auto").orEmpty().ifBlank { "auto" }
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    /** 自动轮询间隔（秒），作为 WS 之外的保底；0 表示只依赖实时推送 */
    var refreshSeconds: Int
        get() = prefs.getInt(KEY_REFRESH, 5)
        set(value) = prefs.edit().putInt(KEY_REFRESH, value).apply()

    var siteTitle: String
        get() = prefs.getString(KEY_TITLE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TITLE, value).apply()

    fun connection(): Connection = Connection(
        baseUrl = baseUrl,
        appPassword = appPassword,
        jwt = jwt,
        username = username,
    )

    fun saveConnection(
        baseUrl: String,
        appPassword: String,
        username: String,
        password: String,
        token: String,
    ) {
        prefs.edit()
            .putString(KEY_BASE, baseUrl)
            .putString(KEY_APP_PASS, appPassword)
            .putString(KEY_USER, username)
            .putString(KEY_PASS, password)
            .putString(KEY_JWT, token)
            .apply()
    }

    fun saveJwt(token: String) {
        prefs.edit().putString(KEY_JWT, token).apply()
    }

    /** 只清除连接信息，保留主题与刷新偏好。 */
    fun clearConnection() {
        prefs.edit()
            .remove(KEY_BASE)
            .remove(KEY_APP_PASS)
            .remove(KEY_USER)
            .remove(KEY_PASS)
            .remove(KEY_JWT)
            .remove(KEY_TITLE)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_BASE = "base_url"
        const val KEY_APP_PASS = "app_password"
        const val KEY_USER = "username"
        const val KEY_PASS = "password"
        const val KEY_JWT = "jwt"
        const val KEY_THEME = "theme_mode"
        const val KEY_REFRESH = "refresh_seconds"
        const val KEY_TITLE = "site_title"
    }
}
