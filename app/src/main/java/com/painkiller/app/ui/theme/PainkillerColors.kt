package com.painkiller.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Painkiller color tokens.
 *
 * Borrowed verbatim from CATALON-GUARD's color grammar so Painkiller
 * feels like a sibling tool. See `instructions.md` § "CATALON-GUARD
 * Color Tokens".
 */
object PainkillerColors {
    // Brand accents.
    val RauschRed = Color(0xFFFF5A5F)
    val BabuTeal = Color(0xFF00A699)
    val AccentAmber = Color(0xFFF7B731)

    // Dark surfaces.
    val DarkBackground = Color(0xFF1A1A1A)
    val DarkSurface = Color(0xFF222222)
    val DarkSurfaceVariant = Color(0xFF2E2E2E)

    // Light surfaces.
    val LightBackground = Color(0xFFF5F5F5)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFF0F0F0)

    // Text contrasts. Kept here so previews and tests can reference them.
    val OnDark = Color(0xFFF2F2F2)
    val OnDarkMuted = Color(0xFFB8B8B8)
    val OnLight = Color(0xFF1A1A1A)
    val OnLightMuted = Color(0xFF555555)

    // Severity tints. Used by PainkillerSeverityBadge and the severity
    // surfaces. Each maps to one of the Painkiller diagnosis severities.
    val SeveritySafe = BabuTeal
    val SeverityWarning = AccentAmber
    val SeverityBlocked = RauschRed
    val SeverityDeferred = Color(0xFF8C8C8C)
}
