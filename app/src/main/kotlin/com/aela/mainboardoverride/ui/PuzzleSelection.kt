package com.aela.mainboardoverride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aela.mainboardoverride.GameUiState
import com.aela.mainboardoverride.MainViewModel
import com.aela.mainboardoverride.R
import com.aela.mainboardoverride.data.ChallengeRecord
import com.aela.mainboardoverride.domain.*

@Composable
private fun ScenarioCard(
    scenario: Scenario,
    unlocked: Boolean,
    completed: Boolean,
    completedChallenges: Int,
    boardSkin: String,
    dominoSkin: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val status = when {
        completed -> stringResource(R.string.scenario_completed)
        unlocked -> stringResource(R.string.scenario_available)
        else -> stringResource(R.string.locked_scenario)
    }
    val accessibilityDescription = stringResource(
        R.string.scenario_accessibility,
        stringResource(scenarioLabel(scenario.id)),
        status,
    )
    val interaction = remember { MutableInteractionSource() }
    Card(onClick = onClick, enabled = unlocked, interactionSource = interaction,
        // The visual checkmark and lock icon are not enough for TalkBack users.
        modifier = modifier.fillMaxWidth().heightIn(min = 190.dp)
            .pressFeedback(interaction, label = "scenario card", pressedScale = .97f)
            .semantics {
            contentDescription = accessibilityDescription
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (completed) Terminal else if (unlocked) Cyan.copy(alpha = .45f) else Muted),
        colors = CardDefaults.cardColors(containerColor = Panel)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 190.dp)) {
            Image(
                painterResource(scenarioBackgroundResource(scenario.id)),
                contentDescription = null,
                modifier = Modifier.matchParentSize().alpha(.72f),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(108.dp), contentAlignment = Alignment.Center) {
                    PuzzlePreview("scenario:${scenario.id}:42", boardSkin, dominoSkin, Modifier.fillMaxSize()) {
                        LevelGenerator.generateScenario(42, scenario.id).board
                    }
                    if (!unlocked) {
                        Box(Modifier.fillMaxSize().background(Void.copy(alpha = .66f)), contentAlignment = Alignment.Center) {
                            Icon(
                                painterResource(R.drawable.ic_locked),
                                stringResource(R.string.locked_scenario),
                                modifier = Modifier.size(38.dp),
                                tint = Warning,
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(scenarioLabel(scenario.id)), Modifier.weight(1f), color = if (unlocked) Cyan else Muted)
                    if (completed) Text("✓", color = Terminal)
                }
                if (!unlocked) Text(stringResource(R.string.scenario_locked, completedChallenges, scenario.required), color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge: ChallengeLevel,
    unlocked: Boolean,
    completed: Boolean,
    record: ChallengeRecord?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val maxTurns = challenge.rules.maxTurns
    val maxTrace = challenge.rules.maxTrace
    val ruleText = when {
        maxTurns != null && maxTrace != null -> stringResource(R.string.challenge_rules_both, maxTurns, maxTrace)
        maxTurns != null -> stringResource(R.string.challenge_rules_turns, maxTurns)
        else -> stringResource(R.string.challenge_rules_trace, maxTrace ?: 0)
    }
    val interaction = remember { MutableInteractionSource() }
    Card(onClick = onClick, enabled = unlocked, interactionSource = interaction,
        modifier = modifier.fillMaxWidth().pressFeedback(interaction, label = "challenge card", pressedScale = .97f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (completed) Terminal else if (unlocked) Cyan.copy(alpha = .45f) else Muted),
        colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("%02d".format(challenge.number), color = if (unlocked) Terminal else Muted, fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.challenge_level, challenge.number), Modifier.weight(1f), color = if (unlocked) Cyan else Muted)
                if (!unlocked) Icon(painterResource(R.drawable.ic_locked), stringResource(R.string.locked_scenario), tint = Warning)
                else if (completed) Text("✓", color = Terminal)
            }
            Text(stringResource(R.string.puzzle_difficulty, challenge.difficulty), color = if (unlocked) Warning else Muted, fontSize = 11.sp, lineHeight = 14.sp)
            Text(ruleText, color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
            Text(
                record?.let { stringResource(R.string.puzzle_record, it.turns, it.trace) }
                    ?: stringResource(R.string.expected_reward, Rewards.VICTORY + Rewards.FIRST_CHALLENGE),
                color = if (record != null) Terminal else Warning,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
internal fun ScenarioScreen(state: GameUiState, actions: MainViewModel, onStart: () -> Unit, onBack: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val transition = LocalWindowTransition.current
    val completedChallenges = state.preferences.challengeBest.size
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressionHeader(stringResource(R.string.free_scenarios), onBack)
            LazyVerticalGrid(GridCells.Adaptive(220.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                itemsIndexed(ScenarioCatalog.all, key = { _, item -> item.id }) { index, scenario ->
                    ScenarioCard(
                        scenario = scenario,
                        unlocked = completedChallenges >= scenario.required,
                        completed = scenario.id in state.preferences.completedScenarios,
                        completedChallenges = completedChallenges,
                        boardSkin = state.preferences.boardSkin,
                        dominoSkin = state.preferences.dominoSkin,
                        modifier = Modifier.testTag("scenario-${scenario.id}"),
                    ) {
                        selected = scenario.id
                    }
                }
            }
        }
    }
    selected?.let { id ->
        val scenario = ScenarioCatalog.get(id)
        if (completedChallenges >= scenario.required) GameDialog(
            stringResource(scenarioLabel(id)), { selected = null }, modifier = Modifier.testTag("scenario-dialog"),
            actions = {
                MenuArtworkButton(stringResource(R.string.back), { selected = null }, compact = true, fillWidth = false, modifier = Modifier.testTag("back-scenario"))
                MenuArtworkButton(stringResource(R.string.play_scenario), {
                    selected = null
                    transition { actions.startScenario(id); onStart() }
                }, compact = true, fillWidth = false, modifier = Modifier.testTag("play-scenario"))
            },
        ) {
            Text(stringResource(R.string.scenario_difficulty,
                ScenarioCatalog.all.indexOf(scenario) + 1, scenario.width, scenario.height))
            Text(stringResource(R.string.scenario_route, scenario.route))
            Text(stringResource(R.string.scenario_firewalls, scenario.firewalls))
            Text(stringResource(R.string.scenario_traps, scenario.traps))
            if (scenario.required > 0) Text(stringResource(R.string.scenario_locked, completedChallenges, scenario.required))
        }
    }
}

@Composable
internal fun ChallengeScreen(state: GameUiState, actions: MainViewModel, onStart: () -> Unit, onBack: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val transition = LocalWindowTransition.current
    CircuitBackground {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressionHeader(stringResource(R.string.challenge), onBack,
                counter = "${state.preferences.challengeBest.size}/${ChallengeCatalog.COUNT}")
            LazyVerticalGrid(GridCells.Adaptive(220.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(ChallengeCatalog.levels, key = { it.number }) { challenge ->
                    ChallengeCard(challenge,
                        challenge.number <= state.preferences.challengeUnlocked,
                        challenge.number in state.preferences.challengeBest,
                        state.preferences.challengeBest[challenge.number],
                        Modifier.testTag("challenge-${challenge.number}")) { selected = challenge.number }
                }
            }
        }
    }
    selected?.let { level ->
        val challenge = ChallengeCatalog.level(level) ?: return@let
        val record = state.preferences.challengeBest[level]
        if (level <= state.preferences.challengeUnlocked) GameDialog(
            stringResource(R.string.challenge_level, level), { selected = null },
            modifier = Modifier.testTag("challenge-dialog"),
            actions = {
                MenuArtworkButton(stringResource(R.string.back), { selected = null }, compact = true, fillWidth = false, modifier = Modifier.testTag("back-challenge"))
                MenuArtworkButton(stringResource(R.string.play_scenario), {
                    selected = null
                    transition { actions.startChallenge(level); onStart() }
                }, compact = true, fillWidth = false, modifier = Modifier.testTag("play-challenge"))
            },
        ) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp)) {
            Text(stringResource(R.string.puzzle_objective))
            Text(stringResource(R.string.puzzle_difficulty, challenge.difficulty))
            challenge.rules.maxTurns?.let { Text(stringResource(R.string.challenge_rules_turns, it)) }
            challenge.rules.maxTrace?.let { Text(stringResource(R.string.challenge_rules_trace, it)) }
            Text(stringResource(R.string.expected_reward, Rewards.VICTORY + if (record == null) Rewards.FIRST_CHALLENGE else 0), color = Warning)
            record?.let { Text(stringResource(R.string.puzzle_record, it.turns, it.trace)) }
            }
        }
    }
}
