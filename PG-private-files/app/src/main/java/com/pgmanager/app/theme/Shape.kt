package com.pgmanager.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Standard restrained shape system.
 * Consistent small-radius rounded rectangles (no pill shapes).
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

val ButtonShape = RoundedCornerShape(8.dp)
val CardShape = RoundedCornerShape(10.dp)
val DialogShape = RoundedCornerShape(12.dp)
val InputFieldShape = RoundedCornerShape(8.dp)
val ChipShape = RoundedCornerShape(6.dp)
