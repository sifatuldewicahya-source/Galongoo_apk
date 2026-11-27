package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = AquaPrimary,
    onPrimary = AquaOnPrimary,
    primaryContainer = AquaPrimaryContainer,
    onPrimaryContainer = AquaOnPrimaryContainer,
    secondary = SkySecondary,
    onSecondary = Color.White,
    secondaryContainer = SkySecondaryContainer,
    onSecondaryContainer = SkyOnSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = AmberOnTertiaryContainer,
    background = WaterMistBackground,
    onBackground = DeepSlateText,
    surface = PureWhiteSurface,
    onSurface = DeepSlateText,
    surfaceVariant = SoftIceSurfaceVariant,
    onSurfaceVariant = MutedSlateText,
    outline = SoftBorderOutline,
    outlineVariant = Color(0xFFE2E8F0),
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAquaPrimary,
    onPrimary = Color(0xFF062040),
    primaryContainer = DarkAquaPrimaryContainer,
    onPrimaryContainer = Color(0xFFD6EAFF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF06283D),
    secondaryContainer = Color(0xFF0E3A5C),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = DarkWaterBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkWaterSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkWaterSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
)

val GalonDekatShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun GalonDekatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = GalonDekatShapes,
        content = content,
    )
}
