package com.aela.mainboardoverride.ui

import com.aela.mainboardoverride.R

internal fun boardSkinResource(skin: String, previewOnly: Boolean = false): Int? = when (skin) {
    "obsidian" -> if (previewOnly) R.drawable.board_obsidian_preview else R.drawable.board_obsidian
    "ceramic" -> if (previewOnly) R.drawable.board_ceramic_preview else R.drawable.board_ceramic
    "pcb" -> if (previewOnly) R.drawable.board_pcb_preview else R.drawable.board_pcb
    "blueprint" -> if (previewOnly) R.drawable.board_blueprint_preview else R.drawable.board_blueprint
    "industrial" -> if (previewOnly) R.drawable.board_industrial_preview else R.drawable.board_industrial
    "rust" -> if (previewOnly) R.drawable.board_rust_preview else R.drawable.board_rust
    "ice" -> if (previewOnly) R.drawable.board_ice_preview else R.drawable.board_ice
    "graphite" -> if (previewOnly) R.drawable.board_graphite_preview else R.drawable.board_graphite
    "signal" -> if (previewOnly) R.drawable.board_signal_preview else R.drawable.board_signal
    "copper" -> if (previewOnly) R.drawable.board_copper_preview else R.drawable.board_copper
    "aurora" -> if (previewOnly) R.drawable.board_aurora_preview else R.drawable.board_aurora
    else -> null
}
