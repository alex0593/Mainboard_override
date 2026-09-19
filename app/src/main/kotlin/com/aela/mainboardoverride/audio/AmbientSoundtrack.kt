package com.aela.mainboardoverride.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import kotlin.math.pow
import kotlin.math.sin

/** Keeps the low-volume procedural soundtrack alive only while the app is visible. */
@Composable
fun AmbientSoundtrackHost(enabled: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val soundtrack = remember { AmbientSoundtrack() }
    val currentEnabled by rememberUpdatedState(enabled)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (currentEnabled) soundtrack.start() else soundtrack.stop()
                Lifecycle.Event.ON_STOP -> soundtrack.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && currentEnabled) {
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
}

private class AmbientSoundtrack {
    private val lock = Any()
    private var audioTrack: AudioTrack? = null
    private var renderJob: Job? = null

    fun start() {
        synchronized(lock) {
            if (renderJob?.isActive == true) return

            val track = createTrack() ?: return
            audioTrack = track
            track.play()
            renderJob = kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
                render(track)
            }
        }
    }

    fun stop() {
        val job: Job?
        val track: AudioTrack?
        synchronized(lock) {
            job = renderJob
            track = audioTrack
            renderJob = null
            audioTrack = null
        }
        job?.cancel()
        runCatching {
            track?.pause()
            track?.flush()
            track?.release()
        }
    }

    private fun createTrack(): AudioTrack? = runCatching {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(BUFFER_SAMPLES * 2) * 2
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
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }.getOrNull()

    private suspend fun render(track: AudioTrack) = withContext(Dispatchers.Default) {
        val samples = ShortArray(BUFFER_SAMPLES)
        var cursor = 0L
        while (isActive) {
            renderBuffer(samples, cursor)
            if (track.write(samples, 0, samples.size) < 0) break
            cursor += samples.size
        }
    }

    private fun renderBuffer(buffer: ShortArray, startSample: Long) {
        for (index in buffer.indices) {
            val sample = startSample + index
            val beat = sample.toDouble() / SAMPLES_PER_BEAT
            val step = (beat * 4.0).toInt() % ARPEGGIO.size
            val stepProgress = (beat * 4.0) % 1.0
            val arpFrequency = midiFrequency(ARPEGGIO[step])
            val bassFrequency = midiFrequency(BASS[(beat.toInt() / 4) % BASS.size])

            val pad = sin(TAU * 55.0 * sample / SAMPLE_RATE) * .045
            val bassEnvelope = .5 + .5 * sin(TAU * bassFrequency * sample / SAMPLE_RATE)
            val bass = sin(TAU * bassFrequency * sample / SAMPLE_RATE) * (.11 + .04 * bassEnvelope)
            val arpEnvelope = if (stepProgress < .82) {
                (1.0 - stepProgress / .82).coerceAtLeast(0.0)
            } else 0.0
            val arp = sin(TAU * arpFrequency * sample / SAMPLE_RATE) * .075 * arpEnvelope
            val pulseEnvelope = expPulse(stepProgress)
            val pulse = sin(TAU * 110.0 * sample / SAMPLE_RATE) * .025 * pulseEnvelope
            val stereoSafeSample = (pad + bass + arp + pulse).coerceIn(-.8, .8)
            buffer[index] = (stereoSafeSample * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun expPulse(progress: Double): Double = when {
        progress < .08 -> 1.0 - progress / .08
        else -> 0.0
    }

    private fun midiFrequency(note: Int): Double = 440.0 * 2.0.pow((note - 69) / 12.0)

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val BUFFER_SAMPLES = 2_048
        const val BPM = 76.0
        const val SAMPLES_PER_BEAT = SAMPLE_RATE * 60.0 / BPM
        const val TAU = 2.0 * PI
        val ARPEGGIO = intArrayOf(57, 60, 64, 67, 64, 60, 55, 60, 57, 60, 65, 69, 65, 60, 55, 60)
        val BASS = intArrayOf(33, 33, 36, 31, 33, 33, 38, 31)
    }
}
