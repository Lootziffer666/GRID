package com.painkiller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PainkillerDarkColors = darkColorScheme(
    primary = PainkillerColors.PrimaryTeal,
    onPrimary = PainkillerColors.OnDark,
    secondary = PainkillerColors.InkBlue,
    onSecondary = PainkillerColors.OnDark,
    tertiary = PainkillerColors.EdgeSoft,
    onTertiary = PainkillerColors.Navy,
    background = PainkillerColors.DarkBackground,
    onBackground = PainkillerColors.OnDark,
    surface = PainkillerColors.DarkSurface,
    onSurface = PainkillerColors.OnDark,
    surfaceVariant = PainkillerColors.DarkSurfaceVariant,
    onSurfaceVariant = PainkillerColors.OnDarkMuted,
    error = PainkillerColors.PrimaryTeal,
    onError = PainkillerColors.OnDark,
)

private val PainkillerLightColors = lightColorScheme(
    primary = PainkillerColors.PrimaryTeal,
    onPrimary = PainkillerColors.LightSurface,
    secondary = PainkillerColors.InkBlue,
    onSecondary = PainkillerColors.LightSurface,
    tertiary = PainkillerColors.EdgeSoft,
    onTertiary = PainkillerColors.OnLight,
    background = PainkillerColors.LightBackground,
    onBackground = PainkillerColors.OnLight,
    surface = PainkillerColors.LightSurface,
    onSurface = PainkillerColors.OnLight,
    surfaceVariant = PainkillerColors.LightSurfaceVariant,
    onSurfaceVariant = PainkillerColors.OnLightMuted,
    error = PainkillerColors.InkBlue,
    onError = PainkillerColors.LightSurface,
)

@Composable
fun PainkillerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) PainkillerDarkColors else PainkillerLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = PainkillerTypography,
        shapes = PainkillerShapes,
        content = content,
    )
}
