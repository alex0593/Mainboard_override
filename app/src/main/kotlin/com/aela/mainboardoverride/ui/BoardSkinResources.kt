package com.aela.mainboardoverride.ui

import com.aela.mainboardoverride.R

internal fun boardSkinResource(skin: String, previewOnly: Boolean = false): Int? = when (skin) {
    "obsidian" -> if (previewOnly) R.drawable.board_obsidian_preview else R.drawable.board_obsidian
    "ceramic" -> if (previewOnly) R.drawable.board_ceramic_preview else R.drawable.board_ceramic
    "titanium" -> if (previewOnly) R.drawable.board_titanium_preview else R.drawable.board_titanium
    "jade" -> if (previewOnly) R.drawable.board_jade_preview else R.drawable.board_jade
    "ruby" -> if (previewOnly) R.drawable.board_ruby_preview else R.drawable.board_ruby
    "sapphire" -> if (previewOnly) R.drawable.board_sapphire_preview else R.drawable.board_sapphire
    "amber" -> if (previewOnly) R.drawable.board_amber_preview else R.drawable.board_amber
    "amethyst" -> if (previewOnly) R.drawable.board_amethyst_preview else R.drawable.board_amethyst
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
