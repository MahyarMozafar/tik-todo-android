package com.mahyarmozafar.tik.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.ui.glass.CapsuleShape
import com.mahyarmozafar.tik.ui.theme.Motion
import com.mahyarmozafar.tik.ui.theme.TikTheme

/** A row of choices where one is picked, with a pill that slides to it (like iOS's segmented picker). */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    tag: (T) -> String = { "segment-$it" },
) {
    val colors = TikTheme.colors
    val feedback = rememberFeedback()
    val index = options.indexOf(selected).coerceAtLeast(0)
    val position by animateFloatAsState(index.toFloat(), Motion.fastSpatial(), label = "segment")

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(colors.fill, CapsuleShape)
            .padding(3.dp),
    ) {
        val segment = maxWidth / options.size
        Box(
            Modifier
                .offset(x = segment * position)
                .width(segment)
                .fillMaxHeight()
                .dropShadow(CapsuleShape, Shadow(radius = 6.dp, color = Color.Black, offset = DpOffset(0.dp, 2.dp), alpha = if (colors.dark) 0f else 0.12f))
                .background(if (colors.dark) Color(0xFF636366) else Color.White, CapsuleShape),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (!isSelected) {
                                feedback.selection()
                                onSelect(option)
                            }
                        }
                        .semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        }
                        .testTag(tag(option)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(option),
                        style = TikTheme.type.footnote.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}
