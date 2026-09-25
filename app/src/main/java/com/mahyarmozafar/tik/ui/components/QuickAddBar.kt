package com.mahyarmozafar.tik.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.ui.glass.CapsuleShape
import com.mahyarmozafar.tik.ui.glass.glass
import com.mahyarmozafar.tik.ui.theme.Motion
import com.mahyarmozafar.tik.ui.theme.TikTheme
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

/**
 * The round glass + button. Tapping it melts it into a text field, so a task can be typed and
 * added in one go. The field stays open after adding, which makes typing a whole day's tasks
 * quick.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddBar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    backdrop: HazeState,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
    onShowDetails: ((String) -> Unit)? = null,
) {
    val colors = TikTheme.colors
    val type = TikTheme.type
    val feedback = rememberFeedback()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    var text by rememberSaveable { mutableStateOf("") }
    val trimmed = text.trim()

    fun close() {
        keyboard?.hide()
        onExpandedChange(false)
    }

    fun submit() {
        if (trimmed.isEmpty()) {
            close()
            return
        }
        onAdd(trimmed)
        feedback.added()
        text = ""
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(60)
            runCatching { focus.requestFocus() }
            keyboard?.show()
        } else {
            text = ""
        }
    }

    // Close again when the keyboard goes away with nothing typed.
    val imeVisible = WindowInsets.isImeVisible
    var sawKeyboard by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible, expanded) {
        if (!expanded) {
            sawKeyboard = false
        } else if (imeVisible) {
            sawKeyboard = true
        } else if (sawKeyboard) {
            delay(300)
            if (text.isBlank()) onExpandedChange(false)
        }
    }

    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.weight(1f),
            enter = expandHorizontally(Motion.liquid(), expandFrom = Alignment.End) + fadeIn(Motion.effects()),
            exit = shrinkHorizontally(Motion.spatial(), shrinkTowards = Alignment.End) + fadeOut(Motion.effects()),
        ) {
            Row(
                Modifier
                    .padding(end = 12.dp)
                    .height(54.dp)
                    // More frosted than the bars, so typed text stays easy to read over the list.
                    .glass(backdrop, CapsuleShape, tint = colors.glassFallback.copy(alpha = if (colors.dark) 0.8f else 0.85f))
                    .padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f).focusRequester(focus).testTag("quickAddField"),
                    singleLine = true,
                    textStyle = type.body.copy(color = colors.primaryText),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    decorationBox = { field ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(stringResource(R.string.new_task), style = type.body, color = colors.tertiaryText)
                            }
                            field()
                        }
                    },
                )
                if (onShowDetails != null) {
                    val label = stringResource(R.string.more_details)
                    Box(
                        Modifier
                            .size(40.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                val title = trimmed
                                close()
                                onShowDetails(title)
                            }
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_tune), contentDescription = null, tint = colors.secondaryText)
                    }
                }
            }
        }

        val canAdd = expanded && trimmed.isNotEmpty()
        val rotation by animateFloatAsState(if (expanded && !canAdd) 45f else 0f, Motion.fastSpatial(), label = "plusTurn")
        val label = stringResource(if (!expanded) R.string.new_task else if (canAdd) R.string.add else R.string.close)
        GlassCircle(
            backdrop = backdrop,
            onClick = {
                when {
                    !expanded -> onExpandedChange(true)
                    trimmed.isEmpty() -> close()
                    else -> submit()
                }
            },
            size = 54.dp,
            glassTint = colors.accent,
            contentDescription = label,
            modifier = Modifier.testTag("quickAddButton"),
        ) {
            AnimatedContent(
                targetState = canAdd,
                transitionSpec = { (scaleIn(Motion.fastSpatial()) + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "plusIcon",
            ) { arrow ->
                Icon(
                    painterResource(if (arrow) R.drawable.ic_arrow_up else R.drawable.ic_add),
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = if (arrow) 0f else rotation },
                )
            }
        }
    }
}
