package com.pgmanager.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SlateTealPrimaryDark,
    onPrimary = SlateTealOnPrimaryDark,
    primaryContainer = SlateTealPrimaryContainerDark,
    onPrimaryContainer = SlateTealOnPrimaryContainerDark,
    secondary = SlateTealSecondaryDark,
    onSecondary = SlateTealOnSecondaryDark,
    secondaryContainer = SlateTealSecondaryContainerDark,
    onSecondaryContainer = SlateTealOnSecondaryContainerDark,
    tertiary = SlateTealTertiaryDark,
    onTertiary = SlateTealOnTertiaryDark,
    tertiaryContainer = SlateTealTertiaryContainerDark,
    onTertiaryContainer = SlateTealOnTertiaryContainerDark,
    background = NeutralBackgroundDark,
    onBackground = NeutralOnBackgroundDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralOnSurfaceVariantDark,
    outline = NeutralOutlineDark,
    error = StatusError,
    onError = SlateTealOnPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = SlateTealPrimary,
    onPrimary = SlateTealOnPrimary,
    primaryContainer = SlateTealPrimaryContainer,
    onPrimaryContainer = SlateTealOnPrimaryContainer,
    secondary = SlateTealSecondary,
    onSecondary = SlateTealOnSecondary,
    secondaryContainer = SlateTealSecondaryContainer,
    onSecondaryContainer = SlateTealOnSecondaryContainer,
    tertiary = SlateTealTertiary,
    onTertiary = SlateTealOnTertiary,
    tertiaryContainer = SlateTealTertiaryContainer,
    onTertiaryContainer = SlateTealOnTertiaryContainer,
    background = NeutralBackgroundLight,
    onBackground = NeutralOnBackgroundLight,
    surface = NeutralSurfaceLight,
    onSurface = NeutralOnSurfaceLight,
    surfaceVariant = NeutralSurfaceVariantLight,
    onSurfaceVariant = NeutralOnSurfaceVariantLight,
    outline = NeutralOutlineLight,
    error = StatusError,
    onError = SlateTealOnPrimary
)

@Composable
fun PGManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent restrained brand colors by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
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
        shapes = AppShapes,
        content = content
    )
}
