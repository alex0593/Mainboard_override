package com.aela.mainboardoverride.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aela.mainboardoverride.R
import kotlin.math.cos
import kotlin.math.sin

/** Shared chrome; only the body scrolls, keeping the title and actions reachable. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GameDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Cyan,
    // Rejections shake in with a decaying jitter and an alpha stutter; motion settings drop it.
    glitch: Boolean = false,
    actions: @Composable FlowRowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as DialogWindowProvider).window
            val unbind = window.bindGameImmersion()
            onDispose { unbind() }
        }
        val glitchProgress = remember { Animatable(0f) }
        val reducedMotion = LocalReducedMotion.current
        LaunchedEffect(glitch) {
            if (glitch && !reducedMotion) {
                glitchProgress.snapTo(1f)
                glitchProgress.animateTo(0f, tween(340, easing = LinearEasing))
            }
        }
        BoxWithConstraints(modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp).widthIn(max = 620.dp).fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp.dp - 48.dp).coerceAtLeast(160.dp))) {
            val horizontalInset = maxOf(40.dp, maxWidth * .08f)
            val verticalInset = 24.dp
            Column(Modifier.graphicsLayer {
                val g = glitchProgress.value
                if (g > 0f) {
                    translationX = sin(g * 38f) * 12f * g
                    translationY = cos(g * 51f) * 3f * g
                    alpha = if (g > .55f || (g * 12).toInt() % 2 == 0) 1f else .88f
                }
            }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
