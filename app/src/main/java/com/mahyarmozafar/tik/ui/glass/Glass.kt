package com.mahyarmozafar.tik.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.ui.theme.TikTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

/** Blur works from Android 12. Bent light at the glass edges (the "water drop") needs Android 13. */
val canBlur: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val canBendLight: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

val CapsuleShape = RoundedCornerShape(percent = 50)

/**
 * What the glass on a screen shows through: the screen's scrolling content and background, which
 * are marked with `hazeSource`. Each screen makes its own.
 */
val LocalBackdrop = staticCompositionLocalOf<HazeState> { error("No backdrop") }

/**
 * Real glass: what scrolls behind it is blurred, and on Android 13+ bent at the edges like water.
 * [backdrop] is the content behind (marked with `hazeSource`). [tint] colors the glass, for
 * example the accent color of the + button.
 */
@Composable
fun Modifier.glass(
    backdrop: HazeState,
    shape: RoundedCornerShape,
    tint: Color? = null,
    clear: Boolean = false,
): Modifier {
    val colors = TikTheme.colors
    val fill = tint ?: if (canBlur) colors.glassTint else colors.glassFallback
    val style = remember(shape, fill, clear) {
        (if (clear) GlassStyle.clear else GlassStyle.regular) then GlassStyle {
            shape(shape)
            tint(fill)
        }
    }
    val input = remember(backdrop) { HazeInput.Sources(backdrop) }
    val rimmed = if (canBendLight) this else this.border(0.75.dp, rimBrush(colors.glassRim), shape)
    return rimmed
        .glassShadow(shape)
        .hazeGlass(input = input, style = style)
}

/** A soft shadow under floating glass, lighter in dark mode where it can't be seen anyway. */
@Composable
fun Modifier.glassShadow(shape: Shape): Modifier {
    val dark = TikTheme.colors.dark
    return dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = 18.dp,
            color = Color.Black,
            offset = DpOffset(0.dp, 6.dp),
            alpha = if (dark) 0.35f else 0.10f,
        ),
    )
}

/**
 * The see-through card that tasks sit on. The background behind cards is a soft gradient, so a
 * light, see-through fill with a bright rim looks like glass without the cost of a real blur.
 */
@Composable
fun Modifier.card(shape: Shape = RoundedCornerShape(22.dp), fill: Color? = null): Modifier {
    val colors = TikTheme.colors
    return this
        .dropShadow(
            shape = shape,
            shadow = Shadow(
                radius = 14.dp,
                color = Color.Black,
                offset = DpOffset(0.dp, 3.dp),
                alpha = if (colors.dark) 0f else 0.045f,
            ),
        )
        .background(fill ?: colors.card, shape)
        .border(0.75.dp, Brush.verticalGradient(listOf(colors.cardBorderTop, colors.cardBorderBottom)), shape)
}

private fun rimBrush(rim: Color) = Brush.verticalGradient(listOf(rim, rim.copy(alpha = rim.alpha * 0.25f)))
