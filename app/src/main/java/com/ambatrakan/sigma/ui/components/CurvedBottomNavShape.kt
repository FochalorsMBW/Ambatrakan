package com.ambatrakan.sigma.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class CurvedBottomNavShape(
    private val cutoutRadius: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height
            val center = width / 2f
            
            // The depth of the cutout. FAB center is 8dp below the top edge of the surface.
            // So the cutout must go down to 8dp + cutoutRadius.
            val depth = cutoutRadius + with(density) { 8.dp.toPx() }
            
            moveTo(0f, 0f)
            
            // Start of the smooth curve
            val startX = center - cutoutRadius * 1.8f
            lineTo(startX, 0f)
            
            // Left half of the curve (S-curve down to the center)
            cubicTo(
                center - cutoutRadius * 1.2f, 0f, // CP1: stay horizontal at the top
                center - cutoutRadius * 0.8f, depth, // CP2: approach horizontal at the bottom
                center, depth // End: exactly at the bottom center
            )
            
            // Right half of the curve (S-curve up to the surface)
            cubicTo(
                center + cutoutRadius * 0.8f, depth, // CP1: stay horizontal at the bottom
                center + cutoutRadius * 1.2f, 0f, // CP2: approach horizontal at the top
                center + cutoutRadius * 1.8f, 0f // End: back to the surface
            )
            
            lineTo(width, 0f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        return Outline.Generic(path)
    }
}
