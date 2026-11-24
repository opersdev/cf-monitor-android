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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** 界面风格：经典浅色 / 极客毛玻璃 / Bento 环形 / 赛博终端。 */
enum class UiStyle(val id: String, val label: String) {
    CLASSIC("classic", "经典"),
    GLASS("glass", "毛玻璃"),
    BENTO("bento", "Bento"),
    CYBER("cyber", "终端");

    companion object {
        fun from(id: String?): UiStyle = entries.firstOrNull { it.id == id } ?: GLASS
    }
}

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

/** 毛玻璃方案：取消多彩，统一低饱和青色系；>80% 时组件侧切换为 warn 琥珀。 */
private val GlassMetrics = MetricPalette(
    cpu = Color(0xFF22D3EE),
    ram = Color(0xFF22D3EE),
    disk = Color(0xFF22D3EE),
    netIn = Color(0xFF22D3EE),
    netOut = Color(0xFF38BDF8),
    latency = Color(0xFF7DD3FC),
    ok = Color(0xFF4ADE80),
    warn = Color(0xFFF59E0B),
    bad = Color(0xFFF87171),
    track = Color(0xFF334155),
    accent = Color(0xFF22D3EE),
)

/** 赛博终端：纯黑底荧光绿，数字等宽。 */
private val CyberMetrics = MetricPalette(
    cpu = Color(0xFF22C55E),
    ram = Color(0xFF22C55E),
    disk = Color(0xFF22C55E),
    netIn = Color(0xFF4ADE80),
    netOut = Color(0xFF22D3EE),
    latency = Color(0xFF67E8F9),
    ok = Color(0xFF4ADE80),
    warn = Color(0xFFEAB308),
    bad = Color(0xFFF87171),
    track = Color(0xFF0A1A0A),
    accent = Color(0xFF22C55E),
)

/** Bento：浅底多彩但低饱和。 */
private val BentoMetrics = MetricPalette(
    cpu = Color(0xFF6366F1),
    ram = Color(0xFF8B5CF6),
    disk = Color(0xFFF59E0B),
    netIn = Color(0xFF2563EB),
    netOut = Color(0xFF7C3AED),
    latency = Color(0xFF2563EB),
    ok = Color(0xFF059669),
    warn = Color(0xFFD97706),
    bad = Color(0xFFDC2626),
    track = Color(0xFFE8EAF0),
    accent = Color(0xFF6366F1),
)

/** 风格皮肤：卡片 / 胶囊 / 元信息等与 Material 色板并行的外观参数。 */
@Immutable
data class SkinSpec(
    val style: UiStyle,
    val pageBrush: Brush?,
    val cardBg: Color,
    val cardBorder: Color,
    val capsuleBg: Color,
    val capsuleBorder: Color,
    val meta: Color,
    val footer: Color,
    val useRing: Boolean,
    val monoAll: Boolean,
)

private val ClassicSkin = SkinSpec(
    style = UiStyle.CLASSIC,
    pageBrush = null,
    cardBg = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE4EAF2),
    capsuleBg = Color(0xFFFFFFFF),
    capsuleBorder = Color(0xFFE4EAF2),
    meta = Color(0xFF7B8AA0),
    footer = Color(0xFF9AA7B8),
    useRing = false,
    monoAll = false,
)

private val GlassSkin = SkinSpec(
    style = UiStyle.GLASS,
    pageBrush = Brush.verticalGradient(listOf(Color(0xFF0B1120), Color(0xFF0F172A))),
    cardBg = Color(0xCC1E293B),
    cardBorder = Color(0x3A38BDF8),
    capsuleBg = Color(0xB31E293B),
    capsuleBorder = Color(0x2E38BDF8),
    meta = Color(0xFF7C8DA6),
    footer = Color(0xFF64748B),
    useRing = false,
    monoAll = false,
)

private val BentoSkin = SkinSpec(
    style = UiStyle.BENTO,
    pageBrush = null,
    cardBg = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFEEF0F4),
    capsuleBg = Color(0xFFFFFFFF),
    capsuleBorder = Color(0xFFEEF0F4),
    meta = Color(0xFF9CA3AF),
    footer = Color(0xFFB0B7C3),
    useRing = true,
    monoAll = false,
)

private val CyberSkin = SkinSpec(
    style = UiStyle.CYBER,
    pageBrush = null,
    cardBg = Color(0xFF050805),
    cardBorder = Color(0xFF14532D),
    capsuleBg = Color(0xFF0A0F0A),
    capsuleBorder = Color(0xFF14532D),
    meta = Color(0xFF4D7C0F),
    footer = Color(0xFF3F6212),
    useRing = false,
    monoAll = true,
)

val LocalSkin = staticCompositionLocalOf { ClassicSkin }

val skin: SkinSpec
    @Composable
    @ReadOnlyComposable
    get() = LocalSkin.current

private val GlassColors = darkColorScheme(
    primary = Color(0xFF22D3EE),
    onPrimary = Color(0xFF04202B),
    primaryContainer = Color(0xFF123A44),
    onPrimaryContainer = Color(0xFFB9F0FA),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF06121F),
    secondaryContainer = Color(0xFF17273D),
    onSecondaryContainer = Color(0xFFC9DDFF),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF162032),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF7C8DA6),
    outline = Color(0xFF2A3B55),
    outlineVariant = Color(0xFF223147),
    error = Color(0xFFF87171),
    onError = Color(0xFF2A0808),
)

private val CyberColors = darkColorScheme(
    primary = Color(0xFF22C55E),
    onPrimary = Color(0xFF03140A),
    primaryContainer = Color(0xFF0F2E16),
    onPrimaryContainer = Color(0xFFB9F5C9),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF031317),
    secondaryContainer = Color(0xFF0C2E36),
    onSecondaryContainer = Color(0xFFB9EFF7),
    tertiary = Color(0xFFEAB308),
    background = Color(0xFF000000),
    onBackground = Color(0xFFD1FAE5),
    surface = Color(0xFF050805),
    onSurface = Color(0xFFD1FAE5),
    surfaceVariant = Color(0xFF0A1A0A),
    onSurfaceVariant = Color(0xFF4D7C0F),
    outline = Color(0xFF14532D),
    outlineVariant = Color(0xFF0F3D20),
    error = Color(0xFFF87171),
    onError = Color(0xFF2A0808),
)

private val BentoColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF9FAFB),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFEEF0F4),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
)

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
    uiStyle: UiStyle = UiStyle.CLASSIC,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    // 风格自带明暗语义：毛玻璃 / 终端恒为深色，Bento 恒为浅色，经典跟随系统设置。
    val dark = when (uiStyle) {
        UiStyle.GLASS, UiStyle.CYBER -> true
        UiStyle.BENTO -> false
        UiStyle.CLASSIC -> when (themeMode) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }
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
    val metrics = when (uiStyle) {
        UiStyle.GLASS -> GlassMetrics
        UiStyle.CYBER -> CyberMetrics
        UiStyle.BENTO -> BentoMetrics
        UiStyle.CLASSIC -> if (dark) DarkMetrics else LightMetrics
    }
    val scheme = when (uiStyle) {
        UiStyle.GLASS -> GlassColors
        UiStyle.CYBER -> CyberColors
        UiStyle.BENTO -> BentoColors
        UiStyle.CLASSIC -> if (dark) DarkColors else LightColors
    }
    val skinSpec = when (uiStyle) {
        UiStyle.GLASS -> GlassSkin
        UiStyle.CYBER -> CyberSkin
        UiStyle.BENTO -> BentoSkin
        UiStyle.CLASSIC -> if (dark) ClassicSkin.copy(cardBg = Color(0xFF10161F), cardBorder = Color(0xFF1D2735), capsuleBg = Color(0xFF10161F), capsuleBorder = Color(0xFF1D2735), meta = Color(0xFF8895A7), footer = Color(0xFF6B7A8D)) else ClassicSkin
    }
    CompositionLocalProvider(
        LocalMetricPalette provides metrics,
        LocalSkin provides skinSpec,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content,
        )
    }
}
