package com.bolao.copa2026.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Palette ──────────────────────────────────────────────────────────────────
private val Emerald700     = Color(0xFF006B3C)
private val Emerald800     = Color(0xFF004D2A)
private val EmeraldLight   = Color(0xFF00934F)
private val EmeraldContainer = Color(0xFFB3E8CC)
private val EmeraldOnContainer = Color(0xFF002111)

private val Gold500        = Color(0xFFFFB700)
private val GoldContainer  = Color(0xFFFFF0C2)
private val GoldOnContainer = Color(0xFF3D2E00)

private val Cobalt700      = Color(0xFF0050B3)
private val CobaltContainer = Color(0xFFD1E4FF)
private val CobaltOnContainer = Color(0xFF001C3D)

private val NeutralSurface = Color(0xFFF6FAF7)
private val White          = Color(0xFFFFFFFF)
private val Black          = Color(0xFF111827)

private val LightColorScheme = lightColorScheme(
    primary             = Emerald700,
    onPrimary           = White,
    primaryContainer    = EmeraldContainer,
    onPrimaryContainer  = EmeraldOnContainer,
    secondary           = Cobalt700,
    onSecondary         = White,
    secondaryContainer  = CobaltContainer,
    onSecondaryContainer = CobaltOnContainer,
    tertiary            = Gold500,
    onTertiary          = Black,
    tertiaryContainer   = GoldContainer,
    onTertiaryContainer = GoldOnContainer,
    background          = NeutralSurface,
    onBackground        = Black,
    surface             = White,
    onSurface           = Black,
    surfaceVariant      = Color(0xFFE8F5EE),
    onSurfaceVariant    = Color(0xFF415B4A),
    outline             = Color(0xFF70916F),
)

private val DarkColorScheme = darkColorScheme(
    primary             = EmeraldLight,
    onPrimary           = Emerald800,
    primaryContainer    = Emerald700,
    onPrimaryContainer  = EmeraldContainer,
    secondary           = Color(0xFFA0C8FF),
    onSecondary         = CobaltOnContainer,
    secondaryContainer  = Color(0xFF003A7D),
    onSecondaryContainer = CobaltContainer,
    tertiary            = Gold500,
    onTertiary          = Black,
    tertiaryContainer   = Color(0xFF574100),
    onTertiaryContainer = GoldContainer,
    background          = Color(0xFF0F1A14),
    surface             = Color(0xFF1A2B1F),
)

// ── Typography ────────────────────────────────────────────────────────────────
private val BolaoTypography = Typography(
    headlineLarge  = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall     = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 11.sp, lineHeight = 16.sp),
)

// ── Theme ─────────────────────────────────────────────────────────────────────
@Composable
fun BolaoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = BolaoTypography,
        content     = content
    )
}
