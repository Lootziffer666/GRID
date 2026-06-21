package com.grid.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val GridDarkColors = darkColorScheme(
    primary = GridColors.PrimaryTeal,
    onPrimary = GridColors.OnDark,
    secondary = GridColors.InkBlue,
    onSecondary = GridColors.OnDark,
    tertiary = GridColors.EdgeSoft,
    onTertiary = GridColors.Navy,
    background = GridColors.DarkBackground,
    onBackground = GridColors.OnDark,
    surface = GridColors.DarkSurface,
    onSurface = GridColors.OnDark,
    surfaceVariant = GridColors.DarkSurfaceVariant,
    onSurfaceVariant = GridColors.OnDarkMuted,
    error = GridColors.PrimaryTeal,
    onError = GridColors.OnDark,
)

private val GridLightColors = lightColorScheme(
    primary = GridColors.PrimaryTeal,
    onPrimary = GridColors.LightSurface,
    secondary = GridColors.InkBlue,
    onSecondary = GridColors.LightSurface,
    tertiary = GridColors.EdgeSoft,
    onTertiary = GridColors.OnLight,
    background = GridColors.LightBackground,
    onBackground = GridColors.OnLight,
    surface = GridColors.LightSurface,
    onSurface = GridColors.OnLight,
    surfaceVariant = GridColors.LightSurfaceVariant,
    onSurfaceVariant = GridColors.OnLightMuted,
    error = GridColors.InkBlue,
    onError = GridColors.LightSurface,
)

@Composable
fun GridTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) GridDarkColors else GridLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = GridTypography,
        shapes = GridShapes,
        content = content,
    )
}
