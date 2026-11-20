package com.cfsm.monitor.data

import org.json.JSONObject

/**
 * CF-Server-Monitor 的接口返回类型非常"宽容"：同一个字段可能是数字、数字字符串、
 * 布尔 false、JSON null 或整个缺失（例如 ping_ct / loss_ct）。这里统一做容错转换，
 * 避免任何一处类型不符就整页解析失败。
 */

internal fun Any?.asDoubleOrNull(): Double? = when (this) {
    null, JSONObject.NULL -> null
    is Number -> this.toDouble()
    is String -> this.trim().toDoubleOrNull()
    is Boolean -> if (this) 1.0 else 0.0
    else -> null
}

internal fun Any?.asLongOrNull(): Long? = when (this) {
    null, JSONObject.NULL -> null
    is Number -> this.toLong()
    is String -> this.trim().toLongOrNull() ?: this.trim().toDoubleOrNull()?.toLong()
    is Boolean -> if (this) 1L else 0L
    else -> null
}

internal fun Any?.asTextOrEmpty(): String = when (this) {
    null, JSONObject.NULL -> ""
    is String -> this
    is Double -> if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
    is Boolean -> if (this) "1" else "0"
    else -> this.toString()
}

internal fun Any?.asBoolOrNull(): Boolean? = when (this) {
    null, JSONObject.NULL -> null
    is Boolean -> this
    is Number -> this.toInt() != 0
    is String -> when (this.trim().lowercase()) {
        "true", "1", "yes", "on" -> true
        "false", "0", "no", "off", "" -> false
        else -> null
    }
    else -> null
}

internal fun JSONObject.text(key: String): String = opt(key).asTextOrEmpty()

internal fun JSONObject.number(key: String): Double? = if (!has(key)) null else opt(key).asDoubleOrNull()

internal fun JSONObject.millis(key: String): Long? = if (!has(key)) null else opt(key).asLongOrNull()

internal fun JSONObject.flag(key: String, default: Boolean = false): Boolean =
    if (!has(key)) default else (opt(key).asBoolOrNull() ?: default)
