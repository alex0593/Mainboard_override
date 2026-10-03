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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** Dramatic context the soundtrack should score. */
enum class SoundtrackScene {
    MENU,
    GAME,
    VICTORY,
    DEFEAT,
}

/**
 * Keeps the adaptive procedural soundtrack alive only while the app is visible.
 *
 * The engine renders a dark-synthwave loop (Am - F - G - E at 100 BPM) whose layers bloom
 * with [trace]: menus stay on pads and bass, matches add drums, bass drive and arps,
 * trace above 80 adds a noise riser for urgency, victory adds a lead motif and defeat
 * falls back to a sparse drone. All layer gains move through smoothed energy, so scene
 * changes never click.
 */
@Composable
fun AmbientSoundtrackHost(
    enabled: Boolean,
    scene: SoundtrackScene = SoundtrackScene.MENU,
    trace: Int = 0,
    volume: Float = 1f,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val soundtrack = remember { AmbientSoundtrack() }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentScene by rememberUpdatedState(scene)
    val currentTrace by rememberUpdatedState(trace)
    val currentVolume by rememberUpdatedState(volume)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    soundtrack.configure(currentScene, currentTrace, currentVolume)
                    if (currentEnabled) soundtrack.start() else soundtrack.stop()
                }
                Lifecycle.Event.ON_STOP -> soundtrack.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && currentEnabled) {
            soundtrack.configure(currentScene, currentTrace, currentVolume)
            soundtrack.start()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            soundtrack.stop()
        }
    }

    LaunchedEffect(enabled, lifecycleOwner) {
        if (enabled && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            soundtrack.start()
        } else {
            soundtrack.stop()
        }
    }

    LaunchedEffect(scene, trace, volume) {
        soundtrack.configure(scene, trace, volume)
    }
}

private class AmbientSoundtrack {
    private val lock = Any()
    private var audioTrack: AudioTrack? = null
    private var renderJob: Job? = null
    /** Bumped by start/stop under [lock]; a render generation stale after a stop never swaps tracks. */
    private var generation = 0

    @Volatile private var targetScene: SoundtrackScene = SoundtrackScene.MENU
    @Volatile private var targetTrace: Int = 0
    @Volatile private var targetVolume: Float = 1f

    fun configure(scene: SoundtrackScene, trace: Int, volume: Float = 1f) {
        targetScene = scene
        targetTrace = trace
        targetVolume = volume
    }

    fun start() {
        synchronized(lock) {
            if (renderJob?.isActive == true) return

            val track = createTrack() ?: return
            audioTrack = track
            // Playback starts inside render(), once the head of the buffer is filled:
            // play() on an empty track underruns immediately (audible startup glitch).
            val generation = ++this.generation
            renderJob = kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
                render(track, generation)
            }
            Log.d(TAG, "soundtrack start (scene=$targetScene, trace=$targetTrace, volume=$targetVolume)")
        }
    }

    fun stop() {
        val job: Job?
        val track: AudioTrack?
        synchronized(lock) {
            // Bumping the generation invalidates any in-flight write recovery below.
            generation++
            job = renderJob
            track = audioTrack
            renderJob = null
            audioTrack = null
        }
        if (job != null || track != null) Log.d(TAG, "soundtrack stop")
        job?.cancel()
        runCatching {
            track?.pause()
            track?.flush()
            track?.release()
        }
    }

    private fun createTrack(): AudioTrack? {
        val track = runCatching {
            val minBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                // Four chunks (~370 ms) of headroom so a GC pause or a busy UI frame during
                // startup or scene changes never drains the buffer into an audible underrun.
                .setBufferSizeInBytes((BUFFER_SAMPLES * 4 * 2).coerceAtLeast(minBytes))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrElse {
            Log.w(TAG, "AudioTrack build threw: ${it.message}")
            return null
        }
        // build() can return an uninitialized track; writes on it fail instantly.
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            Log.w(TAG, "AudioTrack state=${track.state} after build, discarding")
            runCatching { track.release() }
            return null
        }
        return track
    }

    /**
     * Renders loop buffers into [initialTrack].
     *
     * When a write fails while this generation is still the live one (audio policy
     * invalidation, routing change), the track is rebuilt and prefilled again instead
     * of silencing the soundtrack; a stale generation (stop() raced us) exits quietly.
     */
    private suspend fun render(initialTrack: AudioTrack, generation: Int) = withContext(Dispatchers.Default) {
        var track = initialTrack
        var failures = 0
        val samples = ShortArray(BUFFER_SAMPLES)
        var cursor = 0L
        var rendered = 0L
        var trackWritten = 0L
        var energy = 0.0
        var lead = 0.0
        var level = 0.0
        var started = false
        while (isActive) {
            energy += (targetEnergy(targetScene, targetTrace) - energy) * ENERGY_SMOOTHING
            val leadTarget = if (targetScene == SoundtrackScene.VICTORY) 1.0 else 0.0
            lead += (leadTarget - lead) * LEAD_SMOOTHING
            level += (targetVolume.toDouble().coerceIn(0.0, 1.0) - level) * VOLUME_SMOOTHING
            renderBuffer(samples, cursor, energy, lead, level, rendered)
            var failure: String? = null
            try {
                val wrote = track.write(samples, 0, samples.size)
                if (wrote < 0) failure = "write returned $wrote"
            } catch (e: Exception) {
                // The track can be released by stop() while a blocking write is in flight.
                failure = "write threw ${e.javaClass.simpleName}"
            }
            if (failure == null) {
                cursor += samples.size
                rendered += samples.size
                trackWritten += samples.size
                // Only hand the track to the mixer once the head of the buffer holds audio,
                // so the soundtrack fades in from real samples instead of starting underrun.
                if (!started && trackWritten >= PREFILL_SAMPLES) {
                    try {
                        track.play()
                        started = true
                    } catch (e: Exception) {
                        failure = "play threw ${e.javaClass.simpleName}"
                    }
                }
            }
            if (failure == null) continue
            if (!isActive) break
            failures++
            Log.w(TAG, "AudioTrack $failure, recovering soundtrack track (attempt $failures/$MAX_TRACK_FAILURES)")
            if (failures >= MAX_TRACK_FAILURES) {
                Log.w(TAG, "giving up after $failures consecutive soundtrack failures")
                break
            }
            runCatching {
                track.pause()
                track.flush()
                track.release()
            }
            val replacement = createTrack() ?: run {
                Log.w(TAG, "no replacement AudioTrack, soundtrack silent")
                break
            }
            val swapped = synchronized(lock) {
                if (this@AmbientSoundtrack.generation == generation) {
                    audioTrack = replacement
                    true
                } else false
            }
            if (!swapped) {
                runCatching { replacement.release() }
                break
            }
            track = replacement
            trackWritten = 0L
            started = false
        }
    }

    private fun renderBuffer(
        buffer: ShortArray,
        startSample: Long,
        energy: Double,
        leadLevel: Double,
        masterLevel: Double,
        rendered: Long,
    ) {
        val drumGate = gate(energy, .35, .55)
        for (index in buffer.indices) {
            val sample = startSample + index
            val time = sample.toDouble() / SAMPLE_RATE
            val beat = sample.toDouble() / SAMPLES_PER_BEAT
            val loopBeat = beat % LOOP_BEATS
            val chordIndex = (loopBeat / BEATS_PER_BAR).toInt().coerceIn(0, CHORDS - 1)
            val barBeat = loopBeat % BEATS_PER_BAR

            val padFreq = PAD_FREQ[chordIndex]
            val prevPadFreq = PAD_FREQ[(chordIndex + CHORDS - 1) % CHORDS]
            val arpFreq = ARP_FREQ[chordIndex]

            // Pads: detuned chord-stack crossfaded into the next bar, always present.
            val attack = smoothstep((loopBeat % BEATS_PER_BAR) / .75)
            var pad = 0.0
            for (i in padFreq.indices) {
                val frequency = padFreq[i]
                val lfo = .75 + .25 * sin(TAU * .07 * time + i * 1.7)
                val voice = (sin(TAU * frequency * time) +
                    .8 * sin(TAU * frequency * 1.003 * time)) * .5 * lfo
                val prevFrequency = prevPadFreq[i]
                val prevVoice = (sin(TAU * prevFrequency * time) +
                    .8 * sin(TAU * prevFrequency * 1.003 * time)) * .5 * lfo
                pad += attack * voice + (1.0 - attack) * prevVoice
            }
            pad *= .038 * (.6 + .4 * energy)
            val sub = sin(TAU * ROOT_FREQ[chordIndex] * time) * .026

            // Bass: driving eighth notes with a kick-ducked pump.
            val eighth = beat * 2.0
            val eighthTime = (eighth % 1.0) * SECONDS_PER_BEAT / 2.0
            val bassFrequency = BASS_FREQ[chordIndex][eighth.toInt() % EIGHTH_BASS.size]
            val beatSeconds = (beat % 1.0) * SECONDS_PER_BEAT
            val duck = 1.0 - .55 * exp(-beatSeconds * 16.0)
            val bassAttack = (eighthTime * 220.0).coerceIn(0.0, 1.0)
            val bass = (sin(TAU * bassFrequency * eighthTime) +
                .35 * sin(TAU * bassFrequency * 2.0 * eighthTime) +
                .12 * sin(TAU * bassFrequency * 3.0 * eighthTime)) *
                bassAttack * exp(-eighthTime * 12.0) * duck *
                (.05 + .07 * gate(energy, .2, .5))

            // Arp: bright sixteenth pattern, denser and doubled as energy rises.
            val step = (beat * 4.0).toInt()
            val arpFrequency = arpFreq[ARP_PATTERN[step % ARP_PATTERN.size]]
            val sixteenthTime = ((beat * 4.0) % 1.0) * SECONDS_PER_BEAT / 4.0
            val arpEnv = (sixteenthTime * 320.0).coerceIn(0.0, 1.0) * exp(-sixteenthTime * 22.0)
            val oddGate = if (step % 2 == 1) gate(energy, .3, .45) else 1.0
            var arp = (sin(TAU * arpFrequency * sixteenthTime) +
                .3 * sin(TAU * arpFrequency * 2.0 * sixteenthTime)) *
                arpEnv * oddGate * .055 * gate(energy, .15, .5)
            arp += sin(TAU * arpFrequency * 2.0 * sixteenthTime) *
                arpEnv * oddGate * .05 * gate(energy, .75, .9)

            // Kick: four on the floor once the match heats up.
            val kickAttack = (beatSeconds * 500.0).coerceIn(0.0, 1.0)
            val kick = sin(TAU * (45.0 * beatSeconds + 2.0 * (1.0 - exp(-30.0 * beatSeconds)))) *
                exp(-8.0 * beatSeconds) * kickAttack * .30 * drumGate

            // Snare: noise burst plus body on beats 2 and 4.
            val snareTime = when {
                barBeat >= 1.0 && barBeat < 2.0 -> (barBeat - 1.0) * SECONDS_PER_BEAT
                barBeat >= 3.0 -> (barBeat - 3.0) * SECONDS_PER_BEAT
                else -> -1.0
            }
            val snare = if (snareTime >= 0.0) {
                (noise(sample) * exp(-snareTime * 22.0) * .5 +
                    sin(TAU * 190.0 * snareTime) * exp(-snareTime * 28.0) * .5) *
                    (snareTime * 600.0).coerceIn(0.0, 1.0) *
                    .14 * drumGate * gate(energy, .5, .65)
            } else 0.0

            // Hats: offbeat eighths, plus sixteenth ticks at high energy.
            val eighthIndex = (beat * 2.0).toInt()
            val hat = if (eighthIndex % 2 == 1) {
                val hatTime = ((beat * 2.0) % 1.0) * SECONDS_PER_BEAT / 2.0
                (noise(sample) - noise(sample - 1)) * exp(-hatTime * 70.0) * .5 *
                    (hatTime * 900.0).coerceIn(0.0, 1.0) *
                    .05 * drumGate * gate(energy, .4, .55)
            } else 0.0
            val tickTime = ((beat * 4.0) % 1.0) * SECONDS_PER_BEAT / 4.0
            val tick = (noise(sample + 7) - noise(sample + 6)) * exp(-tickTime * 80.0) * .35 *
                (tickTime * 900.0).coerceIn(0.0, 1.0) *
                .04 * drumGate * gate(energy, .78, .9)

            // Riser: noise swell across each bar when trace runs hot.
            val barPhase = (loopBeat % BEATS_PER_BAR) / BEATS_PER_BAR
            val riser = noise(sample + 13) * barPhase * barPhase * .06 * gate(energy, .85, .95)

            // Lead: sparse triumphant motif, victory only.
            val quarter = beat.toInt() % LEAD.size
            val leadNote = LEAD[quarter]
            val quarterTime = (beat % 1.0) * SECONDS_PER_BEAT
            val leadVoice = if (leadNote > 0) {
                sin(TAU * LEAD_FREQ[quarter] * quarterTime + .6 * sin(TAU * 5.5 * time)) *
                    (quarterTime * 40.0).coerceIn(0.0, 1.0) * exp(-quarterTime * 4.0) *
                    .055 * leadLevel
            } else 0.0

            val fade = ((rendered + index).toDouble() / (SAMPLE_RATE * 1.2)).coerceIn(0.0, 1.0)
            val out = (pad + sub + bass + arp + kick + snare + hat + tick + riser + leadVoice) *
                .8 * fade * masterLevel
            buffer[index] = (out.coerceIn(-.85, .85) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Soft 0..1 switch: 0 below [low], 1 above [high], linear between. */
    private fun gate(value: Double, low: Double, high: Double): Double =
        ((value - low) / (high - low)).coerceIn(0.0, 1.0)

    private fun smoothstep(x: Double): Double {
        val t = x.coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }

    private fun targetEnergy(scene: SoundtrackScene, trace: Int): Double = when (scene) {
        // Menus: pads plus a gentle bass line (bass gate .2/.5), still below the drum
        // gate (.35): present at a comfortable level without stealing from SFX.
        SoundtrackScene.MENU -> .34
        SoundtrackScene.GAME -> .45 + .5 * (trace.coerceIn(0, 100) / 100.0)
        SoundtrackScene.VICTORY -> .8
        SoundtrackScene.DEFEAT -> .12
    }

    /** Stateless deterministic white noise in [-1, 1]. */
    private fun noise(sample: Long): Double {
        var h = sample * 0x9E3779B1L + 0x85EBCA77L
        h = h xor (h ushr 13)
        h *= 0xC2B2AE35L
        h = h xor (h ushr 16)
        return ((h ushr 11).toDouble() / 9007199254740992.0) * 2.0 - 1.0
    }

    private companion object {
        const val TAG = "AmbientSoundtrack"
        const val SAMPLE_RATE = 22_050
        const val BUFFER_SAMPLES = 2_048
        const val PREFILL_SAMPLES = BUFFER_SAMPLES * 2
        const val MAX_TRACK_FAILURES = 5
        const val BPM = 100.0
        const val SAMPLES_PER_BEAT = SAMPLE_RATE * 60.0 / BPM
        const val SECONDS_PER_BEAT = 60.0 / BPM
        const val BEATS_PER_BAR = 4.0
        const val LOOP_BEATS = 16.0
        const val CHORDS = 4
        const val TAU = 2.0 * PI
        const val ENERGY_SMOOTHING = 2048.0 / 22050.0 / 3.0
        const val LEAD_SMOOTHING = 2048.0 / 22050.0 / 1.5
        const val VOLUME_SMOOTHING = 2048.0 / 22050.0 / 0.4
        val BASS_ROOTS = intArrayOf(33, 29, 31, 28)
        val PAD_TONES = arrayOf(
            intArrayOf(45, 48, 52),
            intArrayOf(41, 45, 48),
            intArrayOf(43, 47, 50),
            intArrayOf(40, 44, 47),
        )
        val ARP_TONES = arrayOf(
            intArrayOf(57, 60, 64, 67),
            intArrayOf(53, 57, 60, 65),
            intArrayOf(55, 59, 62, 67),
            intArrayOf(52, 56, 59, 64),
        )
        val ARP_PATTERN = intArrayOf(0, 1, 2, 3, 2, 1, 0, 2)
        val EIGHTH_BASS = intArrayOf(0, 0, 12, 0, 0, 0, 7, 12)
        val LEAD = intArrayOf(69, -1, 72, -1, 76, -1, 79, 76, 72, -1, 69, -1, 67, 64, -1, -1)

        // Chord-derived frequencies, precomputed once: calling pow() per sample was the
        // hottest cost in renderBuffer (2048 samples x ~7 calls per buffer) and could
        // delay writes enough to underrun on weak devices.
        val ROOT_FREQ = DoubleArray(CHORDS) { midiFrequency(BASS_ROOTS[it]) }
        val PAD_FREQ = Array(CHORDS) { c ->
            DoubleArray(PAD_TONES[c].size) { i -> midiFrequency(PAD_TONES[c][i]) }
        }
        val ARP_FREQ = Array(ARP_TONES.size) { c ->
            DoubleArray(ARP_TONES[c].size) { i -> midiFrequency(ARP_TONES[c][i]) }
        }
        val BASS_FREQ = Array(CHORDS) { c ->
            DoubleArray(EIGHTH_BASS.size) { i -> midiFrequency(BASS_ROOTS[c] + EIGHTH_BASS[i]) }
        }
        val LEAD_FREQ = DoubleArray(LEAD.size) { if (LEAD[it] > 0) midiFrequency(LEAD[it]) else 0.0 }

        fun midiFrequency(note: Int): Double = 440.0 * 2.0.pow((note - 69) / 12.0)
    }
}
