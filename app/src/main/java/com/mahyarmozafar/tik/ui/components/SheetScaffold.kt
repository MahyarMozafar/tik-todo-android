package com.mahyarmozafar.tik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.ui.glass.LocalBackdrop
import com.mahyarmozafar.tik.ui.theme.TikTheme
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * A full-screen editor that rises like an iOS sheet: a close button, a title, an optional save
 * button, and a scrolling form below.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SheetScaffold(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeIcon: Int = R.drawable.ic_close,
    onSave: (() -> Unit)? = null,
    saveEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = TikTheme.colors
    val density = LocalDensity.current
    val backdrop = rememberHazeState()
    var headerHeight by remember { mutableIntStateOf(0) }

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(modifier.fillMaxSize().background(colors.background)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .hazeSource(backdrop)
                    .background(colors.background)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(with(density) { headerHeight.toDp() } + 8.dp))
                content()
                Spacer(Modifier.height(32.dp))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
            GlassTopBar(backdrop, Modifier.onSizeChanged { headerHeight = it.height }) {
                GlassIconButton(
                    icon = closeIcon,
                    contentDescription = stringResource(if (closeIcon == R.drawable.ic_close) R.string.cancel else R.string.back),
                    backdrop = backdrop,
                    onClick = onClose,
                    modifier = Modifier.testTag("cancelButton"),
                )
                Text(
                    title,
                    style = TikTheme.type.headline,
                    color = colors.primaryText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() },
                )
                if (onSave != null) {
                    GlassIconButton(
                        icon = R.drawable.ic_check,
                        contentDescription = stringResource(R.string.save),
                        backdrop = backdrop,
                        onClick = onSave,
                        enabled = saveEnabled,
                        glassTint = colors.accent,
                        iconTint = colors.onAccent,
                        modifier = Modifier.testTag("saveButton"),
                    )
                } else {
                    Spacer(Modifier.size(44.dp))
                }
            }
        }
    }
}

/** A small title above a group of rows, like the section headers in iOS forms. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = TikTheme.type.footnote,
        color = TikTheme.colors.secondaryText,
        modifier = modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 8.dp),
    )
}

/** A short note under a group of rows. */
@Composable
fun SectionFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = TikTheme.type.footnote,
        color = TikTheme.colors.secondaryText,
        modifier = modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 8.dp),
    )
}
