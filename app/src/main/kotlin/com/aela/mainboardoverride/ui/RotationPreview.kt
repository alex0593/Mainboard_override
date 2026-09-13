package com.aela.mainboardoverride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.Domino
import com.aela.mainboardoverride.domain.Orientation

/** Selected tile preview shared by normal play and tutorial rotation. */
@Composable
internal fun RotationPreview(tile: Domino, rotation: Int, skin: String, modifier: Modifier = Modifier) {
    val preview = if (rotation >= 2) tile.rotated() else tile
    val orientation = if (rotation % 2 == 0) Orientation.HORIZONTAL else Orientation.VERTICAL
    Surface(modifier.width(96.dp).height(126.dp), shape = RoundedCornerShape(10.dp),
        color = Panel.copy(alpha = .98f), border = BorderStroke(1.dp, Cyan.copy(alpha = .8f))) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(if (orientation == Orientation.HORIZONTAL) R.string.horizontal else R.string.vertical),
                color = Cyan, fontSize = 10.sp)
            DominoImage(preview, Modifier.width(if (orientation == Orientation.HORIZONTAL) 68.dp else 34.dp),
                orientation, skin = skin)
            Text("${preview.first} : ${preview.second}", color = Terminal, fontSize = 11.sp)
        }
    }
}
