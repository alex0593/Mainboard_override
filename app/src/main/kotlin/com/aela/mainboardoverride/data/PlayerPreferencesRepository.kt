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
import com.aela.mainboardoverride.domain.Rewards
import androidx.datastore.preferences.core.stringSetPreferencesKey

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
    val credits: Int = 0,
    val ownedSkins: Set<String> = emptySet(),
    val lastScenario: String = "classic",
)

data class ChallengeRecord(val turns: Int, val trace: Int)
data class MatchReward(val base: Int = 0, val bonus: Int = 0, val unlockedScenario: String? = null)

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
    suspend fun finishMatch(id: String, level: Int?, turns: Int, trace: Int): MatchReward {
        if (level != null) recordChallengeVictory(level, turns, trace) else recordVictory(turns, trace)
        return MatchReward()
    }
    suspend fun buySkin(id: String): Boolean = false
    suspend fun setLastScenario(id: String) {}
}

/** Preferences DataStore implementation; match state is deliberately not persisted. */
class DataStorePlayerPreferencesRepository(
    context: Context,
    private val store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> = context.playerDataStore,
) : PlayerPreferencesRepository {

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
            dominoSkin = (values[DOMINO_SKIN] ?: "kenney").let { if (it in setOf("light", "terminal", "neon")) "kenney" else it },
            boardSkin = values[BOARD_SKIN] ?: "pcb",
            contextHelpEnabled = values[CONTEXT_HELP] ?: true,
            challengeUnlocked = maxOf(values[CHALLENGE_UNLOCKED] ?: 1, ((parseRecords(values[CHALLENGE_BEST]).keys.maxOrNull() ?: 0) + 1).coerceAtMost(ChallengeCatalog.COUNT)),
            credits = values[CREDITS] ?: 0,
            ownedSkins = values[OWNED_SKINS] ?: emptySet(),
            lastScenario = values[LAST_SCENARIO] ?: "classic",
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
    override suspend fun setBoardSkin(value: String) { store.edit {
        if (value !in Rewards.premiumSkins || value in (it[OWNED_SKINS] ?: emptySet())) it[BOARD_SKIN] = value
    } }
    override suspend fun setLastScenario(id: String) { store.edit { it[LAST_SCENARIO] = id } }
    override suspend fun buySkin(id: String): Boolean {
        if (id !in Rewards.premiumSkins) return false
        var purchased = false
        store.edit { values ->
            val owned = values[OWNED_SKINS] ?: emptySet()
            val balance = values[CREDITS] ?: 0
            if (id !in owned && balance >= Rewards.SKIN_PRICE) {
                values[CREDITS] = balance - Rewards.SKIN_PRICE
                values[OWNED_SKINS] = owned + id
                purchased = true
            }
        }
        return purchased
    }
    override suspend fun finishMatch(id: String, level: Int?, turns: Int, trace: Int): MatchReward {
        var reward = MatchReward()
        store.edit { values ->
            val paid = values[PAID_MATCHES] ?: emptySet()
            if (id in paid) return@edit
            val records = parseRecords(values[CHALLENGE_BEST])
            val first = level != null && level !in records
            if (level != null) {
                val previous = records[level]
                if (previous == null || turns < previous.turns || turns == previous.turns && trace < previous.trace) records[level] = ChallengeRecord(turns, trace)
                values[CHALLENGE_BEST] = records.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value.turns}:${it.value.trace}" }
                values[CHALLENGE_UNLOCKED] = maxOf(values[CHALLENGE_UNLOCKED] ?: 1, (level + 1).coerceAtMost(ChallengeCatalog.COUNT))
            } else {
                val previous = values[BEST_TURNS]
                if (previous == null || turns < previous || turns == previous && trace < (values[BEST_TRACE] ?: 101)) {
                    values[BEST_TURNS] = turns
                    values[BEST_TRACE] = trace
                }
            }
            reward = MatchReward(Rewards.VICTORY, if (first) Rewards.FIRST_CHALLENGE else 0,
                if (first) com.aela.mainboardoverride.domain.ScenarioCatalog.all.firstOrNull { it.required == records.size }?.id else null)
            values[CREDITS] = (values[CREDITS] ?: 0) + reward.base + reward.bonus
            values[PAID_MATCHES] = paid + id
        }
        return reward
    }
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
        val CREDITS = intPreferencesKey("credits")
        val OWNED_SKINS = stringSetPreferencesKey("owned_skins")
        val PAID_MATCHES = stringSetPreferencesKey("paid_matches")
        val LAST_SCENARIO = stringPreferencesKey("last_scenario")
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
