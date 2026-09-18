package app.fridgedday

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * v2 color tokens.
 *
 * D-023: the light scheme keeps the warm near-white backdrop but drops most of D-022's beige/cream
 * mass. The page is near-white, task surfaces are clean white, forest is the single strong anchor and
 * mint is the only support accent. Warmth is limited to the neutral/background roles; amber and error
 * keep their meaning. D-028 later made the shipping product light-only; the legacy dark token set below
 * remains unused by the shipping theme.
 *
 * Freshness (D-Day) and storage-location semantics keep resolving through Material theme roles
 * (see FreshnessStatus.kt), so screens never hardcode status colors:
 * - SAFE -> surfaceVariant (quiet neutral)
 * - WARNING -> tertiaryContainer (restrained amber)
 * - EXPIRED -> errorContainer (restrained danger)
 */

// Light: warm near-white backdrop that separates from the clean white task surfaces, deep forest
// anchor, restrained mint support
private val WarmBackground = Color(0xFFFAF9F7)
private val WhiteSurface = Color(0xFFFFFFFF)
private val QuietSurface = Color(0xFFF0EEE9)
private val Ink = Color(0xFF1A1917)
private val QuietInk = Color(0xFF6B6862)
private val Hairline = Color(0xFFE7E4DE)
private val Forest = Color(0xFF166534)
private val ForestContainer = Color(0xFFDCFCE7)
private val OnForestContainer = Color(0xFF14532D)
private val FreshInk = Color(0xFF3F6212)
private val FreshAccent = Color(0xFFDCFCE7)
private val OnFreshAccent = Color(0xFF14532D)
private val Amber = Color(0xFF7A5A22)
private val AmberContainer = Color(0xFFF6E7C6)
private val OnAmberContainer = Color(0xFF3F2D08)

// Dark: green-charcoal backdrop, lifted surfaces, soft forest anchor, readable (non-neon) lime
private val NightBackground = Color(0xFF101310)
private val NightSurface = Color(0xFF1C221C)
private val NightSurfaceHigh = Color(0xFF242B24)
private val NightQuietSurface = Color(0xFF2A2F29)
private val NightInk = Color(0xFFF3F5F1)
private val NightQuietInk = Color(0xFFA9B0A7)
private val NightHairline = Color(0xFF2E342D)
private val ForestSoft = Color(0xFFA9D3A4)
private val ForestSoftContainer = Color(0xFF2A4A32)
private val OnForestSoftContainer = Color(0xFFCFE8C9)
private val FreshInkSoft = Color(0xFFC3E27A)
private val FreshAccentSoft = Color(0xFF33421C)
private val OnFreshAccentSoft = Color(0xFFD6ECA1)
private val AmberSoft = Color(0xFFE0C08A)
private val AmberSoftContainer = Color(0xFF4A3A16)
private val OnAmberSoftContainer = Color(0xFFF7E3C0)

/** Explicit v2 light scheme: warm near-white background, clean white task surfaces, deep forest anchor. */
internal val FridgeDDayLightColorScheme: ColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = ForestContainer,
    onPrimaryContainer = OnForestContainer,
    secondary = FreshInk,
    onSecondary = Color.White,
    secondaryContainer = FreshAccent,
    onSecondaryContainer = OnFreshAccent,
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410002),
    background = WarmBackground,
    onBackground = Ink,
    surface = WarmBackground,
    onSurface = Ink,
    surfaceVariant = QuietSurface,
    onSurfaceVariant = QuietInk,
    surfaceContainerLowest = WhiteSurface,
    surfaceContainerLow = WhiteSurface,
    surfaceContainer = Color(0xFFF5F4F1),
    surfaceContainerHigh = Color(0xFFEDEBE6),
    surfaceContainerHighest = Color(0xFFE5E3DD),
    surfaceBright = WhiteSurface,
    surfaceDim = Color(0xFFE8E6E1),
    outline = Color(0xFF8A8880),
    outlineVariant = Hairline,
    inverseSurface = Color(0xFF2C2B27),
    inverseOnSurface = Color(0xFFF5F4F0),
    inversePrimary = ForestSoft,
    scrim = Color.Black
)

/** Legacy pre-D-028 dark token set retained as an unused reference; shipping v2 never selects it. */
internal val FridgeDDayDarkColorScheme: ColorScheme = darkColorScheme(
    primary = ForestSoft,
    onPrimary = Color(0xFF10331A),
    primaryContainer = ForestSoftContainer,
    onPrimaryContainer = OnForestSoftContainer,
    secondary = FreshInkSoft,
    onSecondary = Color(0xFF22300A),
    secondaryContainer = FreshAccentSoft,
    onSecondaryContainer = OnFreshAccentSoft,
    tertiary = AmberSoft,
    onTertiary = Color(0xFF412F0A),
    tertiaryContainer = AmberSoftContainer,
    onTertiaryContainer = OnAmberSoftContainer,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = NightBackground,
    onBackground = NightInk,
    surface = NightBackground,
    onSurface = NightInk,
    surfaceVariant = NightQuietSurface,
    onSurfaceVariant = NightQuietInk,
    surfaceContainerLowest = Color(0xFF0B0D0B),
    surfaceContainerLow = NightSurface,
    surfaceContainer = Color(0xFF1B201B),
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2C332C),
    surfaceBright = Color(0xFF2F352F),
    surfaceDim = Color(0xFF0E110E),
    outline = Color(0xFF7E857D),
    outlineVariant = NightHairline,
    inverseSurface = NightInk,
    inverseOnSurface = Color(0xFF2A2E29),
    inversePrimary = Forest,
    scrim = Color.Black
)
