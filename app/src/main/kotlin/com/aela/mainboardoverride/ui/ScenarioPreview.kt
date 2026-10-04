package com.aela.mainboardoverride.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aela.mainboardoverride.R

/**
 * Free-mode selection preview: the equipped board-skin preview stretched under
 * the baked 640 x 400 seed-42 overlay (grid, start/exit, firewalls, buffs and
 * daemon). Fully static — no generation, spinner, cache or failure state, and
 * the artwork never reveals hidden honeypots.
 *
 * Geometry provenance: `assets/scenarios/generate_overlays.py` reads
 * `game-domain/src/test/resources/overlay-geometry-seed42.json`, which
 * `PreviewOverlayGeometryTest` keeps in sync with `LevelGenerator`.
 */
@Composable
internal fun ScenarioPreview(
    scenarioId: String,
    boardSkin: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        // Match the overlay's 640:400 aspect so both layers align exactly, the
        // way Image's Fit scaled the old bitmap inside the same constraints.
        Box(
            Modifier.matchParentSize()
                .aspectRatio(640f / 400f)
                .background(Color(0xFF091419)),
        ) {
            val board = boardSkinResource(boardSkin, previewOnly = true)
            if (board != null) {
                Image(
                    painterResource(board),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
            }
            Image(
                painterResource(scenarioOverlayResource(scenarioId)),
                contentDescription = stringResource(R.string.puzzle_preview),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}
