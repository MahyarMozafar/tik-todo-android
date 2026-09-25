package com.mahyarmozafar.tik.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.mahyarmozafar.tik.ui.theme.TikTheme
import kotlin.math.max

/**
 * A soft, colorful background in the accent colors, like the iOS app's mesh gradient.
 * Each glow sits where one of the mesh's points is, with the same strength.
 */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    val colors = TikTheme.colors
    val colorful = TikTheme.settings.colorfulBackground
    val strength = if (colors.dark) 0.85f else 1f
    val main = colors.accent
    val partner = colors.partner

    Canvas(modifier.fillMaxSize()) {
        drawRect(colors.background)
        if (!colorful) return@Canvas

        val radius = max(size.width, size.height) * 0.62f
        fun glow(x: Float, y: Float, color: Color, alpha: Float) {
            val center = Offset(size.width * x, size.height * y)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha * strength), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
        glow(0f, 0f, main, 0.34f)
        glow(0.5f, 0f, partner, 0.20f)
        glow(1f, 0f, main, 0.12f)
        glow(0f, 0.5f, partner, 0.12f)
        glow(1f, 0.5f, main, 0.20f)
        glow(0f, 1f, main, 0.08f)
        glow(0.5f, 1f, partner, 0.22f)
        glow(1f, 1f, main, 0.28f)
    }
}
