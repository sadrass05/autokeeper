package com.example.autobookkeeper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = Color.White,
    background = DarkBackground,
    onBackground = Color(0xFFE0E0E0),
    surface = DarkSurface,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFAAAAAA),
    error = ErrorRed,
    onError = OnErrorColor,
    tertiary = ProfitGreen,
    onTertiary = OnTertiaryColor,
    outline = Color(0xFF444444),
    outlineVariant = Color(0xFF333333)
)

private val LightColorScheme = lightColorScheme(
    primary = AmberGold,
    onPrimary = Color.White,
    background = WarmBackground,
    onBackground = Color(0xFF1A1A1A),
    surface = WhiteSurface,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = Color(0xFF666666),
    error = ErrorRed,
    onError = OnErrorColor,
    tertiary = ProfitGreen,
    onTertiary = OnTertiaryColor,
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0)
)

@Composable
fun AutoBookkeeperTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}