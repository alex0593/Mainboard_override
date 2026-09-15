package com.aela.mainboardoverride.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.aela.mainboardoverride.R

/** Resolves scenario artwork independently of the player's board and domino skins. */
@DrawableRes
internal fun scenarioBackgroundResource(id: String): Int = when (id) {
    "lab" -> R.drawable.scenario_lab
    "data" -> R.drawable.scenario_data
    "industry" -> R.drawable.scenario_industry
    "archive" -> R.drawable.scenario_archive
    "core" -> R.drawable.scenario_core
    "ghost" -> R.drawable.scenario_ghost
    else -> R.drawable.scenario_classic
}

/** Menu art follows the last free-play scenario while keeping a safe default. */
internal fun menuBackgroundResource(lastScenario: String): Int = scenarioBackgroundResource(lastScenario)

/** Accent used by transient in-game feedback for each free-play scenario. */
internal fun scenarioAccentColor(id: String): Color = when (id) {
    "lab" -> Color(0xFFB875FF)
    "data" -> Color(0xFF4DB8FF)
    "industry" -> Color(0xFFFF9D4D)
    "archive" -> Color(0xFFFFD166)
    "core" -> Color(0xFFFF5368)
    "ghost" -> Color(0xFF7CFFD4)
    else -> Color(0xFF39E7E0)
}
