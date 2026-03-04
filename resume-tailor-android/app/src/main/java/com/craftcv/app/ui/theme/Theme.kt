package com.craftcv.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CraftShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val CraftLightColorScheme = lightColorScheme(
    background     = LightColors.Background,
    surface        = LightColors.Surface,
    surfaceVariant = LightColors.SurfaceVariant,
    primary        = LightColors.Accent,
    onPrimary      = Color.White,
    onBackground   = LightColors.InkPrimary,
    onSurface      = LightColors.InkPrimary,
    outline        = LightColors.Border,
)

private val CraftDarkColorScheme = darkColorScheme(
    background     = DarkColors.Background,
    surface        = DarkColors.Surface,
    surfaceVariant = DarkColors.SurfaceVariant,
    primary        = DarkColors.Accent,
    onPrimary      = Color.Black,
    onBackground   = DarkColors.InkPrimary,
    onSurface      = DarkColors.InkPrimary,
    outline        = DarkColors.Border,
)

@Composable
fun CraftCVTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val colorScheme = when (themeMode) {
        "light" -> CraftLightColorScheme
        "dark"  -> CraftDarkColorScheme
        else    -> if (isSystemInDarkTheme()) CraftDarkColorScheme else CraftLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = CraftTypography,
        shapes      = CraftShapes,
        content     = content,
    )
}
