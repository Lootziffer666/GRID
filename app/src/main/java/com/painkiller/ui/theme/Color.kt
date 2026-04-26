package com.painkiller.ui.theme

import androidx.compose.ui.graphics.Color

// Color tokens taken directly from CATALON-GUARD as specified in instructions.md.
// Do not reinterpret. Do not add custom colors here in Gate 0.
object PainkillerColors {
    val RauschRed = Color(0xFFFF5A5F)
    val BabuTeal = Color(0xFF00A699)
    val AccentAmber = Color(0xFFF7B731)

    val DarkBackground = Color(0xFF1A1A1A)
    val DarkSurface = Color(0xFF222222)
    val DarkSurfaceVariant = Color(0xFF2E2E2E)

    val LightBackground = Color(0xFFF5F5F5)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFF0F0F0)

    // Foreground tones derived from the spec's intent (calm technical density,
    // readable secondary text). Kept conservative.
    val OnDark = Color(0xFFEDEDED)
    val OnDarkMuted = Color(0xFFB0B0B0)
    val OnLight = Color(0xFF1A1A1A)
    val OnLightMuted = Color(0xFF555555)
}
