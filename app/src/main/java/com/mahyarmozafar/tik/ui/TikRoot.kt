package com.mahyarmozafar.tik.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.mahyarmozafar.tik.app.AppModel
import com.mahyarmozafar.tik.app.AppTab
import com.mahyarmozafar.tik.ui.components.Confetti
import com.mahyarmozafar.tik.ui.components.rememberFeedback
import com.mahyarmozafar.tik.ui.editor.TaskEditorScreen
import com.mahyarmozafar.tik.ui.home.PhoneHome
import com.mahyarmozafar.tik.ui.lists.ListEditorScreen
import com.mahyarmozafar.tik.ui.lists.TasksScreen
import com.mahyarmozafar.tik.ui.navigation.LocalNavigator
import com.mahyarmozafar.tik.ui.navigation.Navigator
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.settings.SettingsScreen
import com.mahyarmozafar.tik.ui.tablet.TabletHome
import com.mahyarmozafar.tik.ui.theme.Motion
import com.mahyarmozafar.tik.ui.theme.TikTheme

/**
 * The whole app: the home screen (tabs on phones, a sidebar on tablets) and the screens that open
 * on top of it. [openTodaySignal] goes up when a reminder or the widget asks for Today.
 */
@Composable
fun TikRoot(model: AppModel, openTodaySignal: Int) {
    val data by model.data.collectAsStateWithLifecycle()
    val loaded = data
    if (loaded == null) {
        Box(Modifier.fillMaxSize().background(TikTheme.colors.background))
        return
    }

    val backStack = rememberNavBackStack(Route.Home)
    val navigator = remember(backStack) { Navigator(backStack) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val celebrations by model.celebrations.collectAsStateWithLifecycle()
    val feedback = rememberFeedback()

    LaunchedEffect(openTodaySignal) {
        if (openTodaySignal > 0) {
            navigator.home()
            model.selectedTab.value = AppTab.Today
        }
    }
    LaunchedEffect(celebrations) {
        if (celebrations > 0) feedback.celebrate()
    }

    // The first task with a time asks for permission to send reminders, like on iOS.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(model) {
        model.permissionRequests.collect {
            if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(TikTheme.colors.background)) {
        val wide = maxWidth >= 700.dp && maxHeight >= 480.dp
        CompositionLocalProvider(
            LocalModel provides model,
            LocalAppData provides loaded,
            LocalNavigator provides navigator,
            LocalWideLayout provides wide,
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { navigator.back() },
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                transitionSpec = { push(rtl) },
                popTransitionSpec = { pop(rtl) },
                predictivePopTransitionSpec = { pop(rtl) },
                entryProvider = entryProvider {
                    entry<Route.Home> { if (wide) TabletHome() else PhoneHome() }
                    entry<Route.Tasks> { TasksScreen(it.place) }
                    entry<Route.Editor>(metadata = sheet) { TaskEditorScreen(it) }
                    entry<Route.ListEditor>(metadata = sheet) { ListEditorScreen(it.listId) }
                    entry<Route.Settings>(metadata = sheet) { SettingsScreen() }
                },
            )
            Confetti(celebrations)
        }
    }
}

/** Screens slide in from the end edge: the right in English, the left in Farsi. */
private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.push(rtl: Boolean): ContentTransform {
    val side = if (rtl) -1 else 1
    return slideInHorizontally(Motion.spatial<IntOffset>()) { width -> side * width } togetherWith
        (slideOutHorizontally(Motion.spatial<IntOffset>()) { width -> -side * width / 4 } + fadeOut(Motion.effects()))
}

private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.pop(rtl: Boolean): ContentTransform {
    val side = if (rtl) -1 else 1
    return (slideInHorizontally(Motion.spatial<IntOffset>()) { width -> -side * width / 4 } + fadeIn(Motion.effects())) togetherWith
        slideOutHorizontally(Motion.spatial<IntOffset>()) { width -> side * width }
}

/** Editors and Settings rise from the bottom like an iOS sheet. */
private val sheet: Map<String, Any> =
    NavDisplay.transitionSpec {
        slideInVertically(Motion.spatial<IntOffset>()) { height -> height } togetherWith
            (scaleOut(targetScale = 0.94f, animationSpec = tween(300)) + fadeOut(tween(300), targetAlpha = 0.6f))
    } + NavDisplay.popTransitionSpec {
        fadeIn(tween(250), initialAlpha = 0.6f) togetherWith slideOutVertically(Motion.spatial<IntOffset>()) { height -> height }
    } + NavDisplay.predictivePopTransitionSpec {
        fadeIn(tween(250), initialAlpha = 0.6f) togetherWith slideOutVertically(Motion.spatial<IntOffset>()) { height -> height }
    }
