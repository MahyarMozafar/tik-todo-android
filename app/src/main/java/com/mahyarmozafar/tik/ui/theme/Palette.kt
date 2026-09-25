package com.mahyarmozafar.tik.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.mahyarmozafar.tik.model.AccentChoice
import com.mahyarmozafar.tik.model.Priority
import com.mahyarmozafar.tik.model.SmartList

/** The same colors as the iOS app, which uses Apple's system colors. Each has a light and a dark version. */
enum class SystemColor(val light: Long, val dark: Long) {
    Blue(0xFF007AFF, 0xFF0A84FF),
    Indigo(0xFF5856D6, 0xFF5E5CE6),
    Purple(0xFFAF52DE, 0xFFBF5AF2),
    Pink(0xFFFF2D55, 0xFFFF375F),
    Red(0xFFFF3B30, 0xFFFF453A),
    Orange(0xFFFF9500, 0xFFFF9F0A),
    Yellow(0xFFFFCC00, 0xFFFFD60A),
    Green(0xFF34C759, 0xFF30D158),
    Mint(0xFF00C7BE, 0xFF63E6E2),
    Teal(0xFF30B0C7, 0xFF40CBE0),
    Cyan(0xFF32ADE6, 0xFF64D2FF),
    Gray(0xFF8E8E93, 0xFF8E8E93);

    fun color(dark: Boolean): Color = Color(if (dark) this.dark else light)
}

val AccentChoice.system: SystemColor
    get() = when (this) {
        AccentChoice.Blue -> SystemColor.Blue
        AccentChoice.Indigo -> SystemColor.Indigo
        AccentChoice.Purple -> SystemColor.Purple
        AccentChoice.Pink -> SystemColor.Pink
        AccentChoice.Red -> SystemColor.Red
        AccentChoice.Orange -> SystemColor.Orange
        AccentChoice.Yellow -> SystemColor.Yellow
        AccentChoice.Green -> SystemColor.Green
        AccentChoice.Mint -> SystemColor.Mint
        AccentChoice.Teal -> SystemColor.Teal
        AccentChoice.Graphite -> SystemColor.Gray
    }

/** A second color that goes well with each accent, for soft backgrounds. */
val AccentChoice.partner: SystemColor
    get() = when (this) {
        AccentChoice.Blue -> SystemColor.Purple
        AccentChoice.Indigo -> SystemColor.Cyan
        AccentChoice.Purple -> SystemColor.Pink
        AccentChoice.Pink -> SystemColor.Orange
        AccentChoice.Red -> SystemColor.Orange
        AccentChoice.Orange -> SystemColor.Pink
        AccentChoice.Yellow -> SystemColor.Orange
        AccentChoice.Green -> SystemColor.Teal
        AccentChoice.Mint -> SystemColor.Blue
        AccentChoice.Teal -> SystemColor.Indigo
        AccentChoice.Graphite -> SystemColor.Blue
    }

fun Priority.color(dark: Boolean): Color? = when (this) {
    Priority.None -> null
    Priority.Low -> SystemColor.Blue.color(dark)
    Priority.Medium -> SystemColor.Orange.color(dark)
    Priority.High -> SystemColor.Red.color(dark)
}

fun SmartList.color(dark: Boolean): Color = when (this) {
    SmartList.Today -> SystemColor.Orange.color(dark)
    SmartList.Scheduled -> SystemColor.Red.color(dark)
    SmartList.All -> SystemColor.Indigo.color(dark)
    SmartList.Completed -> SystemColor.Green.color(dark)
}

/** White or black, whichever reads better on [background]. */
fun contentColorOn(background: Color): Color = if (background.luminance() > 0.6f) Color.Black else Color.White
