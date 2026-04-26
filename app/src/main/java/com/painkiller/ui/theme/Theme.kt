package com.painkiller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PainkillerDarkColors = darkColorScheme(
    primary = PainkillerColors.RauschRed,
    onPrimary = PainkillerColors.OnDark,
    secondary = PainkillerColors.BabuTeal,
    onSecondary = PainkillerColors.OnDark,
    tertiary = PainkillerColors.AccentAmber,
    onTertiary = PainkillerColors.DarkBackground,
    background = PainkillerColors.DarkBackground,
    onBackground = PainkillerColors.OnDark,
    surface = PainkillerColors.DarkSurface,
    onSurface = PainkillerColors.OnDark,
    surfaceVariant = PainkillerColors.DarkSurfaceVariant,
    onSurfaceVariant = PainkillerColors.OnDarkMuted,
    error = PainkillerColors.RauschRed,
    onError = PainkillerColors.OnDark,
)

private val PainkillerLightColors = lightColorScheme(
    primary = PainkillerColors.RauschRed,
    onPrimary = PainkillerColors.LightSurface,
    secondary = PainkillerColors.BabuTeal,
    onSecondary = PainkillerColors.LightSurface,
    tertiary = PainkillerColors.AccentAmber,
    onTertiary = PainkillerColors.OnLight,
    background = PainkillerColors.LightBackground,
    onBackground = PainkillerColors.OnLight,
    surface = PainkillerColors.LightSurface,
    onSurface = PainkillerColors.OnLight,
    surfaceVariant = PainkillerColors.LightSurfaceVariant,
    onSurfaceVariant = PainkillerColors.OnLightMuted,
    error = PainkillerColors.RauschRed,
    onError = PainkillerColors.LightSurface,
)

@Composable
fun PainkillerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
