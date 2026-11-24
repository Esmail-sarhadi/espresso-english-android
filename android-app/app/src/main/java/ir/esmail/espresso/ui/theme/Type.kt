package ir.esmail.espresso.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import ir.esmail.espresso.R

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

val Fraunces = FontFamily(
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

fun espressoTypography(scale: Float = 1f): Typography {
    fun TextUnit.scaledBy(): TextUnit = if (this == TextUnit.Unspecified) this else this * scale

    fun TextStyle.scaled() = copy(
        fontSize = fontSize.scaledBy(),
        lineHeight = lineHeight.scaledBy(),
        letterSpacing = letterSpacing.scaledBy(),
    )

    val base = Typography(
        displayLarge = TextStyle(
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            fontSize = 52.sp,
            lineHeight = 56.sp,
        ),
        displayMedium = TextStyle(
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 42.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 36.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 32.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            lineHeight = 28.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 26.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 24.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 26.sp,
            lineHeightStyle = lineHeightStyle,
        ),
        bodyMedium = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5f.sp,
            lineHeight = 23.sp,
            lineHeightStyle = lineHeightStyle,
        ),
        bodySmall = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 19.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5f.sp,
            lineHeight = 16.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = Vazirmatn,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5f.sp,
            lineHeight = 14.sp,
        ),
    )

    return base.copy(
        displayLarge = base.displayLarge.scaled(),
        displayMedium = base.displayMedium.scaled(),
        headlineLarge = base.headlineLarge.scaled(),
        headlineMedium = base.headlineMedium.scaled(),
        headlineSmall = base.headlineSmall.scaled(),
        titleLarge = base.titleLarge.scaled(),
        titleMedium = base.titleMedium.scaled(),
        titleSmall = base.titleSmall.scaled(),
        bodyLarge = base.bodyLarge.scaled(),
        bodyMedium = base.bodyMedium.scaled(),
        bodySmall = base.bodySmall.scaled(),
        labelLarge = base.labelLarge.scaled(),
        labelMedium = base.labelMedium.scaled(),
        labelSmall = base.labelSmall.scaled(),
    )
}

/** English transcript line: slightly larger, generous leading, easy on the eyes. */
val TranscriptStyle = TextStyle(
    fontFamily = Vazirmatn,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = 30.sp,
    lineHeightStyle = lineHeightStyle,
    textDirection = androidx.compose.ui.text.style.TextDirection.Ltr,
    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
)

val TranscriptTranslationStyle = TextStyle(
    fontFamily = Vazirmatn,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 22.sp,
    lineHeightStyle = lineHeightStyle,
)
