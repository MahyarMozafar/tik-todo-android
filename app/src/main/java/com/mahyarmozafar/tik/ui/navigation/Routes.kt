package com.mahyarmozafar.tik.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.mahyarmozafar.tik.model.SmartList
import kotlinx.serialization.Serializable

/** A place that shows tasks: a smart list, the Inbox, or one of the user's lists. */
@Serializable
sealed interface Place {
    @Serializable
    data class Smart(val list: SmartList) : Place

    @Serializable
    data object Inbox : Place

    @Serializable
    data class Custom(val listId: String) : Place
}

/** The screens. Each one is saved with the back stack, so they come back after a language switch. */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Home : Route

    @Serializable
    data class Tasks(val place: Place) : Route

    /** A new task when [taskId] is null, with these starting values. */
    @Serializable
    data class Editor(
        val taskId: String? = null,
        val title: String = "",
        val dueDateMillis: Long? = null,
        val listId: String? = null,
    ) : Route

    @Serializable
    data class ListEditor(val listId: String? = null) : Route

    @Serializable
    data object Settings : Route
}

/** Moves between screens. */
class Navigator(private val backStack: NavBackStack<NavKey>) {
    val current: NavKey get() = backStack.last()

    fun open(route: Route) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /** Closes a screen even if something else was opened on top of it meanwhile. */
    fun close(route: Route) {
        val index = backStack.lastIndexOf(route)
        if (index > 0) {
            while (backStack.size > index) backStack.removeAt(backStack.lastIndex)
        }
    }

    fun home() {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No Navigator") }

val navigator: Navigator
    @Composable @ReadOnlyComposable get() = LocalNavigator.current
