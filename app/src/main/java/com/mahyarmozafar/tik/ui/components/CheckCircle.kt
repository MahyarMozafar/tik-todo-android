package com.mahyarmozafar.tik.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.ui.theme.TikTheme

/**
 * The round check box. When ticked it fills with the accent color, draws a check mark, and gives
 * a small bounce.
 */
@Composable
fun CheckCircle(
    isDone: Boolean,
    modifier: Modifier = Modifier,
    ringColor: Color = TikTheme.colors.secondaryText,
    fillColor: Color = TikTheme.colors.accent,
    size: Dp = 24.dp,
) {
    // The iOS spring: response 0.32 s, damping 0.62.
    val progress by animateFloatAsState(
        targetValue = if (isDone) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 385f),
        label = "check",
    )
    val bounce = remember { Animatable(1f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(isDone) {
        if (first[0]) {
            first[0] = false
            return@LaunchedEffect
        }
        bounce.animateTo(1.18f, spring(dampingRatio = 0.7f, stiffness = 1800f))
        bounce.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 500f))
    }

    val checkPath = remember { Path() }
    val trimmed = remember { Path() }
    val measure = remember { PathMeasure() }

    Canvas(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = bounce.value
                scaleY = bounce.value
            },
    ) {
        val radius = this.size.minDimension / 2
        val ring = 1.8.dp.toPx()
        drawCircle(
            color = ringColor.copy(alpha = 0.75f * (1f - progress).coerceIn(0f, 1f)),
            radius = radius - ring / 2,
            style = Stroke(ring),
        )
        val fill = progress.coerceIn(0f, 1f)
        if (fill > 0f) {
            drawCircle(color = fillColor.copy(alpha = fill), radius = radius * (0.2f + 0.8f * progress.coerceAtLeast(0f)))
        }

        val width = this.size.width * 0.46f
        val height = this.size.height * 0.34f
        checkPath.reset()
        checkPath.moveTo(0f, height * 0.55f)
        checkPath.lineTo(width * 0.36f, height)
        checkPath.lineTo(width, 0f)
        measure.setPath(checkPath, false)
        trimmed.reset()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), trimmed, true)
        translate(left = (this.size.width - width) / 2, top = (this.size.height - height) / 2) {
            drawPath(
                path = trimmed,
                color = Color.White,
                style = Stroke(width = this@Canvas.size.width * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
