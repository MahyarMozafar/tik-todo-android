package com.mahyarmozafar.tik.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.logic.TaskFilter
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.ui.LocalAppData
import com.mahyarmozafar.tik.ui.components.AppBackground
import com.mahyarmozafar.tik.ui.components.EmptyState
import com.mahyarmozafar.tik.ui.components.GlassTopBar
import com.mahyarmozafar.tik.ui.components.SegmentedControl
import com.mahyarmozafar.tik.ui.components.TaskItem
import com.mahyarmozafar.tik.ui.components.rememberNow
import com.mahyarmozafar.tik.ui.glass.LocalBackdrop
import com.mahyarmozafar.tik.ui.rememberTaskHandlers
import com.mahyarmozafar.tik.ui.theme.TikTheme
import dev.chrisbanes.haze.hazeSource

enum class SearchScope { All, Open, Done }

/** Finds tasks by title, notes or subtasks, in every list. Open tasks come first. */
fun searchResults(tasks: List<Task>, query: String, scope: SearchScope): List<Task> {
    val matches = tasks.sortedByDescending { it.createdAt }.filter { task ->
        TaskFilter.matches(task, query) && when (scope) {
            SearchScope.All -> true
            SearchScope.Open -> !task.isDone
            SearchScope.Done -> task.isDone
        }
    }
    return matches.filter { !it.isDone } + matches.filter { it.isDone }
}

/** The Search tab. The search field itself lives in the tab bar. */
@Composable
fun SearchScreen(query: String, bottomPadding: Dp, modifier: Modifier = Modifier, showTitle: Boolean = true) {
    val data = LocalAppData.current
    val backdrop = LocalBackdrop.current
    val handlers = rememberTaskHandlers()
    val density = LocalDensity.current
    val now = rememberNow()
    var scope by rememberSaveable { mutableStateOf(SearchScope.All) }
    var headerHeight by remember { mutableIntStateOf(0) }
    val results = remember(data.tasks, query, scope) { searchResults(data.tasks, query, scope) }

    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hazeSource(backdrop)) {
            AppBackground()
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("searchResults"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = with(density) { headerHeight.toDp() } + 4.dp,
                    bottom = bottomPadding + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(results, key = { it.id }) { task ->
                    TaskItem(task, data.list(task.listId), now, handlers, Modifier.animateItem())
                }
            }
            Box(
                Modifier.fillMaxSize().padding(top = with(density) { headerHeight.toDp() }, bottom = bottomPadding),
                contentAlignment = Alignment.Center,
            ) {
                if (query.isBlank()) {
                    EmptyState(R.drawable.ic_search, stringResource(R.string.search_your_tasks), stringResource(R.string.search_explained))
                } else if (results.isEmpty()) {
                    EmptyState(R.drawable.ic_search_off, stringResource(R.string.no_results), stringResource(R.string.nothing_matches, query.trim()))
                }
            }
        }
        GlassTopBar(backdrop, Modifier.onSizeChanged { headerHeight = it.height }) {
            Column(Modifier.weight(1f)) {
                if (showTitle) {
                    Text(stringResource(R.string.search), style = TikTheme.type.largeTitle, color = TikTheme.colors.primaryText)
                    Spacer(Modifier.height(10.dp))
                }
                SegmentedControl(
                    options = SearchScope.entries,
                    selected = scope,
                    label = {
                        stringResource(
                            when (it) {
                                SearchScope.All -> R.string.scope_all
                                SearchScope.Open -> R.string.scope_open
                                SearchScope.Done -> R.string.scope_done
                            },
                        )
                    },
                    onSelect = { scope = it },
                )
            }
        }
    }
}
