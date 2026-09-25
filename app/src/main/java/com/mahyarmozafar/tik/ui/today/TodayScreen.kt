package com.mahyarmozafar.tik.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.logic.TaskFilter
import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.ui.LocalAppData
import com.mahyarmozafar.tik.ui.rememberTaskHandlers
import com.mahyarmozafar.tik.ui.components.AppBackground
import com.mahyarmozafar.tik.ui.components.EmptyState
import com.mahyarmozafar.tik.ui.components.GlassIconButton
import com.mahyarmozafar.tik.ui.components.GlassTopBar
import com.mahyarmozafar.tik.ui.components.LocalSwipeGroup
import com.mahyarmozafar.tik.ui.components.ProgressCard
import com.mahyarmozafar.tik.ui.components.SwipeGroup
import com.mahyarmozafar.tik.ui.components.TaskItem
import com.mahyarmozafar.tik.ui.components.rememberNow
import com.mahyarmozafar.tik.ui.glass.LocalBackdrop
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.navigation.navigator
import com.mahyarmozafar.tik.ui.theme.TikTheme
import dev.chrisbanes.haze.hazeSource
import java.time.Instant
import java.util.Locale

/** The first screen: today's tasks, late tasks, and a progress bar. */
@Composable
fun TodayScreen(bottomPadding: Dp, modifier: Modifier = Modifier, showSettingsButton: Boolean = true) {
    val data = LocalAppData.current
    val settings = TikTheme.settings
    val calendar = TikTheme.formatting.calendar
    val backdrop = LocalBackdrop.current
    val navigator = navigator
    val handlers = rememberTaskHandlers()
    val density = LocalDensity.current
    val now = rememberNow()

    val todays = remember(data.tasks, now, settings.sortOrder, calendar) {
        TaskFilter.today(data.tasks, now, calendar, settings.sortOrder)
    }
    val visible = if (settings.showCompletedInToday) todays else todays.filter { !it.isDone }
    val doneCount = todays.count { it.isDone }
    var headerHeight by remember { mutableIntStateOf(0) }
    val swipeGroup = remember { SwipeGroup() }

    CompositionLocalProvider(LocalSwipeGroup provides swipeGroup) {
        Box(modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().hazeSource(backdrop)) {
                AppBackground()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("todayList"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = with(density) { headerHeight.toDp() } + 4.dp,
                        bottom = bottomPadding + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (settings.showProgress && todays.isNotEmpty()) {
                        item(key = "progress") {
                            ProgressCard(doneCount, todays.size, Modifier.animateItem().padding(bottom = 6.dp))
                        }
                    }
                    if (visible.isEmpty()) {
                        item(key = "empty") {
                            val allDone = todays.isNotEmpty()
                            EmptyState(
                                icon = if (allDone) R.drawable.ic_verified else R.drawable.ic_sun_fill,
                                title = stringResource(if (allDone) R.string.everything_is_done else R.string.a_fresh_day),
                                message = stringResource(if (allDone) R.string.enjoy_the_rest else R.string.tap_plus_first_task),
                                modifier = Modifier.animateItem(),
                            )
                        }
                    } else {
                        items(visible, key = { it.id }) { task ->
                            TaskItem(
                                task = task,
                                list = data.list(task.listId),
                                now = now,
                                handlers = handlers,
                                showsDay = false,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
            TodayHeader(
                now = now,
                showSettingsButton = showSettingsButton,
                onOpenSettings = { navigator.open(Route.Settings) },
                modifier = Modifier.onSizeChanged { headerHeight = it.height },
            )
        }
    }
}

/** "THURSDAY, MEHR 2" above a big "Today", with the Settings button. */
@Composable
private fun TodayHeader(now: Instant, showSettingsButton: Boolean, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    val english = TikTheme.settings.language == AppLanguage.English
    val day = TikTheme.formatting.fullDay(now)

    GlassTopBar(LocalBackdrop.current, modifier) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
            Text(
                if (english) day.uppercase(Locale.ENGLISH) else day,
                style = type.subheadline.copy(fontWeight = FontWeight.SemiBold, letterSpacing = if (english) 0.6.sp else 0.sp),
                color = colors.accent,
            )
            Text(stringResource(R.string.today), style = type.largeTitle, color = colors.primaryText)
        }
        if (showSettingsButton) {
            GlassIconButton(
                icon = R.drawable.ic_settings,
                contentDescription = stringResource(R.string.settings),
                backdrop = LocalBackdrop.current,
                onClick = onOpenSettings,
                modifier = Modifier.testTag("settingsButton"),
            )
        }
    }
}
