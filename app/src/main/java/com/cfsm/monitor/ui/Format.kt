package com.cfsm.monitor.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/** 1 MB 的字节数，用于把接口返回的 MB 值换算成字节。 */
const val MB = 1024.0 * 1024.0

private val BYTE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB")

fun formatBytes(bytes: Double, decimals: Int = 1): String {
    if (bytes.isNaN() || bytes.isInfinite()) return "--"
    var value = abs(bytes)
    var index = 0
    while (value >= 1024.0 && index < BYTE_UNITS.size - 1) {
        value /= 1024.0
        index++
    }
    val sign = if (bytes < 0) "-" else ""
    val digits = if (index == 0) 0 else decimals
    return sign + fixed(value, digits) + " " + BYTE_UNITS[index]
}

/** 已用/总量使用同一单位，避免卡片右侧空间不足被截断。 */
fun formatUsage(usedBytes: Double, totalBytes: Double): String {
    if (usedBytes.isNaN() || usedBytes.isInfinite() ||
        totalBytes.isNaN() || totalBytes.isInfinite() || totalBytes <= 0.0
    ) return "--"
    var base = abs(totalBytes)
    var index = 0
    while (base >= 1024.0 && index < BYTE_UNITS.size - 1) {
        base /= 1024.0
        index++
    }
    val used = abs(usedBytes) / 1024.0.pow(index)
    val digits = if (index >= 2 && base >= 10.0) 0 else 1
    return fixed(used, digits) + "/" + fixed(base, digits) + " " + BYTE_UNITS[index]
}

fun formatSpeed(bytesPerSecond: Double): String {
    if (bytesPerSecond <= 0.0) return "0 B/s"
    return formatBytes(bytesPerSecond) + "/s"
}

fun fixed(value: Double, decimals: Int): String =
    String.format(Locale.US, "%." + decimals + "f", value)

fun formatCount(value: Double?): String = when {
    value == null || value < 0 -> "--"
    value >= 100_000 -> fixed(value / 1000.0, 0) + "k"
    else -> value.toInt().toString()
}

fun formatPercent(value: Float): String = fixed(value.toDouble(), 1) + "%"

fun formatPercent(value: Double?): String =
    if (value == null || value.isNaN()) "--" else fixed(value, 1) + "%"

fun relativeTime(timestamp: Long, now: Long): String {
    if (timestamp <= 0L) return "从未上报"
    val diff = (now - timestamp) / 1000L
    return when {
        diff < 5 -> "刚刚"
        diff < 60 -> "${diff} 秒前"
        diff < 3600 -> "${diff / 60} 分钟前"
        diff < 86_400 -> "${diff / 3600} 小时前"
        else -> "${diff / 86_400} 天前"
    }
}

fun formatUptime(bootTime: Long, now: Long): String {
    if (bootTime <= 0L) return "--"
    val total = (now - bootTime) / 1000L
    if (total <= 0L) return "--"
    val days = total / 86_400
    val hours = (total % 86_400) / 3600
    val minutes = (total % 3600) / 60
    return when {
        days > 0 -> "${days} 天 ${hours} 小时"
        hours > 0 -> "${hours} 小时 ${minutes} 分"
        else -> "${minutes} 分"
    }
}

fun formatClock(timestamp: Long, withDate: Boolean = false): String {
    if (timestamp <= 0L) return ""
    val pattern = if (withDate) "MM-dd HH:mm" else "HH:mm"
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    return format.format(Date(timestamp))
}

fun daysUntil(expireDate: String): Int? {
    if (expireDate.isBlank()) return null
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val date = runCatching { format.parse(expireDate) }.getOrNull() ?: return null
    val diff = date.time - System.currentTimeMillis()
    return Math.floorDiv(diff, 86_400_000L).toInt()
}

fun billingCycleLabel(cycle: String): String = when (cycle) {
    "month" -> "月付"
    "quarter" -> "季付"
    "half_year" -> "半年付"
    "year" -> "年付"
    "two_years" -> "两年付"
    "three_years" -> "三年付"
    "four_years" -> "四年付"
    "five_years" -> "五年付"
    else -> ""
}

/** 只显示国家/地区代码文本（如 US / CN / HK）。 */
fun regionLabel(region: String, fallback: String = ""): String {
    val code = region.trim().uppercase()
    return when {
        code.length == 2 && code.all { it in 'A'..'Z' } -> code
        fallback.isNotBlank() -> fallback
        else -> "--"
    }
}

/** 由 ISO 3166-1 两位地区码生成旗帜 emoji（无旗帜环境则显示代码文本）。 */
fun flagEmoji(region: String): String {
    val code = region.trim().uppercase()
    if (code.length != 2 || code.any { it !in 'A'..'Z' }) return ""
    return code.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}

/** 系统名去噪：Debian Linux 12 (bookworm) → Debian 12。 */
fun shortOs(os: String): String {
    if (os.isBlank()) return "未知系统"
    var s = os
        .replace("GNU/Linux", " ")
        .replace("Linux", " ")
        .replace(Regex("\\s*\\([^)]*\\)"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (s.isBlank()) s = os.trim()
    return s
}
