package com.mardous.booming.core.model.equalizer

import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
class EqBand(
    val index: Int,
    val value: Float,
    val valueRange: ClosedFloatingPointRange<Float>,
    private val frequency: Int
) {
    val readableLevel: String
        get() = "%+.0f dB".format(Locale.ROOT, value)

    val readableFrequency: String
        get() = if (frequency >= FREQUENCY_FACTOR) {
            "%.0f kHz".format(Locale.ROOT, frequency / FREQUENCY_FACTOR)
        } else {
            "%.0f Hz".format(Locale.ROOT, frequency.toFloat())
        }

    fun getActualLevel(sliderValue: Float): Int {
        return (sliderValue * VALUE_FACTOR).toInt()
    }

    companion object {
        const val VALUE_FACTOR = 100.0f
        const val FREQUENCY_FACTOR = 1000f
    }
}