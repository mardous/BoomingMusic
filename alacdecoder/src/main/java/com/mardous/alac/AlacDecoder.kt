/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.alac

import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.decoder.SimpleDecoder
import androidx.media3.decoder.SimpleDecoderOutputBuffer
import java.nio.ByteBuffer

@UnstableApi
class AlacDecoder(
    numInputBuffers: Int,
    numOutputBuffers: Int,
    internal val sampleRate: Int,
    internal val channels: Int,
    internal val bitDepth: Int,
    internal val cookie: ByteArray?
) : SimpleDecoder<DecoderInputBuffer, SimpleDecoderOutputBuffer, AlacDecoderException>(
    arrayOfNulls(numInputBuffers), arrayOfNulls(numOutputBuffers)
) {

    private var nativeContext: Long = 0

    // The default value should be the same as kALACDefaultFrameSize in ALACAudioTypes.h
    private var frameLength: Int = 4096

    external fun nativeInit(sampleRate: Int, channels: Int, bps: Int, cookie: ByteArray?): Long
    external fun nativeGetFrameLength(contextPtr: Long): Int
    external fun nativeDecode(contextPtr: Long, input: ByteBuffer, output: ByteBuffer, size: Int): Int
    external fun nativeRelease(contextPtr: Long)

    init {
        nativeContext = nativeInit(sampleRate, channels, bitDepth, cookie)
        if (nativeContext == 0L) {
            throw AlacDecoderException("The native decoder couldn't be initialized")
        }
        frameLength = nativeGetFrameLength(nativeContext)
    }

    override fun getName(): String = "AlacDecoder"

    override fun createInputBuffer(): DecoderInputBuffer {
        return DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT)
    }

    override fun createOutputBuffer(): SimpleDecoderOutputBuffer {
        return SimpleDecoderOutputBuffer { releaseOutputBuffer(it) }
    }

    override fun createUnexpectedDecodeException(error: Throwable): AlacDecoderException {
        return AlacDecoderException("Unexpected error during decodification", error)
    }

    override fun decode(
        inputBuffer: DecoderInputBuffer,
        outputBuffer: SimpleDecoderOutputBuffer,
        reset: Boolean
    ): AlacDecoderException? {
        val inputData = inputBuffer.data ?: return null

        val outputSizeNeeded = frameLength * channels * (bitDepth / 8)
        val outputData = outputBuffer.init(inputBuffer.timeUs, outputSizeNeeded)

        val bytesDecoded = nativeDecode(
            nativeContext,
            inputData,
            outputData,
            inputData.remaining()
        )

        return if (bytesDecoded >= 0) {
            null
        } else {
            AlacDecoderException("Native decoder error")
        }
    }

    override fun release() {
        super.release()
        nativeRelease(nativeContext)
    }

    companion object {
        init {
            System.loadLibrary("alac_decoder")
        }
    }
}