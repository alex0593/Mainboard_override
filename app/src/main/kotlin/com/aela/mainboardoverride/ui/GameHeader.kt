package com.aela.mainboardoverride.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.launch

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
                Text(stringResource(R.string.turn, game.turn), color = Cyan, fontSize = 11.sp, lineHeight = 13.sp)
                Text(stringResource(R.string.ram, game.ram, MAX_RAM), color = Terminal, fontSize = 11.sp, lineHeight = 13.sp)
                TraceIndicator(game.trace)
            }
            Box(
                Modifier.width(1.dp).height(44.dp)
                    .background(Cyan.copy(alpha = .58f))
                    .testTag("header-divider"),
            )
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).testTag("script-hand"),
                horizontalArrangement = Arrangement.spacedBy(4.dp), content = scripts)
            trailing()
        }
    }
}

@Composable
private fun TraceIndicator(trace: Int) {
    var previousTrace by remember { mutableIntStateOf(trace) }
    var increase by remember { mutableIntStateOf(0) }
    val rise = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    val traceColor = if (trace >= 80) Danger else Warning

    LaunchedEffect(trace) {
        val delta = trace - previousTrace
        previousTrace = trace
        if (delta > 0) {
            increase = delta
            rise.snapTo(0f)
            fade.snapTo(1f)
            launch {
                fade.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
            }
            rise.animateTo(-24f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    Box(contentAlignment = Alignment.CenterStart) {
        Text(
            stringResource(R.string.trace, trace),
            color = traceColor,
            fontSize = 11.sp,
            lineHeight = 13.sp,
        )
        if (fade.value > 0f) {
            Text(
                stringResource(R.string.trace_delta, increase),
                color = traceColor,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 34.dp, y = rise.value.dp)
                    .alpha(fade.value)
                    .testTag("trace-delta"),
            )
        }
    }
}
