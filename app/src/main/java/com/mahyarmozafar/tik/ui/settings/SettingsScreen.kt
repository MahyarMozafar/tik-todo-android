package com.mahyarmozafar.tik.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.ui.components.SheetScaffold
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.navigation.navigator

/** Everything that can be changed. */
@Composable
fun SettingsScreen() {
    val navigator = navigator
    SheetScaffold(title = stringResource(R.string.settings), onClose = { navigator.close(Route.Settings) }) {}
}
