package com.aela.mainboardoverride.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.ScriptType

internal enum class HelpTopic(val title: Int, val body: Int, val script: ScriptType? = null) {
    BOARD(R.string.help_board_title, R.string.help_board_body),
    RAM(R.string.help_ram_title, R.string.help_ram_body),
    TRACE(R.string.help_trace_title, R.string.help_trace_body),
    TURN(R.string.help_turn_title, R.string.help_turn_body),
    NOISE(R.string.help_noise_title, R.string.help_noise_body),
    SEED(R.string.help_seed_title, R.string.help_seed_body),
    SCRIPTS(R.string.scripts, R.string.help_scripts_body),
    HARDWARE(R.string.dominoes, R.string.help_hardware_body),
    ROTATE(R.string.rotate, R.string.help_rotate_body),
    END_TURN(R.string.end_turn, R.string.help_end_turn_body),
    PING(R.string.help_ping_title, R.string.help_ping_body, ScriptType.PING),
    SPOOF(R.string.help_spoof_title, R.string.help_spoof_body, ScriptType.SPOOF),
    KILL(R.string.help_kill_title, R.string.help_kill_body, ScriptType.KILL_PROCESS),
    BRIDGE(R.string.help_bridge_title, R.string.help_bridge_body, ScriptType.BRIDGE),
}

internal fun ScriptType.helpTopic(): HelpTopic = when (this) {
    ScriptType.PING -> HelpTopic.PING
    ScriptType.SPOOF -> HelpTopic.SPOOF
    ScriptType.KILL_PROCESS -> HelpTopic.KILL
    ScriptType.BRIDGE -> HelpTopic.BRIDGE
}

@Composable
internal fun HelpButton(topic: HelpTopic, onHelp: (HelpTopic) -> Unit, visible: Boolean = true) {
    if (!visible && topic != HelpTopic.BOARD) return
    val label = stringResource(R.string.help_description, stringResource(topic.title))
    TextButton(onClick = { onHelp(topic) }, modifier = Modifier.size(48.dp).semantics { contentDescription = label }) {
        Text("?", color = Cyan)
    }
}

@Composable
internal fun HelpBody(topic: HelpTopic, darkText: Boolean = false) {
    Column {
        Text(stringResource(topic.body), color = if (darkText) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.Unspecified)
        topic.script?.let { Text(stringResource(R.string.help_script_cost, it.ramCost, it.traceNoise), color = if (darkText) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.Unspecified) }
    }
}

@Composable
internal fun HelpDialog(topic: HelpTopic, onClose: () -> Unit) {
    // Resolve text in the game context before Dialog creates its own window context.
    val title = stringResource(topic.title)
    val body = stringResource(topic.body)
    val cost = topic.script?.let { stringResource(R.string.help_script_cost, it.ramCost, it.traceNoise) }
    val close = stringResource(R.string.help_close)
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(body)
                cost?.let { Text(it) }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(close) } },
    )
}
