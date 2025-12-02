package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- Light Scheme ---
private val LightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = Color.White,
    primaryContainer = MintLight,
    onPrimaryContainer = MintDark,

    secondary = BlueAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F5FE),
    onSecondaryContainer = BlueDark,

    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,
    onSurface = TextPrimaryLight,

    error = ErrorRed,
    onError = Color.White
)

// --- Dark Scheme ---
private val DarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = Color.White,
    primaryContainer = MintDark,
    onPrimaryContainer = MintLight,

    secondary = BlueAccent,
    onSecondary = Color.White,
    secondaryContainer = BlueDark,
    onSecondaryContainer = Color.White,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,

    error = ErrorRed,
    onError = Color.White
)

@Composable
fun DementiaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
