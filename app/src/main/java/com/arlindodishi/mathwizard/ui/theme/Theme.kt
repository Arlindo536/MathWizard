package com.arlindodishi.mathwizard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arlindodishi.mathwizard.engine.School

/** Design tokens for the Endless Spire. Fonts are placeholders until the visual pass (step 7). */
object SpireColors {
    val Night = Color(0xFF0B0A1A)
    val NightRaised = Color(0xFF16142B)
    val Surface = Color(0xFF1F1C3A)
    val Outline = Color(0xFF34305C)
    val Text = Color(0xFFF1EEFF)
    val TextMuted = Color(0xFFA9A3C9)
    val PlayerHp = Color(0xFF5EEAD4)
    val EnemyHp = Color(0xFFFF4D6D)
    val Danger = Color(0xFFFF4D6D)
    val Success = Color(0xFF4ADE80)
    val Gold = Color(0xFFFFD166)
    val Crystal = Color(0xFF7DD3FC)
}

fun School.accent(): Color = when (this) {
    School.ARITHMETIC -> Color(0xFFFF7A3D)
    School.FRACTIONS -> Color(0xFF4CC9F0)
    School.ALGEBRA -> Color(0xFFFFE066)
    School.POWERS -> Color(0xFF8BC34A)
    School.GEOMETRY -> Color(0xFFFFF4C2)
    School.SEQUENCES -> Color(0xFFB388FF)
}

object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
}

private val DisplayFamily = FontFamily.Serif

private val SpireTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = 1.sp),
    displayMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
)

private val SpireColorScheme = darkColorScheme(
    primary = School.ARITHMETIC.accent(),
    onPrimary = SpireColors.Night,
    secondary = SpireColors.Gold,
    background = SpireColors.Night,
    onBackground = SpireColors.Text,
    surface = SpireColors.Surface,
    onSurface = SpireColors.Text,
    surfaceVariant = SpireColors.NightRaised,
    onSurfaceVariant = SpireColors.TextMuted,
    outline = SpireColors.Outline,
    error = SpireColors.Danger,
)

@Composable
fun MathWizardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SpireColorScheme, typography = SpireTypography, content = content)
}
