package com.aela.mainboardoverride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aela.mainboardoverride.R

/** Shared chrome; only the body scrolls, keeping the title and actions reachable. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GameDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Cyan,
    actions: @Composable FlowRowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(modifier.padding(24.dp).widthIn(max = 620.dp).fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp.dp - 48.dp).coerceAtLeast(160.dp))) {
            val horizontalInset = maxOf(40.dp, maxWidth * .08f)
            val verticalInset = 24.dp
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetalTitle(title, Modifier.padding(horizontal = 10.dp))
                Surface(Modifier.weight(1f, fill = false).fillMaxWidth().testTag("dialog-body"), shape = RoundedCornerShape(20.dp), color = Panel.copy(alpha = .96f), contentColor = MaterialTheme.colorScheme.onSurface, border = BorderStroke(1.dp, accent.copy(alpha = .55f))) {
                    Box {
                        Image(painterResource(R.drawable.menu_card_v1), null, Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.FillBounds)
                        Column(Modifier.padding(horizontal = horizontalInset, vertical = verticalInset)
                            .fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
                    }
                }
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp).testTag("dialog-actions"), horizontalArrangement = Arrangement.spacedBy(10.dp, androidx.compose.ui.Alignment.End), verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
            }
        }
    }
}

@Composable
internal fun ConfirmGameDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    GameDialog(
        title = title,
        onDismiss = onDismiss,
        modifier = Modifier.testTag("confirmation-dialog"),
        accent = Warning,
        actions = {
            MenuArtworkButton(stringResource(R.string.cancel), onDismiss, compact = true, fillWidth = false, modifier = Modifier.testTag("cancel-dialog"))
            MenuArtworkButton(confirmLabel, onConfirm, compact = true, primary = true, fillWidth = false, modifier = Modifier.testTag("confirm-dialog"))
        },
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
