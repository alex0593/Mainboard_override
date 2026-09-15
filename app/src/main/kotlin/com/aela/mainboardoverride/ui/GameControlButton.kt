package com.aela.mainboardoverride.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.R

/** A compact surface inside a separate, non-overlapping 48dp touch target. */
@Composable
internal fun GameControlButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, highlighted: Boolean = false) {
    Box(modifier.fillMaxWidth().requiredHeight(52.dp)
        .pressable(enabled = enabled, role = Role.Button, label = "game control", onClick = onClick)
        .alpha(if (enabled) 1f else .45f), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().heightIn(min = 32.dp)
            .then(if (highlighted) Modifier.border(1.dp, Warning, RoundedCornerShape(8.dp)) else Modifier),
            contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.menu_button_compact_v2), null,
                Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
            Text(label, Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                color = Cyan, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}
