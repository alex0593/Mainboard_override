package com.aela.mainboardoverride.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.R

/**
 * Shared shell for the secondary screens (menu, settings, tutorial home, store).
 *
 * @param header optional top art band (see [ArtHeader]).
 * @param bottom optional fixed-height bottom band. It is deliberately a slot: screens leave it
 *   empty today and it is the reserved place for the future non-intrusive ad banner, so adding
 *   ads later will not require touching any screen layout.
 * @param content the main area; it always gets the remaining height after header/bottom.
 */
@Composable
internal fun AppWindow(
    header: (@Composable () -> Unit)? = null,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        header?.invoke()
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        bottom?.invoke()
    }
}

/**
 * Art-backed header band: MetalTitle, optional counter/badges and the back action. This is the
 * shared chrome for every secondary screen (store, scenarios, challenges, tutorial, settings).
 */
@Composable
internal fun ArtHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    counter: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))) {
        Image(
            painter = painterResource(R.drawable.menu_header_v1),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MetalTitle(title, Modifier.weight(1f), fontSize = 20.sp)
            counter?.let { Text(it, color = Warning, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
            trailing?.invoke()
            if (onBack != null) {
                MenuArtworkButton(
                    stringResource(R.string.back), onBack, compact = true, fillWidth = false,
                    modifier = Modifier.widthIn(min = 92.dp).heightIn(min = 48.dp),
                )
            }
        }
    }
}