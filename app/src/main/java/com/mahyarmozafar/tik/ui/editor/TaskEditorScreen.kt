package com.mahyarmozafar.tik.ui.editor

import androidx.compose.runtime.Composable
import com.mahyarmozafar.tik.ui.components.SheetScaffold
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.navigation.navigator

/** Adds a new task or edits one. */
@Composable
fun TaskEditorScreen(route: Route.Editor) {
    val navigator = navigator
    SheetScaffold(title = "", onClose = { navigator.close(route) }) {}
}
