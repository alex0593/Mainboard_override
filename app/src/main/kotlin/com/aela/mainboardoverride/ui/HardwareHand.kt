package com.aela.mainboardoverride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.Domino

/** The hand wraps its tiles rather than stretching each tile to fill the panel. */
@Composable
internal fun HardwareHand(tiles: List<Domino>, selected: String?, skin: String,
    modifier: Modifier = Modifier, highlighted: String? = null, tagPrefix: String = "game-tile",
    onSelect: (String) -> Unit) {
    Surface(modifier.height(IntrinsicSize.Min).testTag("hardware-panel"), shape = RoundedCornerShape(8.dp),
        color = Panel, border = BorderStroke(1.dp, Cyan.copy(alpha = .4f))) {
        Column(Modifier.padding(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.dominoes), color = Cyan, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                tiles.forEach { tile ->
                    DominoView(tile, selected == tile.id, skin, highlighted == tile.id,
                        Modifier.width(48.dp).testTag("$tagPrefix-${tile.id}")) { onSelect(tile.id) }
                }
            }
        }
    }
}
