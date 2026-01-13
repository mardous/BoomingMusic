package com.mardous.booming.core.model.equalizer

import androidx.compose.runtime.Immutable

@Immutable
class EqBandCapabilities(
    val bandRange: ClosedFloatingPointRange<Float>,
    val bandConfigurations: Set<BandConfiguration>
) {
    val availableBandCounts: List<Int> = bandConfigurations.map { it.bandCount }
    val hasMultipleBandConfigurations: Boolean = bandConfigurations.size > 1

    fun isBandCountSupported(bandCount: Int) =
        bandConfigurations.any { it.bandCount == bandCount }

    fun getFrequencies(bandCount: Int): IntArray {
        return bandConfigurations.first { it.bandCount == bandCount }.bandFrequenciesInHz
    }

    fun getBands(profile: EqProfile, bandCount: Int): List<EqBand> {
        if (bandConfigurations.isEmpty())
            return emptyList()

        val freqInHz = bandConfigurations.first { it.bandCount == bandCount }.bandFrequenciesInHz
        val levels = if (profile.isValid && profile.levels.size == bandCount) {
            profile.levels
        } else {
            FloatArray(bandCount)
        }
        return (0 until bandCount).map {
            EqBand(
                index = it,
                value = levels[it],
                valueRange = bandRange,
                frequencyInHz = freqInHz[it]
            )
        }
    }

    class BandConfiguration(val bandCount: Int, val bandFrequenciesInHz: IntArray) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BandConfiguration

            if (bandCount != other.bandCount) return false
            if (!bandFrequenciesInHz.contentEquals(other.bandFrequenciesInHz)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bandCount
            result = 31 * result + bandFrequenciesInHz.contentHashCode()
            return result
        }
    }

    companion object {
        val Empty = EqBandCapabilities(-1f..0f, emptySet())
    }
}