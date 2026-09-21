package com.aela.mainboardoverride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.aela.mainboardoverride.domain.TutorialInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.Domino

@Composable
internal fun PingPreview(tiles: List<Domino>, skin: String) {
    Column {
        Text(stringResource(R.string.ping_preview), color = Cyan)
        tiles.forEach { tile -> RotationPreview(tile, rotation = 0, skin = skin) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpoofDialog(tile: Domino, half: Int, value: Int, error: Int?,
    onHalf: (Int) -> Unit, onValue: (Int) -> Unit, onApply: () -> Unit, onCancel: () -> Unit,
    tutorialExpected: TutorialInput? = null) {
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onCancel,
        properties = PopupProperties(focusable = false),
    ) {
      Surface(
          Modifier.widthIn(max = 320.dp).fillMaxWidth(.9f).heightIn(max = 300.dp).padding(6.dp),
          shape = RoundedCornerShape(14.dp), color = Panel.copy(alpha = .98f),
          border = BorderStroke(1.dp, Cyan.copy(alpha = .8f)),
      ) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.spoof_value), color = Cyan, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.spoof_before), Modifier.weight(1f), fontSize = 12.sp)
                        DominoImage(tile, Modifier.width(52.dp))
                    }
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.spoof_after), Modifier.weight(1f), fontSize = 12.sp)
                        DominoImage(if (half == 0) tile.copy(first = value) else tile.copy(second = value), Modifier.width(52.dp))
                    }
                }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(R.string.first_half, R.string.second_half).forEachIndexed { index, label ->
                        val halfInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(onClick = { onHalf(index) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .testTag("tutorial-half-$index")
                            .pressFeedback(halfInteraction, label = "spoof half")
                            .then(if (tutorialExpected == TutorialInput.Half(index)) Modifier.border(2.dp, Terminal) else Modifier)
                            .semantics { selected = half == index },
                            interactionSource = halfInteraction,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
                            Text(stringResource(label), fontSize = 12.sp)
                        }
                    }
                }
            Text(stringResource(R.string.spoof_scroll_hint), color = Cyan, fontSize = 10.sp, maxLines = 1)
            val valueList = rememberLazyListState(initialFirstVisibleItemIndex = value.coerceIn(0, 6))
            LazyColumn(
                    state = valueList,
                    modifier = Modifier.fillMaxWidth().height(72.dp).testTag("spoof-values").border(1.dp, Cyan.copy(alpha = .35f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items((0..6).toList()) { candidate ->
                        val valueInteraction = remember { MutableInteractionSource() }
                        TextButton(
                            onClick = { onValue(candidate) },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("tutorial-value-$candidate")
                                .pressFeedback(valueInteraction, label = "spoof value", pressedScale = .96f)
                                .then(if (tutorialExpected == TutorialInput.Value(candidate)) Modifier.border(2.dp, Terminal) else Modifier)
                                .semantics { selected = value == candidate },
                            interactionSource = valueInteraction,
                        ) { Text("$candidate", color = if (value == candidate) Terminal else Muted, fontSize = 18.sp, maxLines = 1) }
                    }
            }
            error?.let { Text(stringResource(it), color = Danger, maxLines = 2, fontSize = 10.sp) }
          }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val cancelInteraction = remember { MutableInteractionSource() }
                val applyInteraction = remember { MutableInteractionSource() }
                TextButton(onClick = onCancel, interactionSource = cancelInteraction, modifier = Modifier.weight(1f).height(48.dp)
                    .testTag(if (tutorialExpected != null) "tutorial-action-Cancel" else "spoof-cancel")
                    .pressFeedback(cancelInteraction, label = "spoof cancel")
                    .then(if (tutorialExpected == TutorialInput.Cancel) Modifier.border(2.dp, Terminal) else Modifier)) { Text(stringResource(R.string.cancel)) }
                TextButton(onClick = onApply, interactionSource = applyInteraction, modifier = Modifier.weight(1f).height(48.dp)
                    .testTag(if (tutorialExpected != null) "tutorial-action-ApplySpoof" else "spoof-apply")
                    .pressFeedback(applyInteraction, label = "spoof apply")
                    .then(if (tutorialExpected == TutorialInput.ApplySpoof) Modifier.border(2.dp, Terminal) else Modifier)) { Text(stringResource(R.string.apply)) }
            }
        }
      }
    }
}

@Composable
internal fun BridgeControl(horizontal: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, highlighted: Boolean = false, tag: String = "bridge-orientation") {
    GameControlButton(stringResource(if (horizontal) R.string.horizontal else R.string.vertical), onToggle,
        modifier.testTag(tag), enabled = enabled, highlighted = highlighted)
}
