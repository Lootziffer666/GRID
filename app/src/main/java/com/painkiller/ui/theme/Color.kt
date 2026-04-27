package com.painkiller.ui.theme

import androidx.compose.ui.graphics.Color

// Color tokens taken directly from CATALON-GUARD as specified in instructions.md.
// Do not reinterpret. Do not add custom colors here in Gate 0.
object PainkillerColors {
    val PrimaryTeal = Color(0xFF6BCBBC)
    val InkBlue = Color(0xFF1C446D)
    val Navy = Color(0xFF142C5C)
    val CloudWhite = Color(0xFFF4FCFC)
    val EdgeSoft = Color(0xFFE2EEF0)

    val DarkBackground = Color(0xFF0E1E3F)
    val DarkSurface = Color(0xFF142C5C)
    val DarkSurfaceVariant = Color(0xFF1C446D)

    val LightBackground = CloudWhite
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = EdgeSoft

    val OnDark = CloudWhite
    val OnDarkMuted = EdgeSoft
    val OnLight = InkBlue
    val OnLightMuted = Navy
}
