package com.ambatrakan.sigma.theme

import androidx.compose.ui.graphics.Color

// Electric Blue & Zinc Theme
val BluePrimary = Color(0xFF0055FF) // Electric Blue
val BluePrimaryDark = Color(0xFFA5C8FF) // Pastel Blue for Dark Mode

val BackgroundLight = Color(0xFFF4F7FB) // Very light Zinc
val SurfaceLight = Color(0xFFFFFFFF)
val TextDark = Color(0xFF09090B) // Zinc 950
val TextSecondaryLight = Color(0xFF71717A) // Zinc 500
val BorderLight = Color(0xFFE4E4E7) // Zinc 200

val BackgroundDark = Color(0xFF09090B) // Zinc 950
val SurfaceDark = Color(0xFF18181B) // Zinc 900
val TextLight = Color(0xFFF4F7FB) // Zinc 50
val TextSecondaryDark = Color(0xFFA1A1AA) // Zinc 400
val BorderDark = Color(0xFF27272A) // Zinc 800

// Status Colors
val GreenStatus = Color(0xFF10B981) // Emerald 500
val RedStatus = Color(0xFFEF4444) // Red 500
val YellowStatus = Color(0xFFF59E0B) // Amber 500
val OrangeText = Color(0xFFEA580C) // Orange 600

val PurpleAvatarBg = Color(0xFFE0E7FF)
val PurpleAvatarText = Color(0xFF4338CA)

// --- Playful accent & container palette ---
// Dipakai untuk memberi karakter berbeda per jenis tagihan / status, dan sebagai
// container lembut (background pill/kartu) yang tetap konsisten di light & dark.

// Container status — light (background lembut untuk badge/kartu status)
val GreenContainerLight = Color(0xFFD1FAE5)  // Emerald 100
val RedContainerLight = Color(0xFFFEE2E2)    // Red 100
val YellowContainerLight = Color(0xFFFEF3C7) // Amber 100
val BlueContainerLight = Color(0xFFDBEAFE)   // Blue 100

// Container status — dark (versi gelap yang tetap terbaca)
val GreenContainerDark = Color(0xFF064E3B)   // Emerald 900
val RedContainerDark = Color(0xFF7F1D1D)     // Red 900
val YellowContainerDark = Color(0xFF78350F)  // Amber 900
val BlueContainerDark = Color(0xFF1E3A8A)    // Blue 900

// Aksen playful untuk membedakan jenis tagihan (mis. air vs listrik)
val AccentWater = Color(0xFF06B6D4)   // Cyan 500 — Galon/Air
val AccentEnergy = Color(0xFFF59E0B)  // Amber 500 — Token Listrik
val AccentGeneric = Color(0xFF8B5CF6) // Violet 500 — lainnya

// Secondary/tertiary untuk memperkaya colorScheme (playful tapi selaras dgn biru)
val TealSecondary = Color(0xFF14B8A6)      // Teal 500
val TealSecondaryDark = Color(0xFF5EEAD4)   // Teal 300
val PinkTertiary = Color(0xFFEC4899)        // Pink 500
val PinkTertiaryDark = Color(0xFFF9A8D4)    // Pink 300
