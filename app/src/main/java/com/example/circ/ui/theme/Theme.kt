package com.example.circ.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandBackground,
    background = BrandBackground,
    onBackground = TextHighEmphasis,
    surface = BrandSurface,
    onSurface = TextHighEmphasis,
    surfaceVariant = BrandOutline,
    onSurfaceVariant = TextMediumEmphasis,
    outline = BrandOutline,
    secondary = TextMediumEmphasis
)

@Composable
fun CIRCTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}