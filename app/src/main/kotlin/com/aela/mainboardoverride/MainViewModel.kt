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
import com.aela.mainboardoverride.domain.Tutorial
import com.aela.mainboardoverride.domain.TutorialStep
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
    val tutorial: Boolean = false,
    val tutorialStep: TutorialStep? = null,
    val tutorialBlocked: Boolean = false,
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

    fun start(seed: Long = Random.nextLong(), tutorial: Boolean = false) {
        val actualSeed = if (tutorial) LevelGenerator.TUTORIAL_SEED else seed
        val game = if (tutorial) Tutorial.fixture(TutorialStep.INTRO)
        else GameEngine.resolveForcedResult(LevelGenerator.generate(actualSeed)).state
        session.value = GameUiState(game = game, tutorial = tutorial, tutorialStep = if (tutorial) TutorialStep.INTRO else null)
        if (!tutorial) viewModelScope.launch { repository.setLastSeed(actualSeed) }
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
        if (session.value.tutorial) { restartLesson(); return }
        session.value.challengeLevel?.let { restartChallenge(); return }
        val current = session.value.game ?: return
        startScenario(session.value.scenarioId, current.seed)
    }

    /** Restarts the same challenge with a fresh generated network and seed. */
    fun restartChallenge() {
        val level = session.value.challengeLevel ?: return
        val previousSeed = session.value.game?.seed
        var seed = Random.nextLong()
        while (seed == previousSeed) seed = Random.nextLong()
        startChallenge(level, seed)
    }

    /** Restarts the active network with a fresh seed while preserving its mode. */
    fun restartNetwork() {
        if (session.value.tutorial) {
            restartLesson()
            return
        }
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
        if (!allowControl(TutorialStep.SELECT, TutorialStep.ROTATE, TutorialStep.PLACE, TutorialStep.SPOOF)) return
        if (session.value.game?.dominoHand?.none { it.id == id } != false) return
        if (session.value.tutorialStep == TutorialStep.SELECT) session.update { it.copy(tutorialStep = TutorialStep.ROTATE) }
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
        if (!allowControl(TutorialStep.ROTATE, TutorialStep.PLACE)) return
        session.update { it.copy(rotationSteps = (it.rotationSteps + 1) % 4, tutorialStep = if (it.tutorialStep == TutorialStep.ROTATE) TutorialStep.PLACE else it.tutorialStep) }
    }

    private fun allowControl(vararg steps: TutorialStep): Boolean {
        if (session.value.game?.result != null) return false
        val step = session.value.tutorialStep
        val allowed = step == null || step == TutorialStep.FINAL || step in steps
        session.update { it.copy(tutorialBlocked = !allowed) }
        return allowed
    }

    fun cancelScript() = session.update { it.copy(selectedScriptId = null, selectedDominoId = null, message = null) }
    fun setSpoofHalf(half: Int) { if (half in 0..1) session.update { it.copy(spoofHalf = half) } }
    fun setSpoofValue(value: Int) { if (value in 0..6) session.update { it.copy(spoofValue = value) } }
    fun toggleBridge() = session.update { it.copy(bridgeHorizontal = !it.bridgeHorizontal) }
    fun continueTutorial() {
        val step = session.value.tutorialStep ?: return
        val next = Tutorial.continueFrom(step)
        if (next == step) return
        if (next == TutorialStep.SELECT) session.update { it.copy(tutorialStep = next, tutorialBlocked = false) }
        else loadLesson(next)
    }
    fun restartLesson() { session.value.tutorialStep?.let { loadLesson(Tutorial.lessonStart(it)) } }
    private fun loadLesson(step: TutorialStep) {
        session.value = GameUiState(game = Tutorial.fixture(step), tutorial = true, tutorialStep = step)
    }

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
        val expected = when (card.type) {
            ScriptType.PING -> TutorialStep.PING
            ScriptType.SPOOF -> TutorialStep.SPOOF
            ScriptType.KILL_PROCESS -> TutorialStep.KILL
            ScriptType.BRIDGE -> TutorialStep.BRIDGE
        }
        if (!allowControl(expected)) return
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
        val step = session.value.tutorialStep
        if (step != null && !Tutorial.allows(step, action)) {
            session.update { it.copy(tutorialBlocked = true) }
            return
        }
        val transition = GameEngine.reduce(current, action)
        val resolved = if (step == null) GameEngine.resolveForcedResult(transition.state) else transition
        val rejection = transition.events.filterIsInstance<GameEvent.Rejected>().lastOrNull()?.reason
        session.update {
            val collectedBuff = resolved.events.filterIsInstance<GameEvent.BuffCollected>().lastOrNull()?.buff
            it.copy(
                game = resolved.state,
                tutorialStep = step?.let { tutorialStep -> Tutorial.after(tutorialStep, action, resolved) },
                tutorialBlocked = false,
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
            val isTutorial = session.value.tutorial
            val challengeLevel = session.value.challengeLevel
            val completed = session.value.tutorialStep == TutorialStep.COMPLETE
            val matchId = session.value.matchId
            viewModelScope.launch {
                if (!isTutorial) {
                    val reward = repository.finishMatch(matchId, challengeLevel, resolved.state.turn, resolved.state.trace)
                    session.update { if (it.matchId == matchId) it.copy(reward = reward) else it }
                }
                else if (completed) repository.markTutorialComplete()
            }
        }
    }
}
