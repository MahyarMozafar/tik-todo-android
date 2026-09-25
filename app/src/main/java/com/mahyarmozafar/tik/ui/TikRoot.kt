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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.mahyarmozafar.tik.app.AppModel
import com.mahyarmozafar.tik.app.AppTab
import com.mahyarmozafar.tik.model.SmartList
import com.mahyarmozafar.tik.ui.components.Confetti
import com.mahyarmozafar.tik.ui.components.rememberFeedback
import com.mahyarmozafar.tik.ui.editor.TaskEditorScreen
import com.mahyarmozafar.tik.ui.home.PhoneHome
import com.mahyarmozafar.tik.ui.lists.ListEditorScreen
import com.mahyarmozafar.tik.ui.lists.TasksScreen
import com.mahyarmozafar.tik.ui.navigation.LocalNavigator
import com.mahyarmozafar.tik.ui.navigation.Navigator
import com.mahyarmozafar.tik.ui.navigation.Place
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
    // Remembered across a language switch, so the sound doesn't play again when the screens are rebuilt.
    var celebrated by rememberSaveable { mutableIntStateOf(celebrations) }
    LaunchedEffect(celebrations) {
        if (celebrations > celebrated) {
            celebrated = celebrations
            feedback.celebrate()
        }
    }

    // Debug builds can open a screen at launch, for screenshots.
    val startScreen by model.startScreen.collectAsStateWithLifecycle()
    LaunchedEffect(startScreen) {
        val screen = startScreen ?: return@LaunchedEffect
        model.startScreen.value = null
        when (screen) {
            "settings" -> navigator.open(Route.Settings)
            "scheduled" -> navigator.open(Route.Tasks(Place.Smart(SmartList.Scheduled)))
            "list" -> loaded.lists.firstOrNull()?.let { navigator.open(Route.Tasks(Place.Custom(it.id))) }
            "editor" -> loaded.tasks.firstOrNull { it.subtasks.isNotEmpty() }?.let { navigator.open(Route.Editor(taskId = it.id)) }
        }
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
            // On tablets, editors and Settings open as a card over the sidebar and list, like an
            // iPad form sheet. On phones they fill the screen.
            val sheet = if (wide) cardSheet else fullSheet
            NavDisplay(
                backStack = backStack,
                onBack = { navigator.back() },
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                sceneStrategies = listOf(DialogSceneStrategy<NavKey>(), SinglePaneSceneStrategy()),
                transitionSpec = { push(rtl) },
                popTransitionSpec = { pop(rtl) },
                predictivePopTransitionSpec = { pop(rtl) },
                entryProvider = entryProvider {
                    entry<Route.Home> { if (wide) TabletHome() else PhoneHome() }
                    entry<Route.Tasks> { TasksScreen(it.place) }
                    entry<Route.Editor>(metadata = sheet) { SheetCard(wide) { TaskEditorScreen(it) } }
                    entry<Route.ListEditor>(metadata = sheet) { SheetCard(wide) { ListEditorScreen(it.listId) } }
                    entry<Route.Settings>(metadata = sheet) { SheetCard(wide) { SettingsScreen() } }
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

/**
 * On tablets, a sheet is a card in the middle of the screen. Tapping outside doesn't close it, so
 * changes are never lost by accident; the close button and Back do. The dialog covers the whole
 * screen, so the card can move above the keyboard.
 */
private val cardSheet: Map<String, Any> = DialogSceneStrategy.dialog(
    DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false, decorFitsSystemWindows = false),
)

@Composable
private fun SheetCard(wide: Boolean, content: @Composable () -> Unit) {
    if (!wide) return content()
    // A lighter dim than Android's usual, like iOS.
    val view = LocalView.current
    LaunchedEffect(view) { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0.3f) }
    val shape = RoundedCornerShape(28.dp)
    // The card keeps clear of the bars and the keyboard. The screen inside doesn't add that room
    // again, because the padding here uses it up.
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .widthIn(max = 640.dp)
                .heightIn(max = 760.dp)
                .fillMaxSize()
                .shadow(24.dp, shape)
                .clip(shape),
        ) {
            content()
        }
    }
}

/** On phones, editors and Settings rise from the bottom like an iOS sheet. */
private val fullSheet: Map<String, Any> =
    NavDisplay.transitionSpec {
        slideInVertically(Motion.spatial<IntOffset>()) { height -> height } togetherWith
            (scaleOut(targetScale = 0.94f, animationSpec = tween(300)) + fadeOut(tween(300), targetAlpha = 0.6f))
    } + NavDisplay.popTransitionSpec {
        fadeIn(tween(250), initialAlpha = 0.6f) togetherWith slideOutVertically(Motion.spatial<IntOffset>()) { height -> height }
    } + NavDisplay.predictivePopTransitionSpec {
        fadeIn(tween(250), initialAlpha = 0.6f) togetherWith slideOutVertically(Motion.spatial<IntOffset>()) { height -> height }
    }
