package com.mardous.booming.core.model.audiodevice

import android.media.AudioFormat

data class BitPerfectInfo(
    val deviceName: String,
    val sampleRate: Int,
    val channelCount: Int,
    val encoding: Int
) {
    val encodingLabel: String
        get() = when (encoding) {
            AudioFormat.ENCODING_PCM_16BIT -> "PCM 16-bit"
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> "PCM 24-bit"
            AudioFormat.ENCODING_PCM_32BIT -> "PCM 32-bit"
            AudioFormat.ENCODING_PCM_FLOAT -> "PCM Float"
            else -> "PCM"
        }

    val sampleRateLabel: String
        get() = if (sampleRate >= 1000) {
            val khz = sampleRate / 1000.0
            val formatted = if (khz == khz.toLong().toDouble()) {
                khz.toLong().toString()
            } else {
                khz.toString()
            }
            "$formatted kHz"
        } else {
            "$sampleRate Hz"
        }
}