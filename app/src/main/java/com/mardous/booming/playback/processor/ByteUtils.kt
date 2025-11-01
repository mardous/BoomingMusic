package com.mardous.booming.playback.processor

import java.nio.ByteBuffer

object ByteUtils {
    fun ByteBuffer.getInt24(): Int {
        val b0 = get().toInt() and 0xFF
        val b1 = get().toInt() and 0xFF
        val b2 = get().toInt()
        return (b2 shl 16) or (b1 shl 8) or b0
    }

    fun ByteBuffer.putInt24(sample: Int): ByteBuffer {
        put((sample and 0xFF).toByte())
        put((sample ushr 8 and 0xFF).toByte())
        put((sample ushr 16 and 0xFF).toByte())
        return this
    }

    const val Int24_MIN_VALUE = -8388608
    const val Int24_MAX_VALUE = 8388607
}
