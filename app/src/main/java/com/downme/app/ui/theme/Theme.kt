package com.downme.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.downme.app.data.AppThemeMode

// Professional blue accent — calm, neutral, and consistent across light and dark.
private val BlueDark = Color(0xFF7AA2FF)
private val BlueLight = Color(0xFF2E5BE6)

private val YellowAccent = Color(0xFFFFD700)

private val DownMeDarkColors =
    darkColorScheme(
        primary = BlueDark,
        onPrimary = Color(0xFF06122E),
        primaryContainer = Color(0xFF26406F),
        onPrimaryContainer = Color(0xFFD7E2FF),
        secondary = Color(0xFF9FB4D8),
        onSecondary = Color(0xFF0B1626),
        background = Color(0xFF0F1116),
        onBackground = Color(0xFFE6E8EC),
        surface = Color(0xFF161922),
        onSurface = Color(0xFFE6E8EC),
        surfaceVariant = Color(0xFF222631),
        onSurfaceVariant = Color(0xFFB4B9C4),
        outline = Color(0xFF333845),
        outlineVariant = Color(0xFF272C36),
        tertiary = Color(0xFF8FD0C4),
        error = Color(0xFFE57373),
    )

private val DownMeLightColors =
    lightColorScheme(
        primary = BlueLight,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCE6FF),
        onPrimaryContainer = Color(0xFF0A2A66),
        secondary = Color(0xFF4A5A75),
        onSecondary = Color.White,
        background = Color(0xFFF6F7F9),
        onBackground = Color(0xFF1A1C20),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1A1C20),
        surfaceVariant = Color(0xFFEDEFF3),
        onSurfaceVariant = Color(0xFF565C66),
        outline = Color(0xFFD3D8E0),
        outlineVariant = Color(0xFFE4E8EE),
        tertiary = Color(0xFF2F8C7C),
        error = Color(0xFFC0392B),
    )

private val DownMeYellowColors =
    darkColorScheme(
        primary = YellowAccent,
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF3D3500),
        onPrimaryContainer = Color(0xFFFFEC8B),
        secondary = YellowAccent,
        onSecondary = Color.Black,
        background = Color(0xFF121212),
        onBackground = Color.White,
        surface = Color(0xFF1E1E1E),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF2A2A2A),
        onSurfaceVariant = Color(0xFFD4D4D4),
        outline = Color(0xFF3A3A3A),
        outlineVariant = Color(0xFF2E2E2E),
        tertiary = Color(0xFFFFEC8B),
        error = Color(0xFFE57373),
    )

@Composable
fun DownMeTheme(
    themeMode: AppThemeMode? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when (themeMode) {
            AppThemeMode.Dark -> DownMeDarkColors
            AppThemeMode.Light -> DownMeLightColors
            AppThemeMode.Yellow -> DownMeYellowColors
            null ->
                if (isSystemInDarkTheme()) {
                    DownMeDarkColors
                } else {
                    DownMeLightColors
                }
        }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
