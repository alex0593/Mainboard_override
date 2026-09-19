package com.aela.mainboardoverride.ui

import com.aela.mainboardoverride.audio.SoundCue
import com.aela.mainboardoverride.data.PlayerPreferencesRepository
import com.aela.mainboardoverride.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Owns a practice session, never the player's active match or reward pipeline. */
class TutorialController(
    private val repository: PlayerPreferencesRepository,
    private val scope: CoroutineScope,
    private val onCue: (SoundCue) -> Unit = {},
) {
    private val mutableState = MutableStateFlow(TutorialCatalog.start(0))
    val state = mutableState.asStateFlow()
    fun start(lesson: Int) {
        mutableState.value = TutorialCatalog.start(lesson)
        save(false)
    }
    fun dispatch(input: TutorialInput) {
        mutableState.value = TutorialCatalog.reduce(mutableState.value, input)
        val current = mutableState.value
        onCue(when {
            current.finished && current.lesson == TutorialCatalog.lessons.lastIndex -> SoundCue.Victory
            current.finished -> SoundCue.Coin
            else -> SoundCue.Tick
        })
        if (current.finished && current.lesson == TutorialCatalog.lessons.lastIndex) save(true)
    }
    fun advance() {
        val current = mutableState.value
        if (current.finished && current.lesson < TutorialCatalog.lessons.lastIndex) start(current.lesson + 1)
        onCue(SoundCue.Boot)
    }
    private fun save(completed: Boolean) {
        val lesson = mutableState.value.lesson
        scope.launch { repository.setTutorialProgress(lesson, completed) }
    }
}
