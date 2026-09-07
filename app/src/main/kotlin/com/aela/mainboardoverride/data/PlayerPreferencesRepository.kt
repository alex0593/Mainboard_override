package com.aela.mainboardoverride.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playerDataStore by preferencesDataStore("player_preferences")

data class PlayerPreferences(
    val language: String = "es",
    val audioEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val tutorialComplete: Boolean = false,
    val lastSeed: Long? = null,
    val bestTurns: Int? = null,
    val bestTrace: Int? = null,
)

/** Persistence boundary for profile data that survives process death. */
interface PlayerPreferencesRepository {
    val preferences: Flow<PlayerPreferences>
    suspend fun setLanguage(value: String)
    suspend fun setAudio(value: Boolean)
    suspend fun setVibration(value: Boolean)
    suspend fun setReducedMotion(value: Boolean)
    suspend fun markTutorialComplete()
    suspend fun setLastSeed(seed: Long)
    suspend fun recordVictory(turns: Int, trace: Int)
}

/** Preferences DataStore implementation; match state is deliberately not persisted. */
class DataStorePlayerPreferencesRepository(context: Context) : PlayerPreferencesRepository {
    private val store = context.playerDataStore

    override val preferences = store.data.map { values ->
        PlayerPreferences(
            language = values[LANGUAGE] ?: "es",
            audioEnabled = values[AUDIO] ?: true,
            vibrationEnabled = values[VIBRATION] ?: true,
            reducedMotion = values[REDUCED_MOTION] ?: false,
            tutorialComplete = values[TUTORIAL] ?: false,
            lastSeed = values[LAST_SEED],
            bestTurns = values[BEST_TURNS],
            bestTrace = values[BEST_TRACE],
        )
    }

    override suspend fun setLanguage(value: String) { store.edit { it[LANGUAGE] = value } }
    override suspend fun setAudio(value: Boolean) { store.edit { it[AUDIO] = value } }
    override suspend fun setVibration(value: Boolean) { store.edit { it[VIBRATION] = value } }
    override suspend fun setReducedMotion(value: Boolean) { store.edit { it[REDUCED_MOTION] = value } }
    override suspend fun markTutorialComplete() { store.edit { it[TUTORIAL] = true } }
    override suspend fun setLastSeed(seed: Long) { store.edit { it[LAST_SEED] = seed } }
    override suspend fun recordVictory(turns: Int, trace: Int) {
        store.edit { values ->
            val previousTurns = values[BEST_TURNS]
            val previousTrace = values[BEST_TRACE]
            if (previousTurns == null || turns < previousTurns || turns == previousTurns && trace < (previousTrace ?: 101)) {
                values[BEST_TURNS] = turns
                values[BEST_TRACE] = trace
            }
        }
    }

    private companion object {
        val LANGUAGE = stringPreferencesKey("language")
        val AUDIO = booleanPreferencesKey("audio")
        val VIBRATION = booleanPreferencesKey("vibration")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val TUTORIAL = booleanPreferencesKey("tutorial_complete")
        val LAST_SEED = longPreferencesKey("last_seed")
        val BEST_TURNS = intPreferencesKey("best_turns")
        val BEST_TRACE = intPreferencesKey("best_trace")
    }
}
