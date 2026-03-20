package com.future.ultimate.core.common.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FlotaThemeMode {
    Light,
    Dark,
    Pink,
}

private val FlotaLightColors = lightColorScheme(
    primary = Color(0xFF128C7E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6F5EA),
    onPrimaryContainer = Color(0xFF032E29),
    secondary = Color(0xFF25D366),
    onSecondary = Color(0xFF032B13),
    secondaryContainer = Color(0xFFD8FADB),
    onSecondaryContainer = Color(0xFF0C2F12),
    tertiary = Color(0xFF34B7F1),
    onTertiary = Color(0xFF002B3A),
    tertiaryContainer = Color(0xFFD4F3FF),
    onTertiaryContainer = Color(0xFF003545),
    background = Color(0xFFF4FBF8),
    onBackground = Color(0xFF111B21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111B21),
    surfaceVariant = Color(0xFFE8F2EE),
    onSurfaceVariant = Color(0xFF51656F),
    outline = Color(0xFFB7CBC3),
    outlineVariant = Color(0xFFD8E6E0),
)

private val FlotaDarkColors = darkColorScheme(
    primary = Color(0xFF25D366),
    onPrimary = Color(0xFF062A14),
    primaryContainer = Color(0xFF0E5C42),
    onPrimaryContainer = Color(0xFFD6F5EA),
    secondary = Color(0xFF64E89A),
    onSecondary = Color(0xFF0C2F12),
    secondaryContainer = Color(0xFF124B28),
    onSecondaryContainer = Color(0xFFD8FADB),
    tertiary = Color(0xFF7ED9FF),
    onTertiary = Color(0xFF003547),
    tertiaryContainer = Color(0xFF004D64),
    onTertiaryContainer = Color(0xFFD4F3FF),
    background = Color(0xFF0B141A),
    onBackground = Color(0xFFE9F1EE),
    surface = Color(0xFF111B21),
    onSurface = Color(0xFFE9F1EE),
    surfaceVariant = Color(0xFF1F2C34),
    onSurfaceVariant = Color(0xFFB6C5CB),
    outline = Color(0xFF44606A),
    outlineVariant = Color(0xFF25363E),
)

private val FlotaPinkColors = darkColorScheme(
    primary = Color(0xFFFF4FD8),
    onPrimary = Color(0xFF260018),
    primaryContainer = Color(0xFF5B1146),
    onPrimaryContainer = Color(0xFFFFD7F3),
    secondary = Color(0xFFFF82F9),
    onSecondary = Color(0xFF32002F),
    secondaryContainer = Color(0xFF69005F),
    onSecondaryContainer = Color(0xFFFFD6FA),
    tertiary = Color(0xFF62F7FF),
    onTertiary = Color(0xFF00363A),
    tertiaryContainer = Color(0xFF00565D),
    onTertiaryContainer = Color(0xFFB6F9FF),
    background = Color(0xFF140014),
    onBackground = Color(0xFFFFEAF7),
    surface = Color(0xFF220022),
    onSurface = Color(0xFFFFEAF7),
    surfaceVariant = Color(0xFF3A1239),
    onSurfaceVariant = Color(0xFFF6B7E9),
    outline = Color(0xFFB86BAA),
    outlineVariant = Color(0xFF64345F),
)

private val FlotaTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
)

private val FlotaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
)

@Composable
fun FlotaTheme(
    mode: FlotaThemeMode = if (isSystemInDarkTheme()) FlotaThemeMode.Dark else FlotaThemeMode.Light,
    content: @Composable () -> Unit,
) {
    val colors = when (mode) {
        FlotaThemeMode.Light -> FlotaLightColors
        FlotaThemeMode.Dark -> FlotaDarkColors
        FlotaThemeMode.Pink -> FlotaPinkColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = FlotaTypography,
        shapes = FlotaShapes,
        content = content,
    )
}

object FlotaThemeDefaults {
    val screenShape = RoundedCornerShape(30.dp)
    val cardShape = RoundedCornerShape(26.dp)
    val pillShape = RoundedCornerShape(999.dp)

    @Composable
    fun elevatedCardColors(): CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

    @Composable
    fun mutedCardColors(): CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun ColorScheme.topBarContainerColor(): Color = surfaceVariant.copy(alpha = 0.96f)
