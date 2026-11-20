package com.cfsm.monitor.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** 指标专用色板（CPU/内存/磁盘/网络等），与 Material 主题色分开维护。 */
@Immutable
data class MetricPalette(
    val cpu: Color,
    val ram: Color,
    val disk: Color,
    val netIn: Color,
    val netOut: Color,
    val latency: Color,
    val ok: Color,
    val warn: Color,
    val bad: Color,
    val track: Color,
    val accent: Color,
)

private val DarkMetrics = MetricPalette(
    cpu = Color(0xFF5B9CFF),
    ram = Color(0xFFB18CFF),
    disk = Color(0xFFFFB35C),
    netIn = Color(0xFF35D0BA),
    netOut = Color(0xFFFF8A65),
    latency = Color(0xFF7FD1FF),
    ok = Color(0xFF3ED598),
    warn = Color(0xFFFFC24B),
    bad = Color(0xFFFF6B6B),
    track = Color(0xFF1C2531),
    accent = Color(0xFF35D0BA),
)

private val LightMetrics = MetricPalette(
    cpu = Color(0xFF2F7BE8),
    ram = Color(0xFF7C5CFF),
    disk = Color(0xFFE8890C),
    netIn = Color(0xFF0FA98F),
    netOut = Color(0xFFE05B2B),
    latency = Color(0xFF2C8FD6),
    ok = Color(0xFF16A672),
    warn = Color(0xFFD98B00),
    bad = Color(0xFFD93B3B),
    track = Color(0xFFE8EEF5),
    accent = Color(0xFF0EA5A0),
)

val LocalMetricPalette = staticCompositionLocalOf { DarkMetrics }

val metricPalette: MetricPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalMetricPalette.current

private val DarkColors = darkColorScheme(
    primary = Color(0xFF35D0BA),
    onPrimary = Color(0xFF04201C),
    primaryContainer = Color(0xFF123A34),
    onPrimaryContainer = Color(0xFFB9F5EA),
    secondary = Color(0xFF5B9CFF),
    onSecondary = Color(0xFF06121F),
    secondaryContainer = Color(0xFF17273D),
    onSecondaryContainer = Color(0xFFC9DDFF),
    tertiary = Color(0xFFFFB35C),
    background = Color(0xFF070A0E),
    onBackground = Color(0xFFE9EFF7),
    surface = Color(0xFF0F141B),
    onSurface = Color(0xFFE9EFF7),
    surfaceVariant = Color(0xFF161D26),
    onSurfaceVariant = Color(0xFF8895A7),
    outline = Color(0xFF25303D),
    outlineVariant = Color(0xFF1B242F),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0808),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0EA5A0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6F5F1),
    onPrimaryContainer = Color(0xFF04322F),
    secondary = Color(0xFF2F7BE8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1ECFF),
    onSecondaryContainer = Color(0xFF07203F),
    tertiary = Color(0xFFE8890C),
    background = Color(0xFFF2F5F9),
    onBackground = Color(0xFF101A26),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101A26),
    surfaceVariant = Color(0xFFF6F8FB),
    onSurfaceVariant = Color(0xFF66748A),
    outline = Color(0xFFDCE4EE),
    outlineVariant = Color(0xFFE9EEF5),
    error = Color(0xFFD93B3B),
    onError = Color(0xFFFFFFFF),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp,
    ),
)

@Composable
fun CfsmTheme(
    themeMode: String = "auto",
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalMetricPalette provides if (dark) DarkMetrics else LightMetrics) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
