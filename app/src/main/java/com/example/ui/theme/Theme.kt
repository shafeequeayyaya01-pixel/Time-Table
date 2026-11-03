package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = WafyEmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = WafyEmeraldLight,
    onPrimaryContainer = WafyEmeraldDeep,
    secondary = ScholarlyAmber,
    onSecondary = Color.White,
    secondaryContainer = ScholarlyAmberLight,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = WafiyyaTealPrimary,
    onTertiary = Color.White,
    tertiaryContainer = WafiyyaTealLight,
    onTertiaryContainer = WafiyyaTealDeep,
    background = AlabasterBackground,
    onBackground = SlateInk,
    surface = AlabasterSurface,
    onSurface = SlateInk,
    surfaceVariant = AlabasterCardTint,
    onSurfaceVariant = SlateMuted,
    outline = SlateBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLightAccent,
    onPrimary = DarkBackground,
    primaryContainer = WafyEmeraldDeep,
    onPrimaryContainer = WafyEmeraldLight,
    secondary = AmberLightAccent,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = ScholarlyAmberLight,
    tertiary = Color(0xFF2DD4BF),
    onTertiary = DarkBackground,
    tertiaryContainer = WafiyyaTealDeep,
    onTertiaryContainer = WafiyyaTealLight,
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF1E463B)
)

@Composable
fun WafyPortalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
