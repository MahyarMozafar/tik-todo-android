package com.mahyarmozafar.tik.ui.lists

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.mahyarmozafar.tik.R
import com.mahyarmozafar.tik.model.AccentChoice
import com.mahyarmozafar.tik.model.ListIcon
import com.mahyarmozafar.tik.ui.LocalAppData
import com.mahyarmozafar.tik.ui.LocalModel
import com.mahyarmozafar.tik.ui.components.SectionTitle
import com.mahyarmozafar.tik.ui.components.SheetScaffold
import com.mahyarmozafar.tik.ui.components.drawable
import com.mahyarmozafar.tik.ui.components.rememberFeedback
import com.mahyarmozafar.tik.ui.components.softGradient
import com.mahyarmozafar.tik.ui.components.title
import com.mahyarmozafar.tik.ui.glass.card
import com.mahyarmozafar.tik.ui.navigation.Route
import com.mahyarmozafar.tik.ui.navigation.navigator
import com.mahyarmozafar.tik.ui.theme.Motion
import com.mahyarmozafar.tik.ui.theme.TikTheme
import com.mahyarmozafar.tik.ui.theme.system
import kotlinx.coroutines.delay

/** Name, color and icon for a list. */
@Composable
fun ListEditorScreen(listId: String?) {
    val data = LocalAppData.current
    val model = LocalModel.current
    val navigator = navigator
    val colors = TikTheme.colors
    val type = TikTheme.type
    val existing = listId?.let { data.list(it) }

    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: ListIcon.List) }
    var color by rememberSaveable { mutableStateOf(existing?.color ?: AccentChoice.Blue) }
    val focus = remember { FocusRequester() }
    val chosen = color.system.color(colors.dark)
    val route = Route.ListEditor(listId)

    LaunchedEffect(Unit) {
        if (existing == null) {
            delay(350)
            runCatching { focus.requestFocus() }
        }
    }

    SheetScaffold(
        title = stringResource(if (existing == null) R.string.new_list else R.string.edit_list),
        onClose = { navigator.close(route) },
        onSave = {
            model.launch { saveList(existing, name, icon, color) }
            navigator.close(route)
        },
        saveEnabled = name.isNotBlank(),
    ) {
        Column(
            Modifier.fillMaxWidth().card().padding(vertical = 22.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .dropShadow(CircleShape, Shadow(radius = 14.dp, color = chosen, offset = DpOffset(0.dp, 6.dp), alpha = 0.35f))
                    .background(chosen.softGradient(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon.drawable), contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(16.dp))
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = type.title3.copy(color = colors.primaryText, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(chosen),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.fill, RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp, horizontal = 12.dp)
                    .focusRequester(focus)
                    .testTag("listNameField"),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.Center) {
                        if (name.isEmpty()) {
                            Text(stringResource(R.string.list_name), style = type.title3, color = colors.tertiaryText, textAlign = TextAlign.Center)
                        }
                        field()
                    }
                },
            )
        }

        SectionTitle(stringResource(R.string.color))
        ChoiceGrid(AccentChoice.entries, Modifier.card().padding(12.dp)) { choice ->
            val choiceColor = choice.system.color(colors.dark)
            ColorDot(choiceColor, selected = choice == color, label = stringResource(choice.title)) { color = choice }
        }

        SectionTitle(stringResource(R.string.icon))
        ChoiceGrid(ListIcon.entries, Modifier.card().padding(12.dp)) { item ->
            val selected = item == icon
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(3.dp)
                    .background(if (selected) chosen.softGradient() else SolidColor(colors.fill), CircleShape)
                    .clickable(interactionSource = interaction, indication = null, role = Role.RadioButton) { icon = item }
                    .semantics { this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(item.drawable),
                    contentDescription = null,
                    tint = if (selected) Color.White else colors.primaryText,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

/** Items in rows of six. On wide screens the cells keep their size and the gaps grow, like on iPad. */
@Composable
fun <T> ChoiceGrid(items: List<T>, modifier: Modifier = Modifier, columns: Int = 6, cell: @Composable (T) -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { item ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(Modifier.widthIn(max = 52.dp)) { cell(item) }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A round color swatch with a ring around the chosen one. */
@Composable
fun ColorDot(color: Color, selected: Boolean, label: String, onClick: () -> Unit) {
    val feedback = rememberFeedback()
    val scale by animateFloatAsState(if (selected) 1f else 0.86f, Motion.fastSpatial(), label = "dot")
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(if (selected) Modifier.border(2.5.dp, color, CircleShape) else Modifier)
            .padding(4.dp)
            .scale(scale)
            .background(color.softGradient(), CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) {
                feedback.selection()
                onClick()
            }
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
