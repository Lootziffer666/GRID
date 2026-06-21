package com.grid.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Shape grammar from instructions.md / CATALON-GUARD: 4dp, 12dp, 24dp.
// Mapping:
//   small  -> chips, badges, compact technical labels
//   medium -> cards, list items, warning/error containers
//   large  -> major panels, empty states, confirmation surfaces
val GridShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
