package com.downme.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Yellow = Color(0xFFFFD700)
private val Bg = Color(0xFF121212)
private val Surface = Color(0xFF1E1E1E)

private val DownMeColors =
    darkColorScheme(
        primary = Yellow,
        onPrimary = Color.Black,
        secondary = Yellow,
        onSecondary = Color.Black,
        background = Bg,
        surface = Surface,
        onBackground = Color.White,
        onSurface = Color.White,
        tertiary = Color(0xFFFFEC8B),
    )

@Composable
fun DownMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DownMeColors,
        content = content,
    )
}
