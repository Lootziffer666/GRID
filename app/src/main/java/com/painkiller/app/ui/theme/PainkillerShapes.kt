package com.painkiller.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Painkiller shape tokens, borrowed from CATALON-GUARD's shape grammar.
 *
 * Mapping (per `instructions.md`):
 * - Small (4.dp)  — chips, badges, compact technical labels
 * - Medium (12.dp) — cards, list items, warning/error containers
 * - Large (24.dp) — major panels, empty states, confirmation surfaces
 */
object PainkillerShapes {
    val SmallCorner = 4.dp
    val MediumCorner = 12.dp
    val LargeCorner = 24.dp

    val Small = RoundedCornerShape(SmallCorner)
    val Medium = RoundedCornerShape(MediumCorner)
    val Large = RoundedCornerShape(LargeCorner)
}

internal val PainkillerMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(PainkillerShapes.SmallCorner),
    small = RoundedCornerShape(PainkillerShapes.SmallCorner),
    medium = RoundedCornerShape(PainkillerShapes.MediumCorner),
    large = RoundedCornerShape(PainkillerShapes.LargeCorner),
    extraLarge = RoundedCornerShape(PainkillerShapes.LargeCorner),
)
