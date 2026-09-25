package com.mahyarmozafar.tik.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.mahyarmozafar.tik.app.AppData
import com.mahyarmozafar.tik.app.AppModel
import com.mahyarmozafar.tik.model.AppLanguage
import com.mahyarmozafar.tik.model.Task
import com.mahyarmozafar.tik.ui.components.TaskHandlers
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.navigation.navigator

val LocalModel = staticCompositionLocalOf<AppModel> { error("No AppModel") }
val LocalAppData = staticCompositionLocalOf<AppData> { error("No AppData") }

/** True on tablets and other wide windows, which get a sidebar instead of tabs. */
val LocalWideLayout = staticCompositionLocalOf { false }

/** Switches the whole app's language. The screens are rebuilt in the new language. */
val LocalChangeLanguage = staticCompositionLocalOf<(AppLanguage) -> Unit> { {} }

/** The same tap, swipe and menu actions for a task wherever it shows up. */
@Composable
fun rememberTaskHandlers(): TaskHandlers {
    val model = LocalModel.current
    val navigator = navigator
    return remember(model, navigator) {
        TaskHandlers(
            toggle = { task: Task -> model.launch { toggle(task.id) } },
            edit = { task: Task -> navigator.open(Route.Editor(taskId = task.id)) },
            delete = { task: Task -> model.launch { delete(task) } },
            moveToTomorrow = { task: Task -> model.launch { moveToTomorrow(task) } },
            duplicate = { task: Task -> model.launch { duplicate(task) } },
            setPriority = { task, priority -> model.launch { setPriority(task, priority) } },
        )
    }
}
