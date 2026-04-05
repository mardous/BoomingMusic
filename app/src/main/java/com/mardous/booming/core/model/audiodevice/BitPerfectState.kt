package com.mardous.booming.core.model.audiodevice

import android.media.AudioFormat
import java.util.Locale

sealed class BitPerfectState(val isActive: Boolean, val isVolumeFixed: Boolean) {
    abstract val encodingLabel: String
    abstract val sampleRateLabel: String

    class Inactive(isVolumeFixed: Boolean) : BitPerfectState(false, isVolumeFixed) {
        override val encodingLabel: String = "N/A"
        override val sampleRateLabel: String = "N/A"
    }

    class Active(
        val deviceName: String,
        val sampleRate: Int,
        val channelCount: Int,
        val encoding: Int,
        isVolumeFixed: Boolean
    ) : BitPerfectState(true, isVolumeFixed) {

        override val encodingLabel: String
            get() = when (encoding) {
                AudioFormat.ENCODING_PCM_16BIT -> "PCM 16-bit"
                AudioFormat.ENCODING_PCM_24BIT_PACKED -> "PCM 24-bit"
                AudioFormat.ENCODING_PCM_32BIT -> "PCM 32-bit"
                AudioFormat.ENCODING_PCM_FLOAT -> "PCM Float"
                else -> "PCM"
            }

        override val sampleRateLabel: String
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
}