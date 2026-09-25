package com.mahyarmozafar.tik.ui.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.time.CalendarMonth
import com.mahyarmozafar.tik.ui.components.rememberFeedback
import com.mahyarmozafar.tik.ui.theme.TikTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

/**
 * A month of days in the calendar picked in Settings (Shamsi or Gregorian), with English digits
 * and the week starting on Saturday for Shamsi.
 */
@Composable
fun CalendarPicker(selected: LocalDate, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    val formatting = TikTheme.formatting
    val calendar = formatting.calendar
    val feedback = rememberFeedback()
    val today = remember { calendar.localDate(Instant.now()) }
    var month by remember(selected, calendar) { mutableStateOf(calendar.month(selected)) }
    var forward by remember { mutableStateOf(true) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatting.monthAndYear(month),
                style = type.headline,
                color = colors.primaryText,
                modifier = Modifier.weight(1f),
            )
            MonthButton(R.drawable.ic_chevron_start, stringResource(R.string.previous_month)) {
                forward = false
                month = month.plus(-1)
            }
            MonthButton(R.drawable.ic_chevron_end, stringResource(R.string.next_month)) {
                forward = true
                month = month.plus(1)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            formatting.orderedWeekdays.forEach { weekday ->
                Text(
                    weekday.letter,
                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val side = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
                (slideInHorizontally(tween(260)) { it * side / 3 } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(260)) { -it * side / 3 } + fadeOut(tween(200)))
            },
            label = "month",
        ) { shown ->
            MonthGrid(shown, selected, today) { day ->
                feedback.selection()
                onSelect(day)
            }
        }
    }
}

@Composable
private fun MonthGrid(month: CalendarMonth, selected: LocalDate, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    val formatting = TikTheme.formatting
    val calendar = formatting.calendar
    val blanks = calendar.leadingBlankDays(month)
    val days = calendar.daysInMonth(month)
    val rows = (blanks + days + 6) / 7

    Column {
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val number = row * 7 + column - blanks + 1
                    if (number !in 1..days) {
                        Spacer(Modifier.weight(1f))
                        continue
                    }
                    val date = calendar.date(month, number)
                    val isSelected = date == selected
                    val isToday = date == today
                    val description = formatting.fullDay(calendar.startOfDay(date))
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp)
                            .background(if (isSelected) colors.accent else Color.Transparent, CircleShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(date) }
                            .semantics {
                                role = Role.RadioButton
                                this.selected = isSelected
                                contentDescription = description
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            number.toString(),
                            style = type.callout.copy(fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal),
                            color = when {
                                isSelected -> colors.onAccent
                                isToday -> colors.accent
                                else -> colors.primaryText
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = TikTheme.colors.accent, modifier = Modifier.size(24.dp))
    }
}

/** Scroll wheels for the hour and minute, like the iOS time picker. English digits in both languages. */
@Composable
fun TimeWheels(time: LocalTime, use24Hour: Boolean, onChange: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    val hourLabel = stringResource(R.string.hour)
    val minuteLabel = stringResource(R.string.minute)
    // A clock reads left to right in every language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (use24Hour) {
                Wheel(24, time.hour, { it.toString().padStart(2, '0') }, hourLabel) { onChange(time.withHour(it)) }
            } else {
                val hour12 = if (time.hour % 12 == 0) 12 else time.hour % 12
                Wheel(12, hour12 - 1, { (it + 1).toString() }, hourLabel) { index ->
                    val hour = (index + 1) % 12 + if (time.hour >= 12) 12 else 0
                    onChange(time.withHour(hour))
                }
            }
            Text(":", style = TikTheme.type.title3, color = TikTheme.colors.primaryText, modifier = Modifier.padding(horizontal = 4.dp))
            Wheel(60, time.minute, { it.toString().padStart(2, '0') }, minuteLabel) { onChange(time.withMinute(it)) }
            if (!use24Hour) {
                Spacer(Modifier.width(12.dp))
                val (am, pm) = TikTheme.formatting.amPm
                Wheel(2, if (time.hour < 12) 0 else 1, { if (it == 0) am else pm }, "", cyclic = false) { index ->
                    val base = time.hour % 12
                    onChange(time.withHour(base + if (index == 1) 12 else 0))
                }
            }
        }
    }
}

private val WheelItem = 40.dp
private const val WHEEL_ROWS = 5

/** One scroll wheel. It snaps to a row, and the row in the middle is the chosen one. */
@Composable
private fun Wheel(
    count: Int,
    selected: Int,
    label: (Int) -> String,
    description: String,
    width: Dp = 64.dp,
    cyclic: Boolean = true,
    onSelect: (Int) -> Unit,
) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    val feedback = rememberFeedback()
    val density = LocalDensity.current
    val itemPx = with(density) { WheelItem.toPx() }
    val repeats = if (cyclic) 200 else 1
    val middle = if (cyclic) repeats / 2 * count else 0
    val state = rememberLazyListState(initialFirstVisibleItemIndex = middle + selected)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val center by remember {
        derivedStateOf { state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0 }
    }
    var lastTicked by remember { mutableIntStateOf(center) }

    LaunchedEffect(state) {
        snapshotFlow { center }.distinctUntilChanged().collect { index ->
            if (index != lastTicked) {
                lastTicked = index
                feedback.selection()
            }
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                val value = center % count
                if (value != selected) onSelect(value)
            }
        }
    }
    // Follow changes made elsewhere (for example the AM/PM wheel).
    LaunchedEffect(selected) {
        if (!state.isScrollInProgress && center % count != selected) {
            state.scrollToItem(center - center % count + selected)
        }
    }

    Box(
        Modifier
            .width(width)
            .height(WheelItem * WHEEL_ROWS)
            .semantics {
                contentDescription = description
                stateDescription = label(selected)
                customActions = listOf(
                    CustomAccessibilityAction("+") {
                        onSelect((selected + 1) % count)
                        true
                    },
                    CustomAccessibilityAction("-") {
                        onSelect((selected - 1 + count) % count)
                        true
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxWidth().height(WheelItem).background(colors.fill, RoundedCornerShape(10.dp)))
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = WheelItem * (WHEEL_ROWS / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(count * repeats) { index ->
                val distance = abs(index - center)
                Box(Modifier.height(WheelItem).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        label(index % count),
                        style = type.title3.copy(fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal),
                        color = colors.primaryText,
                        modifier = Modifier.alpha(
                            when (distance) {
                                0 -> 1f
                                1 -> 0.45f
                                else -> 0.2f
                            },
                        ),
                    )
                }
            }
        }
    }
}
