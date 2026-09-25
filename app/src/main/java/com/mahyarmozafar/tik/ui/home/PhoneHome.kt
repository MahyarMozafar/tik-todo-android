package com.mahyarmozafar.tik.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahyarmozafar.tik.app.AppTab
import com.mahyarmozafar.tik.ui.LocalModel
import com.mahyarmozafar.tik.ui.components.QuickAddBar
import com.mahyarmozafar.tik.ui.glass.LocalBackdrop
import com.mahyarmozafar.tik.ui.lists.ListsScreen
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.navigation.navigator
import com.mahyarmozafar.tik.ui.search.SearchScreen
import com.mahyarmozafar.tik.ui.theme.Motion
import com.mahyarmozafar.tik.ui.theme.TikTheme
import com.mahyarmozafar.tik.ui.today.TodayScreen
import dev.chrisbanes.haze.rememberHazeState
import java.time.Instant

/** The phone layout: Today, Lists and Search, with the glass tab bar floating at the bottom. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhoneHome() {
    val model = LocalModel.current
    val navigator = navigator
    val calendar = TikTheme.formatting.calendar
    val density = LocalDensity.current

    val tab by model.selectedTab.collectAsStateWithLifecycle()
    var lastTab by rememberSaveable { mutableStateOf(AppTab.Today) }
    var query by rememberSaveable { mutableStateOf("") }
    var adding by rememberSaveable { mutableStateOf(false) }
    var bottomHeight by remember { mutableIntStateOf(0) }
    val keyboardOpen = WindowInsets.isImeVisible

    LaunchedEffect(tab) {
        if (tab != AppTab.Search) lastTab = tab
        if (tab != AppTab.Today) adding = false
    }

    // The widget's + button opens Today with the quick add field ready.
    val pendingQuickAdd by model.pendingQuickAdd.collectAsStateWithLifecycle()
    LaunchedEffect(pendingQuickAdd) {
        if (pendingQuickAdd) {
            model.pendingQuickAdd.value = false
            model.selectedTab.value = AppTab.Today
            adding = true
        }
    }

    BackHandler(enabled = tab == AppTab.Search) { model.selectedTab.value = lastTab }
    BackHandler(enabled = adding) { adding = false }

    val backdrop = rememberHazeState()
    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize()) {
            val bottomPadding = with(density) { bottomHeight.toDp() }
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(Motion.effects()) togetherWith fadeOut(Motion.effects()) },
                label = "tabs",
            ) { current ->
                when (current) {
                    AppTab.Today -> TodayScreen(bottomPadding = bottomPadding)
                    AppTab.Lists -> ListsScreen(bottomPadding = bottomPadding)
                    AppTab.Search -> SearchScreen(query = query, bottomPadding = bottomPadding)
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { bottomHeight = it.height }
                    .imePadding(),
            ) {
                if (tab == AppTab.Today) {
                    QuickAddBar(
                        expanded = adding,
                        onExpandedChange = { adding = it },
                        backdrop = backdrop,
                        onAdd = { title ->
                            model.launch { addTask(title, calendar.startOfDay(Instant.now())) }
                        },
                        onShowDetails = { title ->
                            navigator.open(
                                Route.Editor(title = title, dueDateMillis = calendar.startOfDay(Instant.now()).toEpochMilli()),
                            )
                        },
                    )
                }
                AnimatedVisibility(
                    visible = !keyboardOpen || tab == AppTab.Search,
                    enter = slideInVertically(Motion.spatial()) { it } + fadeIn(Motion.effects()),
                    exit = slideOutVertically(Motion.spatial()) { it } + fadeOut(Motion.effects()),
                ) {
                    GlassTabBar(
                        selected = tab,
                        lastTab = lastTab,
                        onSelect = { model.selectedTab.value = it },
                        query = query,
                        onQueryChange = { query = it },
                        backdrop = backdrop,
                        modifier = Modifier.navigationBarsPadding().padding(bottom = 8.dp),
                    )
                }
                if (keyboardOpen && tab != AppTab.Search) Spacer(Modifier.height(4.dp))
            }
        }
    }
}
