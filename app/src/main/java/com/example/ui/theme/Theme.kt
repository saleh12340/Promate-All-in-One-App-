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
    onPrimaryContainer = NavyHeader,
    secondary = NavyHeader,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EDF5),
    onSecondaryContainer = NavyHeader,
    background = BackgroundLight,
    surface = SurfaceWhite,
    surfaceVariant = SurfaceCard,
    outline = CellBorderColor,
    outlineVariant = Color(0xFFDCE4EE),
    onBackground = DarkText,
    onSurface = DarkText,
    onSurfaceVariant = MutedText,
    error = CoralRed,
    errorContainer = Color(0xFFFFE9EC),
    onErrorContainer = Color(0xFF8B1824)
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
