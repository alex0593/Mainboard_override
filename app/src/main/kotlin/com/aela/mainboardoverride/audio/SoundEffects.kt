package com.aela.mainboardoverride.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** One-shot feedback sounds; every cue is synthesized, no assets. */
enum class SoundCue(val durationMs: Long) {
    Tick(40),
    Place(140),
    Ping(450),
    Spoof(240),
    Kill(380),
    Bridge(260),
    Stealth(300),
    Alarm(420),
    Cooler(260),
    Ram(220),
    Error(200),
    Turn(140),
    TraceWarning(380),
    Victory(750),
    Defeat(650),
    Boot(380),
    Coin(260),
}

/**
 * Collects one-shot [SoundCue]s and plays them through [SoundEffects].
 *
 * The player is released when the host leaves the composition; cues are dropped
 * while audio is disabled or [volume] is zero.
 */
@Composable
fun SoundEffectsHost(
    enabled: Boolean,
    volume: Float,
    cues: SharedFlow<SoundCue>,
) {
    val player = remember { SoundEffects() }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentVolume by rememberUpdatedState(volume)

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(player) {
        cues.collect { cue ->
            if (currentEnabled && currentVolume > 0f) player.play(cue, currentVolume)
        }
    }
}

/**
 * Fire-and-forget player for [SoundCue].
 *
 * Each cue renders into its own short static [AudioTrack], so overlapping cues mix
 * naturally. Concurrency is bounded; extras are dropped to protect the UI thread.
 */
class SoundEffects {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val active = AtomicInteger(0)

    fun play(cue: SoundCue, volume: Float) {
        if (volume <= 0f || active.get() >= MAX_CONCURRENT) return
        val samples = render(cue, volume)
        scope.launch {
            active.incrementAndGet()
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                try {
                    track.write(samples, 0, samples.size)
                    track.play()
                    delay(cue.durationMs + 80L)
                } finally {
                    runCatching {
                        track.stop()
                        track.release()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "dropping cue $cue", e)
            } finally {
                active.decrementAndGet()
            }
        }
    }

    fun release() {
        scope.cancel()
    }

    private fun render(cue: SoundCue, volume: Float): ShortArray {
        val count = (SAMPLE_RATE * cue.durationMs / 1_000).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val wave = renderWave(cue, count)
        for (i in wave.indices) {
            out[i] = (wave[i] * volume.coerceIn(0f, 1f)).coerceIn(-.9, .9).let {
                (it * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }

    private fun renderWave(cue: SoundCue, count: Int): DoubleArray = when (cue) {
        SoundCue.Tick -> DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            ((noise(i.toLong()) - noise(i.toLong() - 1)) * exp(-t * 220.0) +
                sin(TAU * 2400.0 * t) * exp(-t * 180.0) * .5) * .14
        }
        SoundCue.Place -> DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            (sin(TAU * 180.0 * t) * exp(-t * 38.0) +
                noise(i.toLong()) * exp(-t * 55.0) * .35) *
                attack(t, 1.0) * .5
        }
        SoundCue.Ping -> DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val ping = sin(TAU * (880.0 * t + 440.0 * (1.0 - exp(-t * 4.0)) / 4.0)) * exp(-t * 5.5)
            val echo = if (t >= .15) {
                val e = t - .15
                sin(TAU * (880.0 * e + 440.0 * (1.0 - exp(-e * 4.0)) / 4.0)) * exp(-e * 5.5) * .35
            } else 0.0
            (ping + echo) * attack(t, 2.0) * .4
        }
        SoundCue.Spoof -> blips(
            count,
            listOf(0.0 to 660.0, .06 to 880.0, .12 to 1100.0, .18 to 1320.0),
            blipLength = .05,
            gain = .32,
        )
        SoundCue.Kill -> DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            (noise(i.toLong()) * exp(-t * 11.0) +
                sin(TAU * (60.0 * t + 360.0 * (1.0 - exp(-t * 7.0)) / 7.0)) * exp(-t * 9.0)) *
                attack(t, 1.0) * .5
        }
        SoundCue.Bridge -> notes(
            count,
            listOf(0.0 to 220.0, .13 to 329.63),
            decay = 9.0,
            gain = .38,
        )
        SoundCue.Stealth -> DoubleArray(count) { i ->
            // Downward sweep plus a dying hiss reads as pending noise wiping away.
            val t = i.toDouble() / SAMPLE_RATE
            (sin(TAU * (330.0 * t + 550.0 * (1.0 - exp(-t * 5.0)) / 5.0)) * exp(-t * 6.0) +
                noise(i.toLong()) * exp(-t * 18.0) * .3) * attack(t, 2.0) * .34
        }
        SoundCue.Alarm -> DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val pulse = if (t < .18 || t >= .2 && t < .38) 1.0 else 0.0
            (sin(TAU * 620.0 * t) + sin(TAU * 655.0 * t)) * .5 * pulse *
                attack(t, 4.0) * .38
        }
        SoundCue.Cooler -> DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            sin(TAU * (660.0 * t + 220.0 * (1.0 - exp(-t * 6.0)) / 6.0)) *
                exp(-t * 9.0) * attack(t, 2.0) * .34
        }
        SoundCue.Ram -> DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            sin(TAU * (520.0 * t + 260.0 * (1.0 - exp(-t * 8.0)) / 8.0)) *
                exp(-t * 10.0) * attack(t, 2.0) * .34
        }
        SoundCue.Error -> DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            (sin(TAU * 140.0 * t) + .4 * sin(TAU * 105.0 * t) + .3 * sin(TAU * 420.0 * t)) *
                attack(t, 4.0) * (1.0 - t / (count.toDouble() / SAMPLE_RATE)) * .4
        }
        SoundCue.Turn -> DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            (noise(i.toLong()) * exp(-t * 70.0) * .6 +
                sin(TAU * 120.0 * t) * exp(-t * 45.0)) * attack(t, 1.0) * .45
        }
        SoundCue.TraceWarning -> blips(
            count,
            listOf(0.0 to 700.0, .12 to 920.0, .24 to 1150.0),
            blipLength = .1,
            gain = .38,
        )
        SoundCue.Victory -> notes(
            count,
            listOf(0.0 to 440.0, .13 to 523.25, .26 to 659.25, .39 to 880.0),
            decay = 5.0,
            gain = .42,
        )
        SoundCue.Defeat -> notes(
            count,
            listOf(0.0 to 220.0, .15 to 174.61, .3 to 146.83, .45 to 110.0),
            decay = 4.5,
            gain = .42,
        )
        SoundCue.Boot -> DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            (sin(TAU * (120.0 * t + 720.0 * (1.0 - exp(-t * 5.0)) / 5.0)) * exp(-t * 3.2) +
                noise(i.toLong()) * exp(-t * 120.0) * .3) * attack(t, 2.0) * .38
        }
        SoundCue.Coin -> notes(
            count,
            listOf(0.0 to 987.77, .09 to 1318.51),
            decay = 12.0,
            gain = .38,
        )
    }

    /** Fixed-pitch note sequence; each entry is start-seconds to frequency. */
    private fun notes(count: Int, sequence: List<Pair<Double, Double>>, decay: Double, gain: Double) =
        DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            var voice = 0.0
            for ((start, frequency) in sequence) {
                val local = t - start
                if (local >= 0.0) {
                    voice += (sin(TAU * frequency * local) + .3 * sin(TAU * frequency * 2.0 * local)) *
                        (local * 250.0).coerceIn(0.0, 1.0) * exp(-local * decay)
                }
            }
            voice * gain
        }

    /** Short bright blips for alerts and digital effects. */
    private fun blips(count: Int, sequence: List<Pair<Double, Double>>, blipLength: Double, gain: Double) =
        DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            var voice = 0.0
            for ((start, frequency) in sequence) {
                val local = t - start
                if (local in 0.0..blipLength) {
                    voice += (sin(TAU * frequency * local) + .3 * sin(TAU * frequency * 3.0 * local)) *
                        (local * 500.0).coerceIn(0.0, 1.0) * exp(-local * 30.0)
                }
            }
            voice * gain
        }

    private fun attack(t: Double, millis: Double): Double =
        (t * 1000.0 / millis).coerceIn(0.0, 1.0)

    /** Stateless deterministic white noise in [-1, 1]. */
    private fun noise(sample: Long): Double {
        var h = sample * 0x9E3779B1L + 0x85EBCA77L
        h = h xor (h ushr 13)
        h *= 0xC2B2AE35L
        h = h xor (h ushr 16)
        return ((h ushr 11).toDouble() / 9007199254740992.0) * 2.0 - 1.0
    }

    private companion object {
        const val TAG = "SoundEffects"
        const val SAMPLE_RATE = 22_050
        const val TAU = 2.0 * PI
        const val MAX_CONCURRENT = 6
    }
}
