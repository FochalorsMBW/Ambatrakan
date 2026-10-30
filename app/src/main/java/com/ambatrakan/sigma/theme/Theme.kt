package com.ambatrakan.sigma.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = TextDark,
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = BluePrimaryDark,
    secondary = TealSecondaryDark,
    onSecondary = TextDark,
    tertiary = PinkTertiaryDark,
    onTertiary = TextDark,
    background = BackgroundDark,
    onBackground = TextLight,
    surface = SurfaceDark,
    onSurface = TextLight,
    surfaceVariant = BorderDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    error = RedStatus,
    onError = TextLight,
    errorContainer = RedContainerDark,
    onErrorContainer = RedContainerLight
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = SurfaceLight,
    primaryContainer = BlueContainerLight,
    onPrimaryContainer = BluePrimary,
    secondary = TealSecondary,
    onSecondary = SurfaceLight,
    tertiary = PinkTertiary,
    onTertiary = SurfaceLight,
    background = BackgroundLight,
    onBackground = TextDark,
    surface = SurfaceLight,
    onSurface = TextDark,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    error = RedStatus,
    onError = SurfaceLight,
    errorContainer = RedContainerLight,
    onErrorContainer = OrangeText
)

// Bentuk yang lebih membulat & playful (sebelumnya flat 2-4dp). Selaras dengan
// RoundedCornerShape inline 16-24dp yang sudah dipakai kartu-kartu di layar.
val AmbatrakanShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AmbatrakanTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Dynamic color is disabled by default to maintain the consistent premium Ambatrakan branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AmbatrakanShapes,
        content = content
    )
}
