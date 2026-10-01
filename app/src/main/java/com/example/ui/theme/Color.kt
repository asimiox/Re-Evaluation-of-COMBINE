package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * COMBINE Design System: 60-30-10 Rule
 *
 * 60% DOMINANT: Base canvas / background / neutral card surfaces.
 * 30% SECONDARY: Deep Navy / Midnight Blue for headlines, structure, card outlines, and typography.
 * 10% ACCENT: Warm Gold / Amber-Ochre for CTAs, action buttons, active badges, highlights, and the "C" emblem.
 */

// 60% DOMINANT (Base / Canvas / Neutral Surfaces)
val CanvasBackground = Color(0xFFF4F6F9)        // Clean airy slate canvas
val BentoCardSurface = Color(0xFFFFFFFF)        // Crisp pure white bento card
val BentoCardSurfaceSubtle = Color(0xFFF8FAFC)  // Subtle secondary tile
val BentoCardBorder = Color(0xFFE2E8F0)         // Precision border for bento grid separation
val BentoCardBorderFocused = Color(0xFFCBD5E1)
val TextMuted = Color(0xFF64748B)               // Slate secondary text

// 30% SECONDARY (Deep Navy / Midnight Blue)
val NavyDominant = Color(0xFF0B2545)            // Deep authoritative midnight navy
val NavySecondary = Color(0xFF13315C)           // Rich medium navy
val NavyDark = Color(0xFF07172C)                // Deepest midnight base for splash
val NavySurface = Color(0xFF0F2B48)
val NavyLightContainer = Color(0xFFEEF4FB)      // Soft navy tinted chip
val TextNavy = Color(0xFF0B2545)                // Dominant headline typography

// 10% ACCENT (Warm Gold / Amber / Ochre)
val AccentGold = Color(0xFFD4A346)              // Warm rich gold for CTAs, active highlights
val AccentGoldHover = Color(0xFFC09038)
val AccentGoldLight = Color(0xFFFEF8EC)         // Soft warm gold container
val AccentGoldBorder = Color(0xFFF3D9A4)
val OnAccentGold = Color(0xFF07172C)            // High-contrast deep navy text on gold button

// Status & Semantic Colors
val AlertWarningContainer = Color(0xFFFFF7ED)
val AlertWarningBorder = Color(0xFFFED7AA)
val AlertWarningText = Color(0xFFC2410C)
val AlertWarningGold = Color(0xFFD97706)

val AlertSuccessContainer = Color(0xFFECFDF5)
val AlertSuccessText = Color(0xFF047857)

val AlertErrorContainer = Color(0xFFFEF2F2)
val AlertErrorBorder = Color(0xFFFECACA)
val AlertErrorText = Color(0xFFB91C1C)
