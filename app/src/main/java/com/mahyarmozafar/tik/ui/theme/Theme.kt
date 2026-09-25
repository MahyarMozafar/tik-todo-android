package com.mahyarmozafar.tik.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import com.mahyarmozafar.tik.model.AccentChoice
import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.model.AppTheme
import com.mahyarmozafar.tik.model.TikSettings
import com.mahyarmozafar.tik.time.DateFormatting

/** Colors that Material's scheme has no name for: the glass, the cards and the accent's partner. */
@Immutable
data class TikColors(
    val dark: Boolean,
    val accent: Color,
    val onAccent: Color,
    val partner: Color,
    val background: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    /** A soft fill for small controls, like iOS `.fill.tertiary`. */
    val fill: Color,
    val separator: Color,
    /** The see-through cards that tasks sit on. */
    val card: Color,
    val cardBorderTop: Color,
    val cardBorderBottom: Color,
    /** The tint laid over real glass (bars and floating buttons). */
    val glassTint: Color,
    /** Glass on phones that can't blur (before Android 12). */
    val glassFallback: Color,
    /** Menus and dialogs float in their own window, where nothing can be blurred, so they are solid. */
    val popup: Color,
    val glassRim: Color,
    val destructive: Color,
    val success: Color,
    val warning: Color,
)

object TikTheme {
    val colors: TikColors
        @Composable @ReadOnlyComposable get() = LocalTikColors.current

    val type: TikType
        @Composable @ReadOnlyComposable get() = LocalTikType.current

    val settings: TikSettings
        @Composable @ReadOnlyComposable get() = LocalSettings.current

    val formatting: DateFormatting
        @Composable @ReadOnlyComposable get() = LocalFormatting.current
}

val LocalTikColors = staticCompositionLocalOf<TikColors> { error("TikTheme is missing") }
val LocalTikType = staticCompositionLocalOf { TikType.create(farsi = false) }
val LocalSettings = staticCompositionLocalOf { TikSettings() }
val LocalFormatting = staticCompositionLocalOf { TikSettings().formatting() }

val dynamicColorsAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun isDarkTheme(theme: AppTheme): Boolean = when (theme) {
    AppTheme.System -> isSystemInDarkTheme()
    AppTheme.Light -> false
    AppTheme.Dark -> true
}

@Composable
fun TikTheme(settings: TikSettings, content: @Composable () -> Unit) {
    val dark = isDarkTheme(settings.theme)
    val context = LocalContext.current
    val useWallpaper = settings.wallpaperColors && dynamicColorsAvailable

    val scheme = remember(dark, settings.accent, useWallpaper) {
        if (useWallpaper) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            tikColorScheme(settings.accent, dark)
        }
    }
    val colors = remember(scheme, dark, settings.accent, useWallpaper) {
        tikColors(scheme, dark, settings.accent, useWallpaper)
    }
    val farsi = settings.language == AppLanguage.Farsi
    val type = remember(farsi) { TikType.create(farsi) }
    val typography = remember(farsi) { materialTypography(farsi) }
    val formatting = remember(settings.language, settings.calendar, settings.use24Hour) { settings.formatting() }

    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(
            LocalTikColors provides colors,
            LocalTikType provides type,
            LocalSettings provides settings,
            LocalFormatting provides formatting,
            content = content,
        )
    }
}

/** Uses [color] as the accent for part of the app, like a list's own color inside that list. */
@Composable
fun ProvideAccent(color: Color, content: @Composable () -> Unit) {
    val colors = TikTheme.colors
    val onAccent = contentColorOn(color)
    val scheme = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalTikColors provides colors.copy(accent = color, onAccent = onAccent)) {
        MaterialTheme(
            colorScheme = scheme.copy(primary = color, onPrimary = onAccent),
            typography = MaterialTheme.typography,
            content = content,
        )
    }
}

private fun tikColorScheme(accent: AccentChoice, dark: Boolean): ColorScheme {
    val primary = accent.system.color(dark)
    val onPrimary = contentColorOn(primary)
    val partner = accent.partner.color(dark)
    return if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.28f).compositeOver(Color(0xFF1C1C1E)),
            onPrimaryContainer = Color.White,
            secondary = partner,
            onSecondary = contentColorOn(partner),
            tertiary = partner,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color(0xFF1C1C1E),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2C2C2E),
            onSurfaceVariant = Color(0x99EBEBF5),
            surfaceContainerLowest = Color(0xFF0C0C0D),
            surfaceContainerLow = Color(0xFF161618),
            surfaceContainer = Color(0xFF1C1C1E),
            surfaceContainerHigh = Color(0xFF2C2C2E),
            surfaceContainerHighest = Color(0xFF3A3A3C),
            outline = Color(0xFF545458),
            outlineVariant = Color(0xFF38383A),
            error = SystemColor.Red.color(true),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.16f).compositeOver(Color.White),
            onPrimaryContainer = Color.Black,
            secondary = partner,
            onSecondary = contentColorOn(partner),
            tertiary = partner,
            background = Color(0xFFF2F2F7),
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color(0xFFE5E5EA),
            onSurfaceVariant = Color(0x993C3C43),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF9F9FB),
            surfaceContainer = Color(0xFFF2F2F7),
            surfaceContainerHigh = Color(0xFFEBEBF0),
            surfaceContainerHighest = Color(0xFFE5E5EA),
            outline = Color(0xFFC6C6C8),
            outlineVariant = Color(0xFFE5E5EA),
            error = SystemColor.Red.color(false),
        )
    }
}

private fun tikColors(scheme: ColorScheme, dark: Boolean, accent: AccentChoice, wallpaper: Boolean): TikColors {
    val accentColor = scheme.primary
    val partner = if (wallpaper) scheme.tertiary else accent.partner.color(dark)
    val primaryText = if (dark) Color.White else Color.Black
    return TikColors(
        dark = dark,
        accent = accentColor,
        onAccent = scheme.onPrimary,
        partner = partner,
        background = scheme.background,
        primaryText = primaryText,
        secondaryText = if (dark) Color(0x99EBEBF5) else Color(0x993C3C43),
        tertiaryText = if (dark) Color(0x4DEBEBF5) else Color(0x4D3C3C43),
        fill = if (dark) Color(0x3D767680) else Color(0x1F767680),
        separator = if (dark) Color(0x5C545458) else Color(0x4A3C3C43),
        card = if (dark) Color.White.copy(alpha = 0.075f) else Color.White.copy(alpha = 0.72f),
        cardBorderTop = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.95f),
        cardBorderBottom = if (dark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.35f),
        glassTint = if (dark) Color(0xFF16161A).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.38f),
        glassFallback = if (dark) Color(0xFF2C2C2E).copy(alpha = 0.94f) else Color(0xFFFBFBFD).copy(alpha = 0.94f),
        popup = if (dark) Color(0xFF2C2C2E) else Color(0xFFFCFCFD),
        glassRim = if (dark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.9f),
        destructive = SystemColor.Red.color(dark),
        success = SystemColor.Green.color(dark),
        warning = SystemColor.Orange.color(dark),
    )
}
