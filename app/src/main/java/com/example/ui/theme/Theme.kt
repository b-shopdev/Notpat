package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CleanMinRed,
    onPrimary = Color.White,
    primaryContainer = SoftRoseBg,
    onPrimaryContainer = CleanMinRed,
    secondary = CleanMinYellow,
    onSecondary = CleanMinRed,
    secondaryContainer = SoftRoseBg,
    onSecondaryContainer = CleanMinRed,
    tertiary = CleanMinGold,
    background = CleanMinBackground,
    onBackground = Color(0xFF1E293B), // Slate-900
    surface = Color.White,
    onSurface = Color(0xFF1E293B),    // Slate-900
    surfaceVariant = SoftRoseBg,
    onSurfaceVariant = CleanMinRed,
    outline = SoftRoseBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = WineRed,
    onPrimary = Color(0xFF5F0004),
    primaryContainer = Color(0xFF81000B),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = YellowGlow,
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = WineSurface,
    onSecondaryContainer = Color(0xFFFFE082),
    tertiary = Color(0xFFFFB74D),
    background = WineDarkBg,
    onBackground = Color(0xFFFFDAD6),
    surface = WineSurface,
    onSurface = Color(0xFFFFDAD6),
    surfaceVariant = Color(0xFF4C2023),
    onSurfaceVariant = Color(0xFFFFCDD2),
    outline = Color(0xFF5F3337)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep bespoke red and yellow styling enforced by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
