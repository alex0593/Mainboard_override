package com.aela.mainboardoverride.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The free-mode selection previews are baked PNGs generated from this exact
 * geometry (fixed seed 42). If this test fails, LevelGenerator changed: rerun
 * the bootstrap below to refresh
 * src/test/resources/overlay-geometry-seed42.json, regenerate the artwork with
 * assets/scenarios/generate_overlays.py and re-export it with
 * tools/PrepareSkinAsset.java (see assets/scenarios/README.md).
 */
class PreviewOverlayGeometryTest {
    @Test
    fun bakedOverlayGeometryMatchesGenerator() {
        val file = File("src/test/resources/overlay-geometry-seed42.json")
        val dump = overlayGeometryDump()
        if (!file.isFile) {
            file.parentFile.mkdirs()
            file.writeText(dump)
            println("Materialized ${file.path}; re-run to verify.")
            return
        }
        assertEquals(file.readText().trim(), dump.trim())
    }
}

/**
 * Deterministic JSON consumed by assets/scenarios/generate_overlays.py.
 * Hidden honeypots and solutions are deliberately excluded: a preview must
 * never reveal a trap.
 */
private fun overlayGeometryDump(): String = buildString {
    val scenarios = ScenarioCatalog.all
    appendLine("[")
    scenarios.forEachIndexed { index, scenario ->
        val board = LevelGenerator.generateScenario(42, scenario.id).board
        val firewalls = board.firewalls
            .sortedWith(compareBy({ it.y }, { it.x }))
            .joinToString(",") { "[${it.x},${it.y}]" }
        val buffs = board.buffs
            .filterKeys { it !in board.collectedBuffs }
            .entries
            .sortedWith(compareBy({ it.key.y }, { it.key.x }))
            .joinToString(",") { (position, buff) ->
                """{"p":[${position.x},${position.y}],"k":"${buff.name}"}"""
            }
        val daemon = board.daemon?.let { "[${it.position.x},${it.position.y}]" } ?: "null"
        append("""{"id":"${scenario.id}","width":${board.width},"height":${board.height},""")
        append(""""start":[${board.start.x},${board.start.y}],"extraction":[${board.extraction.x},${board.extraction.y}],""")
        append(""""firewalls":[$firewalls],"buffs":[$buffs],"daemon":$daemon}""")
        appendLine(if (index < scenarios.lastIndex) "," else "")
    }
    appendLine("]")
}
