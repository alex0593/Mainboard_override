package com.aela.mainboardoverride.audio

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.SharedFlow

/**
 * Waveform vibration for a [SoundCue]: start delay first, then alternating
 * vibrate/pause spans with a matching amplitude per span (0 = pause).
 * A null pattern means the cue stays silent on the vibrator.
 */
data class HapticPattern(val timings: LongArray, val amplitudes: IntArray)

/** Which cues reach the vibrator; informational cues stay quiet to avoid buzz fatigue. */
fun hapticPattern(cue: SoundCue): HapticPattern? = when (cue) {
    SoundCue.Tick -> HapticPattern(longArrayOf(0, 20), intArrayOf(0, 64))
    SoundCue.Place -> HapticPattern(longArrayOf(0, 45), intArrayOf(0, 160))
    SoundCue.Ping -> null
    SoundCue.Spoof -> HapticPattern(longArrayOf(0, 30), intArrayOf(0, 110))
    SoundCue.Kill -> HapticPattern(longArrayOf(0, 70, 50, 90), intArrayOf(0, 200, 0, 255))
    SoundCue.Bridge -> HapticPattern(longArrayOf(0, 45), intArrayOf(0, 150))
    SoundCue.Stealth -> HapticPattern(longArrayOf(0, 30), intArrayOf(0, 110))
    SoundCue.Alarm -> HapticPattern(longArrayOf(0, 80, 60, 80), intArrayOf(0, 220, 0, 220))
    SoundCue.Cooler -> HapticPattern(longArrayOf(0, 35), intArrayOf(0, 120))
    SoundCue.Ram -> HapticPattern(longArrayOf(0, 35), intArrayOf(0, 120))
    SoundCue.Error -> HapticPattern(longArrayOf(0, 60, 50, 60), intArrayOf(0, 200, 0, 200))
    SoundCue.Turn -> null
    SoundCue.TraceWarning -> HapticPattern(longArrayOf(0, 70, 60, 70), intArrayOf(0, 220, 0, 220))
    SoundCue.Victory -> HapticPattern(longArrayOf(0, 60, 50, 60, 50, 120), intArrayOf(0, 150, 0, 180, 0, 255))
    SoundCue.Defeat -> HapticPattern(longArrayOf(0, 250), intArrayOf(0, 200))
    SoundCue.Boot -> null
    SoundCue.Coin -> null
}

/**
 * Collects one-shot [SoundCue]s and mirrors the tactile ones on the vibrator.
 * Cues are dropped while vibration is disabled or the device has no vibrator;
 * failures never reach the UI.
 */
@Composable
fun HapticsHost(
    enabled: Boolean,
    cues: SharedFlow<SoundCue>,
) {
    val context = LocalContext.current
    val vibrator = remember(context) { context.vibrator() }
    val currentEnabled by rememberUpdatedState(enabled)
    LaunchedEffect(vibrator) {
        cues.collect { cue ->
            if (!currentEnabled) return@collect
            val pattern = hapticPattern(cue) ?: return@collect
            runCatching {
                if (vibrator?.hasVibrator() == true) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1))
                }
            }.onFailure { Log.w(TAG, "dropping haptic $cue", it) }
        }
    }
}

private fun Context.vibrator(): Vibrator? = runCatching {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }
}.getOrNull()

private const val TAG = "Haptics"
