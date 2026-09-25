package com.mahyarmozafar.tik.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.model.RepeatRule
import com.mahyarmozafar.tik.model.RepeatRule.Frequency
import com.mahyarmozafar.tik.time.TikCalendar
import com.mahyarmozafar.tik.ui.components.GroupPosition
import com.mahyarmozafar.tik.ui.components.GroupRow
import com.mahyarmozafar.tik.ui.components.SectionFooter
import com.mahyarmozafar.tik.ui.components.groupPosition
import com.mahyarmozafar.tik.ui.components.rememberFeedback
import com.mahyarmozafar.tik.ui.theme.TikTheme
import java.time.LocalDate

private enum class Choice { Never, Daily, Weekly, Monthly, Yearly, Weekdays, EveryNDays }

private val RepeatRule?.choice: Choice
    get() = when (this?.frequency) {
        null -> Choice.Never
        Frequency.Daily -> Choice.Daily
        Frequency.Weekly -> Choice.Weekly
        Frequency.Monthly -> Choice.Monthly
        Frequency.Yearly -> Choice.Yearly
        Frequency.Weekdays -> Choice.Weekdays
        Frequency.EveryNDays -> Choice.EveryNDays
    }

/** Short text for a rule, like "Every Day" or "Sat, Mon, Wed". */
@Composable
fun repeatSummary(rule: RepeatRule?): String {
    val formatting = TikTheme.formatting
    return when (rule?.frequency) {
        null -> stringResource(R.string.never)
        Frequency.Daily -> stringResource(R.string.every_day)
        Frequency.Weekly -> stringResource(R.string.every_week)
        Frequency.Monthly -> stringResource(R.string.every_month)
        Frequency.Yearly -> stringResource(R.string.every_year)
        Frequency.EveryNDays -> stringResource(R.string.every_n_days, rule.interval.toString())
        Frequency.Weekdays -> {
            val days = formatting.orderedWeekdays.filter { it.number in rule.weekdays }
            if (days.size == 7) {
                stringResource(R.string.every_day)
            } else {
                days.joinToString(if (formatting.language == AppLanguage.Farsi) "، " else ", ") { it.shortName }
            }
        }
    }
}

/** Picks how a task repeats: every day, on certain weekdays, every few days, and so on. */
@Composable
fun RepeatSheet(rule: RepeatRule?, day: LocalDate, calendar: TikCalendar, onChange: (RepeatRule?) -> Unit, onDismiss: () -> Unit) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    val feedback = rememberFeedback()
    val current = rule.choice

    fun select(choice: Choice) {
        feedback.selection()
        onChange(
            when (choice) {
                Choice.Never -> null
                Choice.Daily -> RepeatRule(Frequency.Daily)
                Choice.Weekly -> RepeatRule(Frequency.Weekly)
                Choice.Monthly -> RepeatRule(Frequency.Monthly)
                Choice.Yearly -> RepeatRule(Frequency.Yearly)
                Choice.Weekdays -> if (current == Choice.Weekdays) rule else RepeatRule(Frequency.Weekdays, weekdays = setOf(TikCalendar.weekdayNumber(day)))
                Choice.EveryNDays -> if (current == Choice.EveryNDays) rule else RepeatRule(Frequency.EveryNDays, interval = 2)
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                stringResource(R.string.repeat),
                style = type.headline,
                color = colors.primaryText,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                textAlign = TextAlign.Center,
            )
            val simple = listOf(
                Choice.Never to R.string.never,
                Choice.Daily to R.string.every_day,
                Choice.Weekly to R.string.every_week,
                Choice.Monthly to R.string.every_month,
                Choice.Yearly to R.string.every_year,
            )
            simple.forEachIndexed { index, (choice, label) ->
                OptionRow(stringResource(label), selected = current == choice, position = groupPosition(index, simple.size)) { select(choice) }
                Spacer(Modifier.height(2.dp))
            }

            Spacer(Modifier.height(18.dp))
            OptionRow(
                stringResource(R.string.on_certain_days),
                selected = current == Choice.Weekdays,
                position = if (current == Choice.Weekdays) groupPosition(0, 4) else groupPosition(0, 2),
            ) { select(Choice.Weekdays) }
            Spacer(Modifier.height(2.dp))
            if (current == Choice.Weekdays && rule != null) {
                GroupRow(position = groupPosition(1, 4)) {
                    WeekdayPicker(rule.weekdays) { onChange(rule.copy(weekdays = it)) }
                }
                Spacer(Modifier.height(2.dp))
            }
            OptionRow(
                stringResource(R.string.every_few_days),
                selected = current == Choice.EveryNDays,
                position = if (current == Choice.Weekdays) groupPosition(2, 4) else if (current == Choice.EveryNDays) groupPosition(1, 3) else groupPosition(1, 2),
            ) { select(Choice.EveryNDays) }
            if (current == Choice.EveryNDays && rule != null) {
                Spacer(Modifier.height(2.dp))
                GroupRow(position = groupPosition(2, 3)) {
                    Stepper(
                        text = stringResource(R.string.every_n_days, rule.interval.toString()),
                        value = rule.interval,
                        range = 2..30,
                    ) { onChange(rule.copy(interval = it)) }
                }
            }
            SectionFooter(stringResource(R.string.repeat_explained))
        }
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, position: GroupPosition, onClick: () -> Unit) {
    val colors = TikTheme.colors
    GroupRow(
        position = position,
        onClick = onClick,
        modifier = Modifier.semantics {
            this.selected = selected
        },
    ) {
        Text(text, style = TikTheme.type.body, color = colors.primaryText, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        }
    }
}

/** Seven round buttons, one per weekday, in the order the week starts. At least one stays on. */
@Composable
fun WeekdayPicker(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val colors = TikTheme.colors
    val feedback = rememberFeedback()
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TikTheme.formatting.orderedWeekdays.forEach { weekday ->
            val on = weekday.number in selected
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(if (on) colors.accent else colors.fill, CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Checkbox) {
                        val updated = if (on) selected - weekday.number else selected + weekday.number
                        if (updated.isNotEmpty()) {
                            feedback.selection()
                            onChange(updated)
                        }
                    }
                    .semantics {
                        contentDescription = weekday.shortName
                        this.selected = on
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    weekday.letter,
                    style = TikTheme.type.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = if (on) colors.onAccent else colors.primaryText,
                )
            }
        }
    }
}

/** "Every 3 days" with − and + buttons. */
@Composable
fun RowScope.Stepper(text: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val colors = TikTheme.colors
    val feedback = rememberFeedback()
    Text(text, style = TikTheme.type.body, color = colors.primaryText, modifier = Modifier.heightIn(min = 36.dp).padding(top = 6.dp))
    Spacer(Modifier.weight(1f))
    Row(
        Modifier.background(colors.fill, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", enabled = value > range.first) {
            feedback.selection()
            onChange(value - 1)
        }
        Box(Modifier.size(1.dp, 18.dp).background(colors.separator))
        StepButton("+", enabled = value < range.last) {
            feedback.selection()
            onChange(value + 1)
        }
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 46.dp, height = 34.dp)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            style = TikTheme.type.title3,
            color = if (enabled) TikTheme.colors.primaryText else TikTheme.colors.tertiaryText,
        )
    }
}
