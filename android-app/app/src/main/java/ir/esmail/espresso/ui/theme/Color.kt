package ir.esmail.espresso.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * "Midnight espresso" — a deep violet-charcoal canvas lit by warm amber, with teal, violet,
 * coral and sky used as accents. The dark scheme is the signature look; the light scheme is the
 * same family in daylight (warm cream paper, espresso ink).
 *
 * Nothing here is a flat single-hue ramp: every screen mixes one warm accent with one cool
 * accent so the UI never turns into a wall of brown.
 */

val Crema = Color(0xFFFFB03A)
val CremaDeep = Color(0xFFE08A1E)
val Mint = Color(0xFF4FE0BE)
val Lilac = Color(0xFFB49BFF)
val Coral = Color(0xFFFF8A6B)
val Sky = Color(0xFF6FB8FF)

@Immutable
data class EspressoPalette(
    val isDark: Boolean,
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val mint: Color,
    val lilac: Color,
    val coral: Color,
    val sky: Color,
    val danger: Color,
    val scrim: Color,
) {
    /** Warm sweep for hero cards and primary buttons. */
    val warmGradient: List<Color> get() = listOf(accent, coral)

    /** Cool sweep for secondary highlights and progress rails. */
    val coolGradient: List<Color> get() = listOf(lilac, sky)

    /** Full spectrum used behind the app mark and onboarding art. */
    val auroraGradient: List<Color> get() = listOf(lilac, sky, mint)
}

val DarkPalette = EspressoPalette(
    isDark = true,
    background = Color(0xFF0C0A10),
    backgroundAlt = Color(0xFF120F18),
    surface = Color(0xFF191521),
    surfaceHigh = Color(0xFF221C2C),
    outline = Color(0xFF322A3E),
    textPrimary = Color(0xFFF8F2EC),
    textSecondary = Color(0xFFBCB0C8),
    textTertiary = Color(0xFF837793),
    accent = Color(0xFFFFB03A),
    accentInk = Color(0xFF241604),
    accentSoft = Color(0x33FFB03A),
    mint = Color(0xFF4FE0BE),
    lilac = Color(0xFFB49BFF),
    coral = Color(0xFFFF8A6B),
    sky = Color(0xFF6FB8FF),
    danger = Color(0xFFFF6B5E),
    scrim = Color(0xE60C0A10),
)

val LightPalette = EspressoPalette(
    isDark = false,
    background = Color(0xFFFDFAF6),
    backgroundAlt = Color(0xFFF5EEE6),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFF6EFE7),
    outline = Color(0xFFE7DACB),
    textPrimary = Color(0xFF1A141F),
    textSecondary = Color(0xFF5F5468),
    textTertiary = Color(0xFF998DA4),
    accent = Color(0xFFB4661A),
    accentInk = Color(0xFFFFF9F0),
    accentSoft = Color(0x1FB4661A),
    mint = Color(0xFF0E9B80),
    lilac = Color(0xFF6A4FD6),
    coral = Color(0xFFD9563C),
    sky = Color(0xFF2F7FD0),
    danger = Color(0xFFC0392B),
    scrim = Color(0xE6FDFAF6),
)

/** Cover palette: one vivid hue per lesson, used by the generated artwork and list accents. */
val CoverPalette = listOf(
    Color(0xFFFFB03A),
    Color(0xFF4FE0BE),
    Color(0xFFFF8A6B),
    Color(0xFFB49BFF),
    Color(0xFFF5D06F),
    Color(0xFF6FB8FF),
    Color(0xFFFF9EC4),
    Color(0xFF7EDC8F),
    Color(0xFFFFC46B),
    Color(0xFF8FA8FF),
)

fun coverAccent(episodeId: String): Color {
    var hash = 0
    for (ch in episodeId) {
        hash = (hash * 31 + ch.code) and 0x7FFFFFFF
    }
    return CoverPalette[hash % CoverPalette.size]
}

/** Soft vertical wash used behind hero sections so large surfaces are never dead flat. */
fun heroWash(palette: EspressoPalette): Brush = Brush.verticalGradient(
    listOf(palette.accentSoft, Color.Transparent),
)
