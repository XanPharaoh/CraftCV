package com.craftcv.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

object CraftColors {
    // Base
    val Background     @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.Background else LightColors.Background
    val Surface        @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.Surface else LightColors.Surface
    val SurfaceVariant @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.SurfaceVariant else LightColors.SurfaceVariant

    // Ink (text)
    val InkPrimary   @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.InkPrimary else LightColors.InkPrimary
    val InkSecondary @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.InkSecondary else LightColors.InkSecondary
    val InkTertiary  @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.InkTertiary else LightColors.InkTertiary

    // Accent
    val Accent     @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.Accent else LightColors.Accent
    val AccentSoft @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.AccentSoft else LightColors.AccentSoft
    val AccentDark   @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.AccentDark else LightColors.AccentDark
    val AccentBorder @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.AccentBorder else LightColors.AccentBorder

    // Semantic
    val Success     @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.Success else LightColors.Success
    val SuccessSoft @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.SuccessSoft else LightColors.SuccessSoft
    val Warning     @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.Warning else LightColors.Warning
    val WarningSoft @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.WarningSoft else LightColors.WarningSoft
    val Error       @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.Error else LightColors.Error
    val ErrorSoft   @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.ErrorSoft else LightColors.ErrorSoft

    // Borders
    val Border       @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.Border else LightColors.Border
    val BorderStrong @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.BorderStrong else LightColors.BorderStrong

    // Pro
    val ProGold     @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.ProGold else LightColors.ProGold
    val ProGoldSoft @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) DarkColors.ProGoldSoft else LightColors.ProGoldSoft
}

// ── Light palette (same as original) ──
object LightColors {
    val Background     = Color(0xFFFAFAF8)
    val Surface        = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F1EF)

    val InkPrimary   = Color(0xFF1A1915)
    val InkSecondary = Color(0xFF706F6C)
    val InkTertiary  = Color(0xFFAFADAA)

    val Accent     = Color(0xFF2F6BFF)
    val AccentSoft = Color(0xFFEBF0FF)
    val AccentDark   = Color(0xFF1A4FD6)
    val AccentBorder = Color(0xFFBFD0FF)

    val Success     = Color(0xFF1A8A5A)
    val SuccessSoft = Color(0xFFE6F5EE)
    val Warning     = Color(0xFFB85C00)
    val WarningSoft = Color(0xFFFFF0E0)
    val Error       = Color(0xFFCC3333)
    val ErrorSoft   = Color(0xFFFFEBEB)

    val Border       = Color(0xFFE8E8E5)
    val BorderStrong = Color(0xFFCCCCC8)

    val ProGold     = Color(0xFFD4A017)
    val ProGoldSoft = Color(0xFFFFF8E7)
}

// ── Dark palette ──
object DarkColors {
    val Background     = Color(0xFF121212)
    val Surface        = Color(0xFF1E1E1E)
    val SurfaceVariant = Color(0xFF2A2A2A)

    val InkPrimary   = Color(0xFFE8E6E3)
    val InkSecondary = Color(0xFFA5A3A0)
    val InkTertiary  = Color(0xFF6B6966)

    val Accent     = Color(0xFF5B8FFF)
    val AccentSoft = Color(0xFF1A2A4D)
    val AccentDark   = Color(0xFF3D6FE8)
    val AccentBorder = Color(0xFF2A3A5D)

    val Success     = Color(0xFF2EBD7F)
    val SuccessSoft = Color(0xFF0F2A1F)
    val Warning     = Color(0xFFE69500)
    val WarningSoft = Color(0xFF2A1E0A)
    val Error       = Color(0xFFEF5350)
    val ErrorSoft   = Color(0xFF2A1212)

    val Border       = Color(0xFF3A3A3A)
    val BorderStrong = Color(0xFF505050)

    val ProGold     = Color(0xFFE8B830)
    val ProGoldSoft = Color(0xFF2A2410)
}
