package com.aela.mainboardoverride.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.domain.GameState
import com.aela.mainboardoverride.domain.MAX_RAM

/** Shared artwork, status indicators and script band for matches and tutorial practice. */
@Composable
internal fun GameHeader(
    game: GameState,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    scripts: @Composable RowScope.() -> Unit,
) {
    Box(modifier.heightIn(min = 64.dp).testTag("game-header").clip(RoundedCornerShape(10.dp))) {
        Image(painterResource(R.drawable.menu_header_v1), null, Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds)
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 18.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.turn, game.turn), color = Cyan, fontSize = 11.sp)
                Text(stringResource(R.string.ram, game.ram, MAX_RAM), color = Terminal, fontSize = 11.sp)
                Text(stringResource(R.string.trace, game.trace),
                    color = if (game.trace >= 80) Danger else Warning, fontSize = 11.sp)
            }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).testTag("script-hand"),
                horizontalArrangement = Arrangement.spacedBy(4.dp), content = scripts)
            trailing()
        }
    }
}
