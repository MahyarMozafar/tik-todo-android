package com.mahyarmozafar.tik.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.ui.glass.card
import com.mahyarmozafar.tik.ui.theme.TikTheme

/**
 * Rows in a group are separate cards with small corners between them and big corners at the
 * ends, like the grouped lists in Android 16.
 */
enum class GroupPosition { Only, First, Middle, Last }

fun groupPosition(index: Int, count: Int): GroupPosition = when {
    count <= 1 -> GroupPosition.Only
    index == 0 -> GroupPosition.First
    index == count - 1 -> GroupPosition.Last
    else -> GroupPosition.Middle
}

private val Outer = 22.dp
private val Inner = 6.dp

fun GroupPosition.shape(): Shape = when (this) {
    GroupPosition.Only -> RoundedCornerShape(Outer)
    GroupPosition.First -> RoundedCornerShape(topStart = Outer, topEnd = Outer, bottomStart = Inner, bottomEnd = Inner)
    GroupPosition.Middle -> RoundedCornerShape(Inner)
    GroupPosition.Last -> RoundedCornerShape(topStart = Inner, topEnd = Inner, bottomStart = Outer, bottomEnd = Outer)
}

/** A color that is a little lighter at the top, like iOS's `.gradient`. */
fun Color.softGradient(): Brush = Brush.verticalGradient(listOf(lerp(this, Color.White, 0.18f), this))

/** A small colored circle with a white icon, used for lists and smart lists. */
@Composable
fun IconBadge(@DrawableRes icon: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 32.dp, iconSize: Dp = 17.dp) {
    Box(modifier.size(size).background(color.softGradient(), CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

/** One row in a group, on a see-through card. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupRow(
    position: GroupPosition,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val clickable = if (onClick != null || onLongClick != null) {
        Modifier
            .pressScale(interaction, pressedScale = 0.98f)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            )
    } else {
        Modifier
    }
    Row(
        modifier
            .then(clickable)
            .fillMaxWidth()
            .card(position.shape())
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A row in "My Lists": colored icon, name and the number of open tasks. */
@Composable
fun RowScope.ListRowContent(
    title: String,
    @DrawableRes icon: Int,
    color: Color,
    count: Int,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    IconBadge(icon, color)
    Spacer(Modifier.width(12.dp))
    Text(
        title,
        style = type.body.copy(fontWeight = FontWeight.Medium),
        color = colors.primaryText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
    if (count > 0) {
        Text(count.toString(), style = type.body, color = colors.secondaryText)
        Spacer(Modifier.width(6.dp))
    }
    if (trailing != null) {
        trailing()
    } else {
        Icon(
            painterResource(R.drawable.ic_chevron_end),
            contentDescription = null,
            tint = colors.tertiaryText,
            modifier = Modifier.size(20.dp),
        )
    }
}
