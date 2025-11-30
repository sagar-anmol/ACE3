package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenColorScheme = lightColorScheme(
    primary = Color(0xFF2ECC71),          // main green
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA3E4D7),
    onPrimaryContainer = Color(0xFF003824),

    secondary = Color(0xFF27AE60),
    onSecondary = Color.White,

    tertiary = Color(0xFFF1C40F),
    onTertiary = Color(0xFF4A3B00),

    background = Color(0xFFE9F7EF),
    onBackground = Color(0xFF083220),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF083220),

    error = Color(0xFFE74C3C),
    onError = Color.White
)

@Composable
fun DementiaAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GreenColorScheme,
        content = content
    )
}
