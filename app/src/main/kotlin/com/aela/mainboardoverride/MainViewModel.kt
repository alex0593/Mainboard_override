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
        session.value = GameUiState(game = if (tutorial) Tutorial.fixture(TutorialStep.INTRO) else LevelGenerator.generate(actualSeed), tutorial = tutorial, tutorialStep = if (tutorial) TutorialStep.INTRO else null)
        if (!tutorial) viewModelScope.launch { repository.setLastSeed(actualSeed) }
    }

    fun retry() {
        if (session.value.tutorial) { restartLesson(); return }
        val current = session.value.game ?: return
        start(current.seed, session.value.tutorial)
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

    private fun dispatch(action: GameAction) {
        val current = session.value.game ?: return
        val step = session.value.tutorialStep
        if (step != null && !Tutorial.allows(step, action)) {
            session.update { it.copy(tutorialBlocked = true) }
            return
        }
        val transition = GameEngine.reduce(current, action)
        val rejection = transition.events.filterIsInstance<GameEvent.Rejected>().lastOrNull()?.reason
        session.update {
            it.copy(
                game = transition.state,
                tutorialStep = step?.let { tutorialStep -> Tutorial.after(tutorialStep, action, transition) },
                tutorialBlocked = false,
                selectedDominoId = if (action is GameAction.PlaceDomino && rejection == null) null else it.selectedDominoId,
                selectedScriptId = if (action !is GameAction.PlaceDomino && rejection == null) null else it.selectedScriptId,
                rotationSteps = if (action is GameAction.PlaceDomino && rejection == null) 0 else it.rotationSteps,
                message = rejection,
            )
        }
        if (current.result == null && transition.state.result == GameResult.VICTORY) {
            val isTutorial = session.value.tutorial
            val completed = session.value.tutorialStep == TutorialStep.COMPLETE
            viewModelScope.launch {
                if (!isTutorial) repository.recordVictory(transition.state.turn, transition.state.trace)
                else if (completed) repository.markTutorialComplete()
            }
        }
    }
}
