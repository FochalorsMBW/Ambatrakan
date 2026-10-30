package com.ambatrakan.sigma.theme

import androidx.compose.ui.unit.dp

/**
 * Konstanta spacing terpusat agar padding/jarak konsisten di seluruh app.
 * Sebelumnya semua nilai dp ditulis inline dan tidak konsisten antar layar.
 *
 * Skala kelipatan 4dp (standar Material):
 *   xs=4  sm=8  md=16  lg=24  xl=32
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
