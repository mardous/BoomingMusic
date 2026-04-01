package com.mardous.booming.core.model.audiodevice

import android.media.AudioFormat
import java.util.Locale

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
            val formatted = when (sampleRate) {
                44100 -> "44.1"
                48000 -> "48"
                else -> {
                    // Format with one decimal place and trim trailing ".0" to avoid
                    // long or imprecise decimal representations like 44.1000003
                    val raw = String.format(Locale.US, "%.1f", khz)
                    if (raw.endsWith(".0")) raw.dropLast(2) else raw
                }
            }
            "$formatted kHz"
        } else {
            "$sampleRate Hz"
        }
}