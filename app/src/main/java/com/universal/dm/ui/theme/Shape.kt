package com.universal.dm.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),    // Quality and codec badges/chips
    large = RoundedCornerShape(20.dp),     // Download cards & container blocks
    extraLarge = RoundedCornerShape(28.dp) // Bottom sheets, dialogs & floating sheets
)
