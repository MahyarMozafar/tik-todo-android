package com.mahyarmozafar.tik.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.ui.glass.card
import com.mahyarmozafar.tik.ui.theme.TikTheme
import kotlin.math.max
import kotlin.math.roundToInt

/** A rounded bar that fills from the start edge in the accent colors. */
@Composable
fun ProgressBar(value: Float, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val colors = TikTheme.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The iOS spring: response 0.5 s, damping 0.8.
    val animated by animateFloatAsState(value, spring(dampingRatio = 0.8f, stiffness = 158f), label = "progress")

    Canvas(modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = colors.primaryText.copy(alpha = 0.08f), cornerRadius = radius)
        if (animated <= 0.001f) return@Canvas
        val width = max(size.height, size.width * animated.coerceAtMost(1f))
        val left = if (rtl) size.width - width else 0f
        val gradient = listOf(colors.partner.copy(alpha = 0.8f), colors.accent)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = if (rtl) gradient.reversed() else gradient,
                startX = left,
                endX = left + width,
            ),
            topLeft = Offset(left, 0f),
            size = Size(width, size.height),
            cornerRadius = radius,
        )
    }
}

/** "3 of 5 done" with a bar that fills up as tasks are ticked. */
@Composable
fun ProgressCard(done: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    val fraction = if (total == 0) 0f else done.toFloat() / total
    val percent = "${(fraction * 100).roundToInt()}%"

    Column(
        modifier
            .fillMaxWidth()
            .card(RoundedCornerShape(24.dp))
            .padding(18.dp)
            .semantics(mergeDescendants = true) { stateDescription = percent },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = done == total,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "progressTitle",
            ) { allDone ->
                if (allDone) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_verified),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.all_done_for_today),
                            style = type.subheadline.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.accent,
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.progress_done, done.toString(), total.toString()),
                        style = type.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primaryText,
                    )
                }
            }
            Text(
                percent,
                style = type.subheadline.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = colors.secondaryText,
            )
        }
        ProgressBar(fraction)
    }
}
