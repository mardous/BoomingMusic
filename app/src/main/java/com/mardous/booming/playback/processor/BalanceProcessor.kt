package com.mardous.booming.playback.processor

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class BalanceAudioProcessor(
    private var leftGain: Float = 1.0f,
    private var rightGain: Float = 1.0f
) : BaseAudioProcessor() {

    @Synchronized
    fun setBalance(left: Float, right: Float) {
        leftGain = left
        rightGain = right
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        var position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val size = limit - position

        val buffer = replaceOutputBuffer(size)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        while (position < limit) {
            val left = inputBuffer.getShort(position)
            val right = inputBuffer.getShort(position + 2)

            val newLeft = (left * leftGain).toInt()
            val newRight = (right * rightGain).toInt()

            buffer.putShort(newLeft.toShort())
            buffer.putShort(newRight.toShort())

            position += 4
        }

        inputBuffer.position(limit)
        buffer.flip()
    }
}
