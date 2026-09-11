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
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(modifier.padding(24.dp).widthIn(max = 620.dp).fillMaxWidth()
            .heightIn(max = (LocalConfiguration.current.screenHeightDp.dp - 48.dp).coerceAtLeast(160.dp))) {
            // Keep body copy away from the decorative frame, especially on wide dialogs.
            val horizontalInset = maxOf(40.dp, maxWidth * .08f)
            val verticalInset = maxOf(36.dp, maxHeight * .10f)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Panel.copy(alpha = .96f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, accent.copy(alpha = .55f)),
            ) {
              Box {
                Image(
                    painter = painterResource(R.drawable.menu_card_v1),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                )
                Column(Modifier.padding(horizontal = horizontalInset, vertical = verticalInset), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, color = accent, style = MaterialTheme.typography.titleLarge)
                Column(
                    Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
                HorizontalDivider(color = accent.copy(alpha = .18f))
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = actions,
                )
                }
              }
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
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag("cancel-dialog")) {
                Text(stringResource(R.string.cancel))
            }
            Button(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp).testTag("confirm-dialog")) {
                Text(confirmLabel)
            }
        },
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
