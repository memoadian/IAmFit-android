package com.geeckosoft.iamfit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MintPrimaryDark,
    onPrimary = Color(0xFF06301F),
    primaryContainer = MintContainerDark,
    onPrimaryContainer = MintContainerLight,
    secondary = CoralAccentDark,
    tertiary = SkyAccent,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceMutedDark,
    outline = OnSurfaceMutedDark,
)

private val LightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = MintContainerLight,
    onPrimaryContainer = Color(0xFF04351F),
    secondary = CoralAccent,
    tertiary = SkyAccent,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceMutedLight,
    outline = OnSurfaceMutedLight,
)

@Composable
fun IamFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
