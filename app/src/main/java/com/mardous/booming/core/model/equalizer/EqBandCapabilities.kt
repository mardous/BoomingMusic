package com.mardous.booming.core.model.equalizer

import androidx.compose.runtime.Immutable

@Immutable
class EqBandCapabilities(
    val bandCount: Int,
    bandRange: IntRange,
    val bandFrequencies: IntArray
) {
    val minimumValue: Float = (bandRange.first / EqBand.VALUE_FACTOR)
    val maximumValue: Float = (bandRange.last / EqBand.VALUE_FACTOR)

    fun getBands(preset: EQPreset): List<EqBand> {
        if (preset.levels.size == bandCount) {
            return (0 until bandCount).map {
                EqBand(
                    index = it,
                    value = preset.getLevelShort(it) / EqBand.VALUE_FACTOR,
                    valueRange = minimumValue..maximumValue,
                    frequency = bandFrequencies[it]
                )
            }
        }
        return emptyList()
    }

    companion object {
        val Empty = EqBandCapabilities(0, IntRange(-1, 0), IntArray(0))
    }
}