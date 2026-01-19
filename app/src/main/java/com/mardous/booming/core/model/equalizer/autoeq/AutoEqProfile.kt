package com.mardous.booming.core.model.equalizer.autoeq

import kotlinx.serialization.Serializable
import kotlin.math.log10

@Serializable
class AutoEqPoint(
    val frequency: Float,
    val gain: Float
)

@Serializable
data class AutoEqProfile(
    val name: String,
    val points: List<AutoEqPoint>,
    val preamp: Float = 0f
) {

    fun getBandGains(
        targetFrequencies: IntArray,
        validGainRange: ClosedFloatingPointRange<Float>
    ): FloatArray {
        val sortedPoints = points.sortedBy { it.frequency }
        val bandGains = FloatArray(targetFrequencies.size)
        for (i in targetFrequencies.indices) {
            val interpolatedGain = interpolate(sortedPoints, targetFrequencies[i].toFloat())
            bandGains[i] = (interpolatedGain + preamp).coerceIn(validGainRange)
        }
        return bandGains
    }

    private fun interpolate(sortedPoints: List<AutoEqPoint>, targetFrequency: Float): Float {
        if (sortedPoints.isEmpty()) return 0f

        if (targetFrequency <= sortedPoints.first().frequency) {
            return sortedPoints.first().gain
        }
        if (targetFrequency >= sortedPoints.last().frequency) {
            return sortedPoints.last().gain
        }

        var p1 = sortedPoints.first()
        var p2 = sortedPoints.last()

        for (i in 0 until sortedPoints.size - 1) {
            if (targetFrequency >= sortedPoints[i].frequency &&
                targetFrequency <= sortedPoints[i + 1].frequency) {
                p1 = sortedPoints[i]
                p2 = sortedPoints[i + 1]
                break
            }
        }

        val logTarget = log10(targetFrequency)
        val logF1 = log10(p1.frequency)
        val logF2 = log10(p2.frequency)

        if (logF2 == logF1) return p1.gain

        val ratio = (logTarget - logF1) / (logF2 - logF1)
        return p1.gain + (p2.gain - p1.gain) * ratio
    }
}