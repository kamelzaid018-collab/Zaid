package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudioColorScheme = darkColorScheme(
    primary = StudioAccentBlue,
    onPrimary = StudioDarkBg,
    primaryContainer = StudioDarkCard,
    onPrimaryContainer = StudioTextPrimary,
    secondary = StudioAccentPurple,
    onSecondary = StudioDarkBg,
    secondaryContainer = StudioDarkSurface,
    onSecondaryContainer = StudioTextPrimary,
    tertiary = StudioAccentGreen,
    background = StudioDarkBg,
    onBackground = StudioTextPrimary,
    surface = StudioDarkSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioDarkCard,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioDarkBorder,
    error = StudioAccentRed,
    onError = StudioDarkBg
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        typography = Typography,
        content = content
    )
}
