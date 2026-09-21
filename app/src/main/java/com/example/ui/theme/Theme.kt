package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B2865),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF084B5B),
    onSecondaryContainer = Color(0xFFC7F8FF),
    tertiary = NeonAmber,
    onTertiary = Color.Black,
    background = StudioDarkBg,
    onBackground = TextPrimary,
    surface = StudioCardBg,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = StudioCardBorder,
    error = NeonRose,
    onError = Color.White
)

private val LightColorScheme = DarkColorScheme // Default to dark aesthetic for voice changer studio

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

