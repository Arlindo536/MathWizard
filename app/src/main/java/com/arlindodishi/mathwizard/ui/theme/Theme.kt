package com.arlindodishi.mathwizard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.engine.School

/** Design tokens for the pixel-art Endless Spire. */
object SpireColors {
    val Night = Color(0xFF120B24)
    val NightRaised = Color(0xFF170E30)
    val Surface = Color(0xFF1C1338)
    val SurfaceRaised = Color(0xFF2A1B55)
    /** Pixel outline around panels, buttons and bars. */
    val Ink = Color(0xFF2A0F1F)
    /** Empty part of segmented bars. */
    val Track = Color(0xFF3B3466)
    val Outline = Color(0xFF3B3466)
    val Text = Color(0xFFF4EFFF)
    val TextMuted = Color(0xFFB4ACD8)
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
    School.GEOMETRY -> Color(0xFFFFE8A3)
    School.SEQUENCES -> Color(0xFFB388FF)
}

object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
}

/**
 * Jersey 10 (SIL OFL 1.1), bundled in res/font. It has a single weight, registered for every
 * weight so Compose never fakes a bold version. Chosen over Pixelify Sans for clearer B/5.
 */
private val Jersey = Font(R.font.jersey10, FontWeight.Normal)
val PixelFont = FontFamily(
    Jersey,
    Font(R.font.jersey10, FontWeight.Medium),
    Font(R.font.jersey10, FontWeight.SemiBold),
    Font(R.font.jersey10, FontWeight.Bold),
)

/** Hard drop shadow used on display text, like a sprite shadow. */
val PixelShadow = Shadow(color = Color(0xFF05020D), offset = Offset(6f, 6f), blurRadius = 0f)

private val SpireTypography = Typography(
    displayLarge = TextStyle(fontFamily = PixelFont, fontSize = 58.sp, letterSpacing = 1.sp, shadow = PixelShadow),
    displayMedium = TextStyle(fontFamily = PixelFont, fontSize = 48.sp, letterSpacing = 1.sp, shadow = PixelShadow),
    headlineMedium = TextStyle(fontFamily = PixelFont, fontSize = 32.sp, letterSpacing = 1.sp, shadow = PixelShadow),
    titleLarge = TextStyle(fontFamily = PixelFont, fontSize = 25.sp, letterSpacing = 0.5.sp),
    titleMedium = TextStyle(fontFamily = PixelFont, fontSize = 21.sp, letterSpacing = 0.5.sp),
    bodyLarge = TextStyle(fontFamily = PixelFont, fontSize = 21.sp),
    bodyMedium = TextStyle(fontFamily = PixelFont, fontSize = 19.sp),
    labelLarge = TextStyle(fontFamily = PixelFont, fontSize = 20.sp, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontFamily = PixelFont, fontSize = 18.sp),
)

/** Square corners everywhere: pixel art has no rounded edges. */
private val PixelShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

private val SpireColorScheme = darkColorScheme(
    primary = School.ARITHMETIC.accent(),
    onPrimary = SpireColors.Night,
    secondary = SpireColors.Gold,
    background = SpireColors.Night,
    onBackground = SpireColors.Text,
    surface = SpireColors.Surface,
    onSurface = SpireColors.Text,
    surfaceVariant = SpireColors.SurfaceRaised,
    onSurfaceVariant = SpireColors.TextMuted,
    outline = SpireColors.Outline,
    error = SpireColors.Danger,
)

@Composable
fun MathWizardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SpireColorScheme, typography = SpireTypography, shapes = PixelShapes, content = content)
}
