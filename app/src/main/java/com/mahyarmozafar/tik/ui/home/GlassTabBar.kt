package com.mahyarmozafar.tik.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.app.AppTab
import com.mahyarmozafar.tik.ui.components.rememberFeedback
import com.mahyarmozafar.tik.ui.glass.CapsuleShape
import com.mahyarmozafar.tik.ui.glass.canBendLight
import com.mahyarmozafar.tik.ui.glass.glass
import com.mahyarmozafar.tik.ui.theme.Motion
import com.mahyarmozafar.tik.ui.theme.TikTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt

private class TabItem(
    val tab: AppTab,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int,
    @param:StringRes val label: Int,
    val tag: String,
)

private val tabs = listOf(
    TabItem(AppTab.Today, R.drawable.ic_today, R.drawable.ic_today_fill, R.string.today, "tab-today"),
    TabItem(AppTab.Lists, R.drawable.ic_lists, R.drawable.ic_lists_fill, R.string.lists, "tab-lists"),
)

private val BarHeight = 60.dp
private val Gap = 12.dp

/**
 * The floating glass tab bar: Today and Lists in one capsule, and a round Search button next to
 * it. Tapping Search melts the capsule into a small circle and stretches the button into a search
 * field, like the tab bar in iOS 26.
 */
@Composable
fun GlassTabBar(
    selected: AppTab,
    lastTab: AppTab,
    onSelect: (AppTab) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    backdrop: HazeState,
    modifier: Modifier = Modifier,
) {
    val searching = selected == AppTab.Search
    val progress by animateFloatAsState(if (searching) 1f else 0f, Motion.liquid(), label = "searchMorph")

    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = 16.dp).height(BarHeight)) {
        val big = maxWidth - BarHeight - Gap
        val tabsWidth = lerp(big, BarHeight, progress).coerceIn(BarHeight, big)
        val searchWidth = lerp(BarHeight, big, progress).coerceIn(BarHeight, big)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(tabsWidth).fillMaxHeight().glass(backdrop, CapsuleShape)) {
                if (progress < 0.99f) {
                    TabsRow(
                        selectedIndex = tabs.indexOfFirst { it.tab == (if (searching) lastTab else selected) }.coerceAtLeast(0),
                        onSelect = { onSelect(tabs[it].tab) },
                        modifier = Modifier.graphicsLayer { alpha = (1f - progress * 1.8f).coerceIn(0f, 1f) },
                    )
                }
                if (progress > 0.01f) {
                    val item = tabs.first { it.tab == lastTab }
                    val label = stringResource(item.label)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f) }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(lastTab) }
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(item.icon), contentDescription = null, tint = TikTheme.colors.primaryText, modifier = Modifier.size(26.dp))
                    }
                }
            }
            Spacer(Modifier.width(Gap))
            SearchPill(
                progress = progress,
                searching = searching,
                query = query,
                onQueryChange = onQueryChange,
                onOpen = { onSelect(AppTab.Search) },
                backdrop = backdrop,
                modifier = Modifier.width(searchWidth).fillMaxHeight(),
            )
        }
    }
}

/**
 * The tabs, with a pill behind the selected one. While a finger is down, the pill follows it and,
 * on Android 13 and newer, turns into a clear drop of glass that bends the icons under it.
 */
@Composable
private fun TabsRow(selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = TikTheme.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val feedback = rememberFeedback()

    var width by remember { mutableIntStateOf(0) }
    var height by remember { mutableIntStateOf(0) }
    val slot = if (width == 0) 0f else width.toFloat() / tabs.size
    val pillCenter = remember { Animatable(Float.NaN) }
    var pressing by remember { mutableStateOf(false) }
    var fingerX by remember { mutableFloatStateOf(0f) }
    var hoverIndex by remember { mutableIntStateOf(selectedIndex) }

    fun center(index: Int) = if (rtl) width - (index + 0.5f) * slot else (index + 0.5f) * slot
    fun indexAt(x: Float): Int {
        if (slot == 0f) return selectedIndex
        val fromStart = if (rtl) width - x else x
        return (fromStart / slot).toInt().coerceIn(0, tabs.lastIndex)
    }

    LaunchedEffect(selectedIndex, width, pressing) {
        if (width == 0 || pressing) return@LaunchedEffect
        if (pillCenter.value.isNaN()) pillCenter.snapTo(center(selectedIndex)) else pillCenter.animateTo(center(selectedIndex), Motion.liquid())
    }
    LaunchedEffect(fingerX, pressing) {
        if (!pressing || slot == 0f) return@LaunchedEffect
        pillCenter.animateTo(fingerX.coerceIn(slot / 2, width - slot / 2), spring(dampingRatio = 0.8f, stiffness = 1400f))
    }
    val lens by animateFloatAsState(if (pressing) 1f else 0f, Motion.effects(), label = "lens")
    val pillScale by animateFloatAsState(if (pressing) 1.1f else 1f, Motion.fastSpatial(), label = "pillScale")

    val barBackdrop = rememberHazeState()
    val pillWidth = with(density) { (slot - 8.dp.toPx()).coerceAtLeast(0f).toDp() }
    val lensWidth = pillWidth + 14.dp
    val lensHeight = with(density) { height.toDp() } + 10.dp

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged {
                width = it.width
                height = it.height
            }
            .pointerInput(rtl, width) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressing = true
                    fingerX = down.position.x
                    hoverIndex = indexAt(fingerX)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        fingerX = change.position.x
                        val index = indexAt(fingerX)
                        if (index != hoverIndex) {
                            hoverIndex = index
                            feedback.selection()
                        }
                        change.consume()
                    }
                    pressing = false
                    onSelect(indexAt(fingerX))
                }
            },
    ) {
        Box(Modifier.fillMaxSize().then(if (canBendLight) Modifier.hazeSource(barBackdrop) else Modifier)) {
            if (!pillCenter.value.isNaN()) {
                Box(
                    Modifier
                        .align(AbsoluteAlignment.CenterLeft)
                        .absoluteOffset { IntOffset((pillCenter.value - pillWidth.toPx() / 2).roundToInt(), 0) }
                        .width(pillWidth)
                        .fillMaxHeight()
                        .padding(vertical = 5.dp)
                        .graphicsLayer {
                            scaleX = pillScale
                            scaleY = pillScale
                        }
                        .background(if (colors.dark) Color.White.copy(alpha = 0.13f) else Color.Black.copy(alpha = 0.07f), CapsuleShape),
                )
            }
            Row(Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, item ->
                    TabButton(item, selected = index == selectedIndex, onClick = { onSelect(index) }, modifier = Modifier.weight(1f))
                }
            }
        }
        if (canBendLight && lens > 0.01f && !pillCenter.value.isNaN()) {
            val lensStyle = remember {
                GlassStyle.clear then GlassStyle {
                    shape(CapsuleShape)
                    optics(
                        refractionStrength = 0.9f,
                        refractionHeightFraction = 0.42f,
                        refractionDisplacement = 16.dp,
                        depth = 0f,
                        blurRadius = 0.5.dp,
                    )
                    tint(Color.White.copy(alpha = 0.05f))
                    specularIntensity(0.9f)
                }
            }
            val input = remember(barBackdrop) { HazeInput.Sources(barBackdrop) }
            Box(
                Modifier
                    .align(AbsoluteAlignment.CenterLeft)
                    .absoluteOffset { IntOffset((pillCenter.value - lensWidth.toPx() / 2).roundToInt(), 0) }
                    .size(lensWidth, lensHeight)
                    .graphicsLayer {
                        alpha = lens
                        scaleX = 0.85f + 0.15f * lens
                        scaleY = 0.85f + 0.15f * lens
                    }
                    .hazeGlass(input = input, style = lensStyle),
            )
        }
    }
}

@Composable
private fun TabButton(item: TabItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TikTheme.colors
    val tint = if (selected) colors.accent else colors.primaryText.copy(alpha = 0.85f)
    Column(
        modifier
            .fillMaxHeight()
            .testTag(item.tag)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                onClick {
                    onClick()
                    true
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painterResource(if (selected) item.selectedIcon else item.icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Text(
            stringResource(item.label),
            style = TikTheme.type.caption2.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = tint,
            maxLines = 1,
        )
    }
}

/** The round Search button, which stretches into the search field. */
@Composable
private fun SearchPill(
    progress: Float,
    searching: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpen: () -> Unit,
    backdrop: HazeState,
    modifier: Modifier = Modifier,
) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val searchLabel = stringResource(R.string.search)

    LaunchedEffect(searching) {
        if (searching) {
            runCatching { focus.requestFocus() }
            keyboard?.show()
        } else {
            focusManager.clearFocus()
        }
    }

    Box(
        modifier
            .glass(backdrop, CapsuleShape)
            .clickable(
                enabled = !searching,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpen,
            )
            .testTag("tab-search")
            .semantics {
                role = Role.Tab
                selected = searching
                contentDescription = searchLabel
            },
    ) {
        if (progress < 0.5f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - progress * 2f }, contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(26.dp))
            }
        }
        // Always there (only hidden), so the field can take the keyboard as soon as Search opens.
        Row(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = ((progress - 0.3f) / 0.7f).coerceIn(0f, 1f) }
                .padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).focusRequester(focus).testTag("searchField"),
                singleLine = true,
                enabled = searching,
                textStyle = type.body.copy(color = colors.primaryText),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(stringResource(R.string.search_prompt), style = type.body, color = colors.tertiaryText, maxLines = 1)
                        }
                        field()
                    }
                },
            )
            if (query.isNotEmpty()) {
                val clear = stringResource(R.string.clear)
                Box(
                    Modifier
                        .size(44.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onQueryChange("") }
                        .semantics { contentDescription = clear },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(22.dp).background(colors.fill, CapsuleShape), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
