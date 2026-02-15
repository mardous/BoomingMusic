package com.mardous.booming.playback.equalizer.engine

import android.media.audiofx.Equalizer
import com.mardous.booming.core.model.equalizer.CompressorState
import com.mardous.booming.core.model.equalizer.EqBandCapabilities
import com.mardous.booming.core.model.equalizer.EqProfile
import com.mardous.booming.core.model.equalizer.LimiterState
import kotlin.math.roundToInt

class BasicEQEngine(sessionId: Int) : EQEngine(sessionId) {

    private var equalizer: Equalizer? = execOpCatchingWithTag("Create Equalizer") {
        Equalizer(0, sessionId)
    }

    override val isEnabled: Boolean
        get() = execOpCatching(equalizer, false) { it.enabled }

    override val isMBCSupported: Boolean = false
    override val isLimiterSupported: Boolean = false

    override val bandCapabilities: EqBandCapabilities
        get() = execOpCatching(equalizer, EqBandCapabilities.Empty) { eq ->
            val bandCount = eq.numberOfBands.toInt()
            val bandLevelRange = eq.bandLevelRange.map { (it / MILLIBELS_FACTOR).toFloat() }
            val bandFrequenciesInHz = (0 until bandCount).map {
                val centerFreqInMilliHertz = eq.getCenterFreq(it.toShort())
                (centerFreqInMilliHertz / MILLIHERTZ_FACTOR) // convert to Hz
            }
            EqBandCapabilities(
                bandRange = bandLevelRange[0]..bandLevelRange[1],
                bandConfigurations = setOf(
                    EqBandCapabilities.BandConfiguration(
                        bandCount = bandCount,
                        bandFrequenciesInHz = bandFrequenciesInHz.toIntArray()
                    )
                )
            )
        }

    override fun setEnabled(isEnabled: Boolean) {
        execOpCatching(equalizer, Unit) { eq ->
            if (eq.enabled != isEnabled) {
                eq.enabled = isEnabled
            }
        }
    }

    override fun setProfile(profile: EqProfile) {
        execOpCatching(equalizer, Unit) { eq ->
            val bandCount = eq.numberOfBands.toInt()
            if (bandCount > 0 && profile.numberOfBands == bandCount) {
                for (i in 0 until bandCount) {
                    val bandGainInMillibels = (profile.levels[i] * MILLIBELS_FACTOR).roundToInt()
                    eq.setBandLevel(i.toShort(), bandGainInMillibels.toShort())
                }
            }
        }
    }

    override fun setBandGain(bandIndex: Int, bandGainInDecibels: Float) {
        execOpCatching(equalizer, Unit) { eq ->
            val bandGainInMillibels = (bandGainInDecibels * MILLIBELS_FACTOR).roundToInt()
            eq.setBandLevel(bandIndex.toShort(), bandGainInMillibels.toShort())
        }
    }

    override fun setBandCount(bandCount: Int): Boolean = false

    override fun setInputGain(inputGain: Float): Boolean = false

    override fun setCompressorState(state: CompressorState): Boolean = false

    override fun setLimiterState(state: LimiterState): Boolean = false

    override fun release() {
        super.release()
        execOpCatching(equalizer, Unit) {
            it.release()
        }
    }

    companion object {
        private const val MILLIHERTZ_FACTOR = 1000
        private const val MILLIBELS_FACTOR = 100
    }
}