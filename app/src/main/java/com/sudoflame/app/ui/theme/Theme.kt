package com.sudoflame.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Brand {
    val Violet = Color(0xFF6B3BFF)
    val VioletDark = Color(0xFF3A1490)
    val Night = Color(0xFF24095E)
    val Flame = Color(0xFFFF7A1A)
    val FlameDeep = Color(0xFFFF3D6E)
    val Sun = Color(0xFFFFC93C)
    val Mint = Color(0xFF14C79A)
    val Sky = Color(0xFF3FA9FF)
    val Ice = Color(0xFF7FD6FF)
    val Error = Color(0xFFFF4D5E)

    val flameBrush = Brush.verticalGradient(listOf(Sun, Flame, FlameDeep))
    val violetBrush = Brush.linearGradient(listOf(Violet, Night))
    val mintBrush = Brush.linearGradient(listOf(Color(0xFF2EE6B0), Color(0xFF0E9C86)))
    val skyBrush = Brush.linearGradient(listOf(Color(0xFF63C3FF), Color(0xFF3567FF)))
    val sunsetBrush = Brush.linearGradient(listOf(Color(0xFFFFB347), Color(0xFFFF5F6D)))
    val iceBrush = Brush.linearGradient(listOf(Color(0xFFB8ECFF), Color(0xFF5BB8FF)))
}

@Immutable
data class AppColors(
    val bg: Color,
    val card: Color,
    val text: Color,
    val subtext: Color,
    val line: Color,
    val boardBg: Color,
    val cellSelected: Color,
    val cellRelated: Color,
    val cellSame: Color,
    val given: Color,
    val entered: Color,
)

private val LightApp = AppColors(
    bg = Color(0xFFFFF7F0), card = Color.White, text = Color(0xFF1E1440), subtext = Color(0xFF7A7192),
    line = Color(0xFFEDE6F7), boardBg = Color.White, cellSelected = Color(0xFFCDBBFF),
    cellRelated = Color(0xFFF1ECFF), cellSame = Color(0xFFE0D4FF), given = Color(0xFF1E1440), entered = Color(0xFF6B3BFF),
)
private val DarkApp = AppColors(
    bg = Color(0xFF130B2B), card = Color(0xFF21163F), text = Color(0xFFF4EFFF), subtext = Color(0xFFA79FC2),
    line = Color(0xFF33285A), boardBg = Color(0xFF1B1236), cellSelected = Color(0xFF4F33A8),
    cellRelated = Color(0xFF261A4A), cellSame = Color(0xFF3A2780), given = Color(0xFFF4EFFF), entered = Color(0xFFB39BFF),
)

val LocalAppColors = staticCompositionLocalOf { LightApp }

@Composable
fun SudoFlameTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(primary = Brand.Violet, secondary = Brand.Flame, background = DarkApp.bg, surface = DarkApp.card)
    else lightColorScheme(primary = Brand.Violet, secondary = Brand.Flame, background = LightApp.bg, surface = LightApp.card)
    val base = Typography()
    val typo = base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Black),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        labelLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 0.5.sp),
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalAppColors provides if (dark) DarkApp else LightApp) {
        MaterialTheme(colorScheme = scheme, typography = typo, content = content)
    }
}
