package com.aela.mainboardoverride

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aela.mainboardoverride.data.DataStorePlayerPreferencesRepository
import com.aela.mainboardoverride.data.PlayerPreferencesRepository
import com.aela.mainboardoverride.data.PlayerPreferences
import com.aela.mainboardoverride.domain.GameAction
import com.aela.mainboardoverride.domain.GameEngine
import com.aela.mainboardoverride.domain.GameEvent
import com.aela.mainboardoverride.domain.GameResult
import com.aela.mainboardoverride.domain.GameState
import com.aela.mainboardoverride.domain.LevelGenerator
import com.aela.mainboardoverride.domain.Orientation
import com.aela.mainboardoverride.domain.Position
import com.aela.mainboardoverride.domain.RejectReason
import com.aela.mainboardoverride.domain.ScriptType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.aela.mainboardoverride.domain.ScenarioCatalog
import com.aela.mainboardoverride.domain.ChallengeCatalog
import com.aela.mainboardoverride.domain.BoardBuff
import com.aela.mainboardoverride.data.MatchReward
import java.util.UUID

data class GameUiState(
    val game: GameState? = null,
    val preferences: PlayerPreferences = PlayerPreferences(),
    val selectedDominoId: String? = null,
    val rotationSteps: Int = 0,
    val selectedScriptId: String? = null,
    val message: RejectReason? = null,
    val spoofHalf: Int = 0,
    val spoofValue: Int = 0,
    val bridgeHorizontal: Boolean = true,
    val challengeLevel: Int? = null,
    val scenarioId: String = "classic",
    val matchId: String = UUID.randomUUID().toString(),
    val reward: MatchReward? = null,
    val reviewingBoard: Boolean = false,
    val endTurnHint: Boolean = false,
    val lastBuff: BoardBuff? = null,
)

/** Coordinates navigation-facing selection state, the pure engine and player preferences. */
class MainViewModel(application: Application, private val repository: PlayerPreferencesRepository) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, DataStorePlayerPreferencesRepository(application))
    private val session = MutableStateFlow(GameUiState())

    val uiState: StateFlow<GameUiState> = combine(session, repository.preferences) { game, preferences ->
        game.copy(preferences = preferences)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState())

    fun start(seed: Long = Random.nextLong()) {
        val game = GameEngine.resolveForcedResult(LevelGenerator.generate(seed)).state
        session.value = GameUiState(game = game)
        viewModelScope.launch { repository.setLastSeed(seed) }
    }

    fun startChallenge(level: Int) {
        startChallenge(level, ChallengeCatalog.level(level)?.seed ?: return)
    }

    fun startChallenge(level: Int, seed: Long) {
        if (level !in 1..com.aela.mainboardoverride.domain.ChallengeCatalog.COUNT || level > uiState.value.preferences.challengeUnlocked) return
        session.value = GameUiState(game = GameEngine.resolveForcedResult(LevelGenerator.generateChallenge(level, seed)).state, challengeLevel = level)
    }

    fun startScenario(id: String, seed: Long = Random.nextLong()) {
        val scenario = ScenarioCatalog.get(id)
        if (scenario.required > uiState.value.preferences.challengeBest.size) return
        session.value = GameUiState(game = GameEngine.resolveForcedResult(LevelGenerator.generateScenario(seed, scenario.id)).state, scenarioId = scenario.id)
        viewModelScope.launch { repository.setLastSeed(seed); repository.setLastScenario(scenario.id) }
    }

    fun retryLast() { uiState.value.preferences.let { prefs -> prefs.lastSeed?.let { startScenario(prefs.lastScenario, it) } } }
    fun reviewBoard() { session.update { it.copy(reviewingBoard = true, selectedDominoId = null, selectedScriptId = null) } }
    fun showResult() { session.update { it.copy(reviewingBoard = false) } }
    fun buySkin(id: String) = viewModelScope.launch { repository.buySkin(id) }

    fun retry() {
        session.value.challengeLevel?.let { restartChallenge(); return }
        val current = session.value.game ?: return
        startScenario(session.value.scenarioId, current.seed)
    }

    /** Restarts the same challenge with a fresh generated network and seed. */
    fun restartChallenge() {
        val level = session.value.challengeLevel ?: return
        // Challenge levels are fixed puzzles: restart restores the catalog seed
        // so the player can retry the exact same network and rules.
        startChallenge(level)
    }

    /** Restarts the active network while preserving its mode; free play gets a fresh seed. */
    fun restartNetwork() {
        session.value.challengeLevel?.let {
            restartChallenge()
            return
        }
        val current = session.value.game ?: return
        var seed = Random.nextLong()
        while (seed == current.seed) seed = Random.nextLong()
        startScenario(session.value.scenarioId, seed)
    }

    fun selectDomino(id: String) {
        if (session.value.game?.result != null) return
        if (session.value.game?.dominoHand?.none { it.id == id } != false) return
        val selectedScript = session.value.selectedScriptId
        val script = session.value.game?.scriptHand?.find { it.id == selectedScript }
        if (script?.type == ScriptType.SPOOF) {
            session.update { it.copy(selectedDominoId = id, message = null) }
        } else {
            session.update {
                it.copy(selectedDominoId = if (it.selectedDominoId == id) null else id, selectedScriptId = null, message = null)
            }
        }
    }

    fun rotate() {
        if (session.value.game?.result != null) return
        session.update { it.copy(rotationSteps = (it.rotationSteps + 1) % 4) }
    }

    fun cancelScript() = session.update { it.copy(selectedScriptId = null, selectedDominoId = null, message = null) }
    fun setSpoofHalf(half: Int) { if (half in 0..1) session.update { it.copy(spoofHalf = half) } }
    fun setSpoofValue(value: Int) { if (value in 0..6) session.update { it.copy(spoofValue = value) } }
    fun toggleBridge() = session.update { it.copy(bridgeHorizontal = !it.bridgeHorizontal) }
    fun placeAt(position: Position) {
        val ui = session.value
        val game = ui.game ?: return
        val script = game.scriptHand.find { it.id == ui.selectedScriptId }
        if (script?.type == ScriptType.KILL_PROCESS) {
            dispatch(GameAction.PlayKillProcess(script.id, position))
            return
        }
        if (script?.type == ScriptType.BRIDGE) {
            dispatch(GameAction.PlayBridge(script.id, position, horizontal = ui.bridgeHorizontal))
            return
        }
        val id = ui.selectedDominoId ?: return
        dispatch(GameAction.PlaceDomino(
            id,
            position,
            if (ui.rotationSteps % 2 == 0) Orientation.HORIZONTAL else Orientation.VERTICAL,
            rotated = ui.rotationSteps >= 2,
        ))
    }

    fun selectScript(id: String) {
        val game = session.value.game ?: return
        val card = game.scriptHand.find { it.id == id } ?: return
        if (session.value.game?.result != null) return
        if (card.type == ScriptType.PING) dispatch(GameAction.PlayPing(id))
        else session.update { it.copy(selectedScriptId = id, selectedDominoId = null, message = null, spoofHalf = 0, spoofValue = 0, bridgeHorizontal = true) }
    }

    fun spoof(value: Int) {
        val ui = session.value
        val card = ui.game?.scriptHand?.find { it.id == ui.selectedScriptId } ?: return
        val tileId = ui.selectedDominoId ?: return
        dispatch(GameAction.PlaySpoof(card.id, tileId, half = ui.spoofHalf, value = value))
    }

    fun endTurn() = dispatch(GameAction.EndTurn)

    fun setLanguage(value: String) = viewModelScope.launch { repository.setLanguage(value) }
    fun setAudio(value: Boolean) = viewModelScope.launch { repository.setAudio(value) }
    fun setVibration(value: Boolean) = viewModelScope.launch { repository.setVibration(value) }
    fun setReducedMotion(value: Boolean) = viewModelScope.launch { repository.setReducedMotion(value) }
    fun setDominoSkin(value: String) = viewModelScope.launch { repository.setDominoSkin(value) }
    fun setBoardSkin(value: String) = viewModelScope.launch { repository.setBoardSkin(value) }
    fun setContextHelpEnabled(value: Boolean) = viewModelScope.launch { repository.setContextHelpEnabled(value) }

    private fun dispatch(action: GameAction) {
        val current = session.value.game ?: return
        val transition = GameEngine.reduce(current, action)
        val resolved = GameEngine.resolveForcedResult(transition.state)
        val rejection = transition.events.filterIsInstance<GameEvent.Rejected>().lastOrNull()?.reason
        session.update {
            val collectedBuff = resolved.events.filterIsInstance<GameEvent.BuffCollected>().lastOrNull()?.buff
            it.copy(
                game = resolved.state,
                selectedDominoId = if (action is GameAction.PlaceDomino && rejection == null) null else it.selectedDominoId,
                selectedScriptId = if (action !is GameAction.PlaceDomino && rejection == null) null else it.selectedScriptId,
                rotationSteps = if (action is GameAction.PlaceDomino && rejection == null) 0 else it.rotationSteps,
                message = rejection,
                endTurnHint = when {
                    action is GameAction.EndTurn && rejection == null -> false
                    rejection == RejectReason.DOMINO_ALREADY_PLACED -> true
                    else -> it.endTurnHint
                },
                lastBuff = collectedBuff,
            )
        }
        if (current.result == null && resolved.state.result == GameResult.VICTORY) {
            val challengeLevel = session.value.challengeLevel
            val matchId = session.value.matchId
            viewModelScope.launch {
                val reward = repository.finishMatch(matchId, challengeLevel, resolved.state.turn, resolved.state.trace)
                session.update { if (it.matchId == matchId) it.copy(reward = reward) else it }
            }
        }
    }
}
