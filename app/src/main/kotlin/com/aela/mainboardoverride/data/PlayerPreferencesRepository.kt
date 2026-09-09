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
import com.aela.mainboardoverride.domain.ChallengeCatalog

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
    val dominoSkin: String = "kenney",
    val boardSkin: String = "pcb",
    val contextHelpEnabled: Boolean = true,
    val challengeUnlocked: Int = 1,
    val challengeBest: Map<Int, ChallengeRecord> = emptyMap(),
)

data class ChallengeRecord(val turns: Int, val trace: Int)

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
    suspend fun setDominoSkin(value: String)
    suspend fun setBoardSkin(value: String)
    suspend fun setContextHelpEnabled(value: Boolean)
    suspend fun recordChallengeVictory(level: Int, turns: Int, trace: Int)
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
            dominoSkin = values[DOMINO_SKIN] ?: "kenney",
            boardSkin = values[BOARD_SKIN] ?: "pcb",
            contextHelpEnabled = values[CONTEXT_HELP] ?: true,
            challengeUnlocked = values[CHALLENGE_UNLOCKED] ?: 1,
            challengeBest = values[CHALLENGE_BEST].orEmpty().split(",").mapNotNull { item ->
                val fields = item.split(":")
                if (fields.size == 3) fields[0].toIntOrNull()?.let { level ->
                    ChallengeRecord(fields[1].toIntOrNull() ?: return@let null, fields[2].toIntOrNull() ?: return@let null).let { level to it }
                } else null
            }.toMap(),
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

    override suspend fun setDominoSkin(value: String) { store.edit { it[DOMINO_SKIN] = value } }
    override suspend fun setBoardSkin(value: String) { store.edit { it[BOARD_SKIN] = value } }
    override suspend fun setContextHelpEnabled(value: Boolean) { store.edit { it[CONTEXT_HELP] = value } }
    override suspend fun recordChallengeVictory(level: Int, turns: Int, trace: Int) {
        store.edit { values ->
            val records = parseRecords(values[CHALLENGE_BEST])
            val previous = records[level]
            if (previous == null || turns < previous.turns || turns == previous.turns && trace < previous.trace) records[level] = ChallengeRecord(turns, trace)
            values[CHALLENGE_BEST] = records.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value.turns}:${it.value.trace}" }
            values[CHALLENGE_UNLOCKED] = maxOf(values[CHALLENGE_UNLOCKED] ?: 1, (level + 1).coerceAtMost(ChallengeCatalog.COUNT))
        }
    }

    private fun parseRecords(raw: String?): MutableMap<Int, ChallengeRecord> = raw.orEmpty().split(",").mapNotNull { item ->
        val fields = item.split(":")
        if (fields.size == 3) {
            val level = fields[0].toIntOrNull(); val turns = fields[1].toIntOrNull(); val trace = fields[2].toIntOrNull()
            if (level != null && turns != null && trace != null) level to ChallengeRecord(turns, trace) else null
        } else null
    }.toMap().toMutableMap()

    private companion object {
        val LANGUAGE = stringPreferencesKey("language")
        val AUDIO = booleanPreferencesKey("audio")
        val VIBRATION = booleanPreferencesKey("vibration")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val TUTORIAL = booleanPreferencesKey("tutorial_complete")
        val LAST_SEED = longPreferencesKey("last_seed")
        val BEST_TURNS = intPreferencesKey("best_turns")
        val BEST_TRACE = intPreferencesKey("best_trace")
        val DOMINO_SKIN = stringPreferencesKey("domino_skin")
        val BOARD_SKIN = stringPreferencesKey("board_skin")
        val CONTEXT_HELP = booleanPreferencesKey("context_help")
        val CHALLENGE_UNLOCKED = intPreferencesKey("challenge_unlocked")
        val CHALLENGE_BEST = stringPreferencesKey("challenge_best")
    }
}
