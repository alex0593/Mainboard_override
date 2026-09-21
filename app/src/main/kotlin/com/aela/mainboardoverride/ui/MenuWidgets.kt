package com.aela.mainboardoverride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.R

@Composable
private fun InfoScreen(onBack: () -> Unit) {
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            Text("// ${stringResource(R.string.help)}", color = Terminal, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            ProtocolContent()
            Spacer(Modifier.height(22.dp))
            SmallButton(stringResource(R.string.back), onBack)
        }
    }
}

@Composable
private fun StatusBox(label: String, value: String, color: Color, wide: Boolean = false) {
    Column(
        Modifier.width(if (wide) 88.dp else 64.dp).height(42.dp)
            .background(Void.copy(alpha = .65f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = .38f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Muted, fontSize = 8.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        Text(value, color = color, fontSize = if (wide) 9.sp else 13.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

@Composable
internal fun TerminalButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true) {
    MenuArtworkButton(label, onClick, modifier = modifier, primary = primary, enabled = enabled)
}

@Composable
internal fun LevelActionButton(label: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.size(48.dp).pressFeedback(interaction, label = "level action").semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))) {
          Image(painterResource(R.drawable.menu_button_compact_v2), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.FillBounds)
        }
        androidx.compose.material3.IconButton(onClick = onClick, modifier = Modifier.matchParentSize(), interactionSource = interaction) {
            androidx.compose.material3.Icon(painterResource(icon), contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun SmallButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, minHeight: Dp? = null) {
    MenuArtworkButton(label, onClick, modifier = modifier, compact = true, minHeight = minHeight)
}

@Composable
private fun StatusModule(label: String, tint: Color, topic: HelpTopic, onHelp: (HelpTopic) -> Unit, visible: Boolean = true, detail: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.background(Void.copy(alpha = .7f), RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, color = tint, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            detail()
        }
        HelpButton(topic, onHelp, visible)
    }
}

@Composable
private fun PanelHeading(label: String, topic: HelpTopic, onHelp: (HelpTopic) -> Unit, visible: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(3.dp, 14.dp).background(Cyan))
        Text(label, color = Cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        HorizontalDivider(Modifier.weight(1f), color = Cyan.copy(alpha = .25f))
        HelpButton(topic, onHelp, visible)
    }
}

@Composable
private fun ProtocolContent() {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE7FFF2)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            HelpTopic.entries.forEach { topic ->
                Text(stringResource(topic.title), color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                HelpBody(topic, darkText = true)
            }
        }
    }
}
