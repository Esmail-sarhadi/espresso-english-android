package ir.esmail.espresso.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import ir.esmail.espresso.data.ThemeMode

val LocalEspresso = staticCompositionLocalOf { DarkPalette }

@Composable
fun EspressoTheme(
    mode: ThemeMode = ThemeMode.DARK,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (dark) DarkPalette else LightPalette
    val typography = espressoTypography(fontScale)

    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.accentInk,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.accent,
            secondary = palette.mint,
            onSecondary = palette.background,
            tertiary = palette.lilac,
            onTertiary = palette.background,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceHigh,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.outline,
            outlineVariant = palette.outline,
            error = palette.danger,
            onError = palette.background,
            scrim = palette.scrim,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.accentInk,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.accent,
            secondary = palette.mint,
            onSecondary = palette.background,
            tertiary = palette.lilac,
            onTertiary = palette.background,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceHigh,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.outline,
            outlineVariant = palette.outline,
            error = palette.danger,
            onError = palette.background,
            scrim = palette.scrim,
        )
    }

    CompositionLocalProvider(
        LocalEspresso provides palette,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            content = content,
        )
    }
}

/** Convenience accessor used across screens. */
val espresso: EspressoPalette
    @Composable get() = LocalEspresso.current

/** English snippets must stay left-to-right inside the RTL layout. */
val LtrTextStyle = TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr)
