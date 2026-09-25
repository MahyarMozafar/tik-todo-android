package com.mahyarmozafar.tik.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.ui.glass.glass
import com.mahyarmozafar.tik.ui.theme.Motion
import com.mahyarmozafar.tik.ui.theme.TikTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/** Sinks in a little while pressed, like iOS glass. Use with an `indication = null` clickable. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressedScale: Float = 0.92f): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, Motion.fastSpatial(), label = "pressScale")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** A round glass button with an icon, like iOS's `.buttonStyle(.glass)`. */
@Composable
fun GlassIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    backdrop: HazeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    glassTint: Color? = null,
    iconTint: Color = TikTheme.colors.primaryText,
    enabled: Boolean = true,
) {
    GlassCircle(backdrop, onClick, modifier, size, glassTint, enabled, contentDescription) {
        Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun GlassCircle(
    backdrop: HazeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    glassTint: Color? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(interaction)
            .size(size)
            .glass(backdrop, CircleShape, tint = glassTint)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * A bar at the top of a screen. What scrolls under it is blurred, strongest at the top edge and
 * fading out toward its bottom, like the soft edge iOS draws under its bars.
 */
@Composable
fun GlassTopBar(
    backdrop: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val background = TikTheme.colors.background
    val style = remember(background) {
        HazeBlurStyle {
            blurRadius(20.dp)
            backgroundColor(background)
            colorEffects(
                listOf(
                    HazeColorEffect.tint(
                        Brush.verticalGradient(listOf(background.copy(alpha = 0.72f), background.copy(alpha = 0f))),
                    ),
                ),
            )
            fallbackColorEffect(
                HazeColorEffect.tint(
                    Brush.verticalGradient(listOf(background.copy(alpha = 0.97f), background.copy(alpha = 0.6f), background.copy(alpha = 0f))),
                ),
            )
            progressive(HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f))
        }
    }
    val input = remember(backdrop) { HazeInput.Sources(backdrop) }
    Box(modifier.fillMaxWidth().hazeBlur(input = input, style = style)) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
