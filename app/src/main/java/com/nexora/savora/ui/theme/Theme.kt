package com.nexora.savora.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MonoWhite,
    onPrimary = MonoBlack,
    primaryContainer = MonoGrayLight,
    onPrimaryContainer = MonoBlack,
    secondary = MonoGrayLight,
    onSecondary = MonoBlack,
    tertiary = MonoGray,
    onTertiary = MonoBlack,
    background = BlackBackground,
    onBackground = MonoWhite,
    surface = WhiteGlass,
    onSurface = MonoWhite,
    surfaceVariant = WhiteGlass,
    onSurfaceVariant = MonoGrayLight,
    outline = WhiteGlassBorder,
    outlineVariant = WhiteGlassBorder.copy(alpha = 0.5f),
    error = Color(0xFFFF6B6B),
    onError = MonoBlack
)

private val LightColorScheme = lightColorScheme(
    primary = MonoBlack,
    onPrimary = MonoWhite,
    primaryContainer = MonoGrayDark,
    onPrimaryContainer = MonoWhite,
    secondary = MonoGrayDark,
    onSecondary = MonoWhite,
    tertiary = MonoGray,
    onTertiary = MonoWhite,
    background = WhiteBackground,
    onBackground = MonoBlack,
    surface = BlackGlass,
    onSurface = MonoBlack,
    surfaceVariant = BlackGlass,
    onSurfaceVariant = MonoGrayDark,
    outline = BlackGlassBorder,
    outlineVariant = BlackGlassBorder.copy(alpha = 0.5f)
)

@Composable
fun SavoraTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}