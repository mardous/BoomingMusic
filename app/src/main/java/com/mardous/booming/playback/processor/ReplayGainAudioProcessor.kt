package com.mardous.booming.playback.processor

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.mardous.booming.data.model.replaygain.ReplayGain
import com.mardous.booming.data.model.replaygain.ReplayGainMode
import com.mardous.booming.playback.processor.ByteUtils.getInt24
import com.mardous.booming.playback.processor.ByteUtils.putInt24
import java.nio.ByteBuffer
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

@UnstableApi
class ReplayGainAudioProcessor(
    var mode: ReplayGainMode,
    var preAmpGain: Float = 0f,
    var preAmpGainWithoutTag: Float = 0f
) : BaseAudioProcessor() {

    var currentGain: ReplayGain? = null
        @Synchronized get
        @Synchronized set

    private val gain: Float
        get() = currentGain?.let { rg ->
            var adjustDB: Float
            var peak: Float

            when (mode) {
                ReplayGainMode.Album -> {
                    adjustDB = if (rg.albumGain != 0f) rg.albumGain else rg.trackGain
                    peak = if (rg.albumPeak != 1f) rg.albumPeak else rg.trackPeak
                }

                ReplayGainMode.Track -> {
                    adjustDB = if (rg.trackGain != 0f) rg.trackGain else rg.albumGain
                    peak = if (rg.trackPeak != 1f) rg.trackPeak else rg.albumPeak
                }

                ReplayGainMode.Off -> return 0f
            }

            if (adjustDB == 0f) {
                adjustDB = preAmpGainWithoutTag
            } else {
                adjustDB += preAmpGain

                if (peak in 0f..1f) {
                    val peakDB = -20f * log10(peak.toDouble()).toFloat()
                    adjustDB = min(adjustDB, peakDB)
                }
            }

            adjustDB
        } ?: 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_24BIT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val g = gain
        if (g != 0f) {
            val size = inputBuffer.remaining()
            val buffer = replaceOutputBuffer(size)
            val delta = 10.0.pow(g / 20.0)

            when (outputAudioFormat.encoding) {
                C.ENCODING_PCM_16BIT -> {
                    while (inputBuffer.hasRemaining()) {
                        val sample = inputBuffer.short
                        val scaled = (sample * delta).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                        buffer.putShort(scaled)
                    }
                }

                C.ENCODING_PCM_24BIT -> {
                    while (inputBuffer.hasRemaining()) {
                        val sample = inputBuffer.getInt24()
                        val scaled = (sample * delta).toInt()
                            .coerceIn(ByteUtils.Int24_MIN_VALUE, ByteUtils.Int24_MAX_VALUE)
                        buffer.putInt24(scaled)
                    }
                }

                else -> { /* No-op */ }
            }

            inputBuffer.position(inputBuffer.limit())
            buffer.flip()
        } else {
            val remaining = inputBuffer.remaining()
            if (remaining > 0) {
                replaceOutputBuffer(remaining).put(inputBuffer).flip()
            }
        }
    }
}

