package com.icoffee.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val CoffeeShapes = Shapes(
    small = RoundedCornerShape(CoffeeRadius.sm),
    medium = RoundedCornerShape(CoffeeRadius.md),
    large = RoundedCornerShape(CoffeeRadius.lg)
)

val CoffeeSheetShape = RoundedCornerShape(
    topStart = CoffeeRadius.xl,
    topEnd = CoffeeRadius.xl
)
