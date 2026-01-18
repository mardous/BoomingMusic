package com.mardous.booming.core.model.equalizer

import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
class EqBand(
    val index: Int,
    val value: Float,
    val valueRange: ClosedFloatingPointRange<Float>,
    private val frequencyInHz: Int
) {

    val readableFrequency: String
        get() = if (frequencyInHz >= FREQUENCY_FACTOR) {
            "%.0f kHz".format(Locale.ROOT, (frequencyInHz / FREQUENCY_FACTOR).toFloat())
        } else {
            "%.0f Hz".format(Locale.ROOT, frequencyInHz.toFloat())
        }

    companion object {
        const val FREQUENCY_FACTOR = 1000
    }
}