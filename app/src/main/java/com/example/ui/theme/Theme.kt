package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 60-30-10 Rule Theme Implementation for COMBINE
 */
private val LightColorScheme = lightColorScheme(
    primary = NavyDominant,
    onPrimary = Color.White,
    primaryContainer = NavyLightContainer,
    onPrimaryContainer = NavyDominant,

    secondary = AccentGold,
    onSecondary = OnAccentGold,
    secondaryContainer = AccentGoldLight,
    onSecondaryContainer = OnAccentGold,

    tertiary = NavySecondary,
    onTertiary = Color.White,

    background = CanvasBackground,
    onBackground = TextNavy,

    surface = BentoCardSurface,
    onSurface = TextNavy,
    surfaceVariant = BentoCardSurfaceSubtle,
    onSurfaceVariant = TextMuted,

    outline = BentoCardBorder,
    outlineVariant = BentoCardBorderFocused,

    error = AlertErrorText,
    errorContainer = AlertErrorContainer,
    onErrorContainer = AlertErrorText
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = OnAccentGold,
    primaryContainer = NavySecondary,
    onPrimaryContainer = Color.White,

    secondary = AccentGold,
    onSecondary = OnAccentGold,
    secondaryContainer = Color(0xFF241E15),
    onSecondaryContainer = AccentGold,

    tertiary = NavyLightContainer,
    onTertiary = NavyDark,

    background = Color(0xFF071220),
    onBackground = Color(0xFFF1F5F9),

    surface = Color(0xFF0F1E33),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF142640),
    onSurfaceVariant = Color(0xFF94A3B8),

    outline = Color(0xFF1E3554),
    outlineVariant = Color(0xFF2B4C77),

    error = Color(0xFFF87171),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Explicitly enforce the bespoke 60-30-10 palette rather than dynamic wallpaper colors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
