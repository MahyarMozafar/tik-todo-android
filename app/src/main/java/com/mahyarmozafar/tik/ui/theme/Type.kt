package com.mahyarmozafar.tik.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mahyarmozafar.tik.R

/** The Farsi font, in four weights. English uses the phone's own font. */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

/** Text styles named like the iOS ones, so the screens read the same on both. */
data class TikType(
    val largeTitle: TextStyle,
    val title: TextStyle,
    val title3: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val subheadline: TextStyle,
    val footnote: TextStyle,
    val caption: TextStyle,
    val caption2: TextStyle,
) {
    companion object {
        fun create(farsi: Boolean): TikType {
            val family = if (farsi) Vazirmatn else FontFamily.Default
            fun style(size: Int, weight: FontWeight = FontWeight.Normal, spacing: Float = 0f) = TextStyle(
                fontFamily = family,
                fontSize = size.sp,
                fontWeight = weight,
                letterSpacing = if (farsi) 0.sp else spacing.sp,
            )
            return TikType(
                largeTitle = style(32, FontWeight.Bold, -0.4f),
                title = style(26, FontWeight.Bold, -0.2f),
                title3 = style(19, FontWeight.SemiBold),
                headline = style(16, FontWeight.SemiBold),
                body = style(16),
                callout = style(15),
                subheadline = style(14),
                footnote = style(13),
                caption = style(12),
                caption2 = style(11),
            )
        }
    }
}

/** Material's own styles, so dialogs and menus use the same font. */
fun materialTypography(farsi: Boolean): Typography {
    val base = Typography()
    if (!farsi) return base
    fun TextStyle.farsi() = copy(fontFamily = Vazirmatn, letterSpacing = 0.sp)
    return Typography(
        displayLarge = base.displayLarge.farsi(),
        displayMedium = base.displayMedium.farsi(),
        displaySmall = base.displaySmall.farsi(),
        headlineLarge = base.headlineLarge.farsi(),
        headlineMedium = base.headlineMedium.farsi(),
        headlineSmall = base.headlineSmall.farsi(),
        titleLarge = base.titleLarge.farsi(),
        titleMedium = base.titleMedium.farsi(),
        titleSmall = base.titleSmall.farsi(),
        bodyLarge = base.bodyLarge.farsi(),
        bodyMedium = base.bodyMedium.farsi(),
        bodySmall = base.bodySmall.farsi(),
        labelLarge = base.labelLarge.farsi(),
        labelMedium = base.labelMedium.farsi(),
        labelSmall = base.labelSmall.farsi(),
    )
}
