package com.mahyarmozafar.tik.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.ui.theme.TikTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class SwipeAction(
    @DrawableRes val icon: Int,
    val label: String,
    val color: Color,
    val onClick: () -> Unit,
)

/** Which row is swiped open on a screen, so opening one closes the others. */
class SwipeGroup {
    var openKey by mutableStateOf<Any?>(null)
}

val LocalSwipeGroup = staticCompositionLocalOf { SwipeGroup() }

private val ButtonWidth = 74.dp
private val Gap = 8.dp

/**
 * Swipe toward the end to show [leading] (for example Done), and toward the start to show
 * [trailing] (for example Delete and Tomorrow). A long swipe runs the outermost action,
 * like on iOS. Everything flips in Farsi.
 */
@Composable
fun SwipeActionsBox(
    key: Any,
    leading: SwipeAction?,
    trailing: List<SwipeAction>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val direction = if (rtl) -1f else 1f
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val feedback = rememberFeedback()
    val group = LocalSwipeGroup.current

    /** Positive shows the leading action, negative the trailing ones. */
    val offset = remember { Animatable(0f) }
    var rowWidth by remember { mutableFloatStateOf(0f) }
    var pastFullSwipe by remember { mutableStateOf(false) }

    val button = with(density) { ButtonWidth.toPx() }
    val gap = with(density) { Gap.toPx() }
    val leadingOpen = if (leading != null) button + gap else 0f
    val trailingOpen = trailing.size * (button + gap)
    val settleSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 500f)

    LaunchedEffect(group.openKey) {
        if (group.openKey != key && offset.value != 0f) offset.animateTo(0f, settleSpring)
    }

    fun fullSwipe(value: Float) = rowWidth > 0f && abs(value) > rowWidth * 0.55f

    fun close() {
        scope.launch { offset.animateTo(0f, settleSpring) }
    }

    fun run(action: SwipeAction, fromFull: Boolean) {
        scope.launch {
            if (fromFull) offset.animateTo(if (offset.value > 0) rowWidth else -rowWidth, settleSpring)
            action.onClick()
            offset.snapTo(0f)
        }
        if (!fromFull) close()
    }

    fun settle(velocity: Float) {
        val value = offset.value
        when {
            value > 0f && leading != null -> when {
                fullSwipe(value) || (velocity > 2500f && value > leadingOpen) -> run(leading, fromFull = true)
                value > leadingOpen / 2 || velocity > 900f -> scope.launch { offset.animateTo(leadingOpen, settleSpring) }
                else -> close()
            }
            value < 0f && trailing.isNotEmpty() -> when {
                fullSwipe(value) || (velocity < -2500f && -value > trailingOpen) -> run(trailing.first(), fromFull = true)
                -value > trailingOpen / 2 || velocity < -900f -> scope.launch { offset.animateTo(-trailingOpen, settleSpring) }
                else -> close()
            }
            else -> close()
        }
    }

    val dragState = rememberDraggableState { delta ->
        val max = if (leading != null) rowWidth else 0f
        val min = if (trailing.isNotEmpty()) -rowWidth else 0f
        val target = (offset.value + delta * direction).coerceIn(min, max)
        scope.launch { offset.snapTo(target) }
        val past = fullSwipe(target)
        if (past != pastFullSwipe) {
            pastFullSwipe = past
            feedback.selection()
        }
    }

    Box(modifier.onSizeChanged { rowWidth = it.width.toFloat() }) {
        val value = offset.value
        if (value > 0f && leading != null) {
            Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.Start) {
                ActionButton(leading, widthPx = value - gap) { run(leading, fromFull = false) }
            }
        }
        if (value < 0f && trailing.isNotEmpty()) {
            val shown = -value
            Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
                // The first action is the outermost one, at the end edge, and grows on a long swipe.
                val widths = trailing.indices.map { index ->
                    if (shown <= trailingOpen) {
                        shown / trailing.size - gap
                    } else if (index == 0) {
                        shown - (trailing.size - 1) * (button + gap) - gap
                    } else {
                        button
                    }
                }
                for (index in trailing.indices.reversed()) {
                    Spacer(Modifier.width(with(density) { gap.toDp() }))
                    ActionButton(trailing[index], widthPx = widths[index]) { run(trailing[index], fromFull = false) }
                }
            }
        }

        Box(
            Modifier
                .offset { IntOffset((value * direction).roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        group.openKey = key
                        pastFullSwipe = false
                    },
                    onDragStopped = { velocity -> settle(velocity * direction) },
                ),
        ) {
            content()
            if (value != 0f) {
                // While open, a tap on the task closes the actions instead of opening the task.
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
                )
            }
        }
    }
}

@Composable
private fun ActionButton(action: SwipeAction, widthPx: Float, onClick: () -> Unit) {
    val density = LocalDensity.current
    val width = with(density) { widthPx.coerceAtLeast(0f).toDp() }
    val shape = RoundedCornerShape(22.dp)
    val showContent = width > 40.dp
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .clip(shape)
            .background(action.color, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { alpha = if (showContent) 1f else 0f },
        ) {
            Icon(painterResource(action.icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                action.label,
                style = TikTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
