package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SunFlowersLightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandSurface,
    primaryContainer = BrandPrimarySoft,
    onPrimaryContainer = BrandPrimaryDark,
    secondary = BrandNavy,
    onSecondary = BrandSurface,
    secondaryContainer = BrandPrimarySoft,
    onSecondaryContainer = BrandNavy,
    tertiary = BrandPrimaryDark,
    onTertiary = BrandSurface,
    background = BrandBackground,
    onBackground = BrandNavy,
    surface = BrandSurface,
    onSurface = BrandNavy,
    surfaceVariant = BrandPrimarySoft,
    onSurfaceVariant = BrandTextMuted,
    outline = BrandBorder,
    outlineVariant = BrandBorder,
    error = BrandDanger,
    onError = BrandSurface,
    errorContainer = BrandDangerSoft,
    onErrorContainer = BrandDanger
)

val SunFlowersShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SunFlowersTheme(
    isTamil: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SunFlowersLightColorScheme,
        typography = buildSunFlowersTypography(isTamil = isTamil),
        shapes = SunFlowersShapes,
        content = content
    )
}
