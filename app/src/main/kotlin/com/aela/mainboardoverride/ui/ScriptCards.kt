package com.aela.mainboardoverride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.Domino
import com.aela.mainboardoverride.domain.Orientation
import com.aela.mainboardoverride.domain.ScriptCard
import com.aela.mainboardoverride.domain.ScriptType

@Composable
internal fun ScriptCardView(card: ScriptCard, selected: Boolean, enabled: Boolean, highlighted: Boolean = false, compact: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.then((if (compact) Modifier.width(88.dp) else Modifier.fillMaxWidth())
            .heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).testTag("script-${card.id}")
            .semantics { this.selected = selected }.alpha(if (enabled) 1f else .4f)
            .pressable(enabled = enabled, role = Role.Button, label = "script card", onClick = onClick)
            .border(1.dp, if (highlighted) Warning else if (selected) Cyan else Muted, RoundedCornerShape(8.dp))),
    ) {
        Image(
            painter = painterResource(R.drawable.menu_card_v1),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
        )
        if (selected) Box(Modifier.matchParentSize().background(Cyan.copy(alpha = .2f)))
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            scriptArtwork(card.type)?.let { artwork ->
                Image(
                    painter = painterResource(artwork),
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 26.dp else 34.dp),
                )
            }
            Column {
                Text(if (card.type == ScriptType.KILL_PROCESS) "KILL" else card.type.name, color = Cyan, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${card.type.ramCost}R · +${card.type.traceNoise}", color = Terminal, fontSize = 10.sp, lineHeight = 12.sp)
            }
        }
    }
}

private fun scriptArtwork(type: ScriptType): Int? = when (type) {
    ScriptType.PING -> R.drawable.script_card_ping
    ScriptType.SPOOF -> R.drawable.script_card_spoof
    ScriptType.KILL_PROCESS -> R.drawable.script_card_kill
    ScriptType.BRIDGE -> R.drawable.script_card_bridge
    ScriptType.STEALTH -> R.drawable.script_card_stealth
    else -> null
}

@Composable
internal fun DominoView(tile: Domino, selected: Boolean, skin: String = "kenney", highlighted: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val label = stringResource(R.string.domino_description, tile.first, tile.second)
    Row(
        modifier.widthIn(min = 42.dp).heightIn(min = 64.dp).semantics { contentDescription = label; this.selected = selected }.background(if (selected) Terminal.copy(alpha = .2f) else Void)
            .border(1.dp, if (highlighted) Warning else if (selected) Terminal else Muted).pressable(role = Role.Button, label = "hand tile", onClick = onClick).padding(3.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
            DominoImage(tile, Modifier.height(64.dp).width(32.dp), Orientation.VERTICAL, describe = false, skin = skin)
    }
}
