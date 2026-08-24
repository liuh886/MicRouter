package com.liuh886.microuter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AppPalette {
    val BlueLight = Color(0xFF007AFF)
    val BlueDark = Color(0xFF0A84FF)
    val GreenLight = Color(0xFF34C759)
    val GreenDark = Color(0xFF30D158)
    val RedLight = Color(0xFFFF3B30)
    val RedDark = Color(0xFFFF453A)
    val PurpleLight = Color(0xFFAF52DE)
    val PurpleDark = Color(0xFFBF5AF2)
    val OrangeLight = Color(0xFFFF9500)
    val OrangeDark = Color(0xFFFF9F0A)
    val IndigoLight = Color(0xFF5856D6)
    val IndigoDark = Color(0xFF5E5CE6)

    val GrayLabel = Color(0xFF8E8E93)
    val GrayLabelDark = Color(0xFFA9A9AD)
}

private val LightColors = lightColorScheme(
    primary = AppPalette.BlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EAFF),
    onPrimaryContainer = Color(0xFF003B73),
    secondary = Color(0xFF5856D6),
    tertiary = AppPalette.GreenLight,
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0x29787880),
    onSurfaceVariant = AppPalette.GrayLabel,
    outline = Color(0xFFD6D6DA),
    error = AppPalette.RedLight
)

private val DarkColors = darkColorScheme(
    primary = AppPalette.BlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0B3D66),
    onPrimaryContainer = Color(0xFFCDE6FF),
    secondary = AppPalette.IndigoDark,
    tertiary = AppPalette.GreenDark,
    background = Color.Black,
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0x5C787880),
    onSurfaceVariant = AppPalette.GrayLabelDark,
    outline = Color(0xFF3A3A3C),
    error = AppPalette.RedDark
)

private fun iosType(size: TextUnit, weight: FontWeight, tracking: TextUnit = 0.sp): TextStyle =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = size,
        fontWeight = weight,
        letterSpacing = tracking,
        lineHeight = size * 1.3f
    )

private val AppTypography = Typography(
    titleLarge = iosType(32.sp, FontWeight.Bold, (-0.5).sp),
    titleMedium = iosType(22.sp, FontWeight.SemiBold, (-0.3).sp),
    titleSmall = iosType(16.sp, FontWeight.SemiBold, (-0.2).sp),
    bodyLarge = iosType(17.sp, FontWeight.Normal, (-0.3).sp),
    bodyMedium = iosType(15.sp, FontWeight.Normal),
    bodySmall = iosType(13.sp, FontWeight.Normal),
    labelLarge = iosType(15.sp, FontWeight.Medium),
    labelMedium = iosType(13.sp, FontWeight.Medium),
    labelSmall = iosType(12.sp, FontWeight.Normal)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

val Hairline: Dp = 0.5.dp

@Composable
fun MicRouterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
