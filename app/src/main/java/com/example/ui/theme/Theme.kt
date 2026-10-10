package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = LightBluePill,
    onPrimaryContainer = PrimaryBlue,
    secondary = NavyHeader,
    onSecondary = Color.White,
    background = BackgroundLight,
    surface = SurfaceWhite,
    surfaceVariant = SurfaceCard,
    outline = CellBorderColor,
    onBackground = DarkText,
    onSurface = DarkText,
    error = CoralRed
)

@Composable
fun SmartInvoicingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
