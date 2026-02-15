package com.mardous.booming.playback.equalizer.engine

import android.media.audiofx.DynamicsProcessing
import com.mardous.booming.core.model.equalizer.CompressorState
import com.mardous.booming.core.model.equalizer.EqBandCapabilities
import com.mardous.booming.core.model.equalizer.EqBandCapabilities.BandConfiguration
import com.mardous.booming.core.model.equalizer.EqProfile
import com.mardous.booming.core.model.equalizer.LimiterState

class DynamicsProcessingEngine(sessionId: Int, bandCount: Int) : EQEngine(sessionId) {

    private var dynamicsProcessing = createDynamicsProcessing(bandCount)

    private var compressorState: CompressorState? = null
    private var limiterState: LimiterState? = null

    override val isMBCSupported: Boolean = false // disabled for now
    override val isLimiterSupported: Boolean = true

    override val isEnabled: Boolean
        get() = execOpCatching(dynamicsProcessing, false) { it.enabled }

    override val bandCapabilities: EqBandCapabilities
        get() = EqBandCapabilities(
            bandRange = BAND_RANGE,
            bandConfigurations = arrayOf(5, 10, 15).mapTo(mutableSetOf()) {
                BandConfiguration(it, getFreqInHzByBandCount(it))
            }
        )

    override fun setEnabled(isEnabled: Boolean) {
        execOpCatching(dynamicsProcessing, Unit) {
            if (it.enabled != isEnabled) {
                it.enabled = isEnabled
            }
        }
    }

    override fun setProfile(profile: EqProfile) {
        execOpCatching(dynamicsProcessing, Unit) { dp ->
            if (profile.isValid) {
                for (channelIndex in 0 until dp.channelCount) {
                    val preEq = dp.getPreEqByChannelIndex(channelIndex)
                    if (preEq.bandCount == profile.numberOfBands) {
                        profile.levels.forEachIndexed { bandIndex, bandGainInDecibels ->
                            val band = preEq.getBand(bandIndex)
                            if (band.gain != bandGainInDecibels) {
                                band.gain = bandGainInDecibels
                                preEq.setBand(bandIndex, band)
                            }
                        }
                        dp.setPreEqByChannelIndex(channelIndex, preEq)
                    }
                }
            }
        }
    }

    override fun setBandGain(bandIndex: Int, bandGainInDecibels: Float) {
        execOpCatching(dynamicsProcessing, Unit) { dp ->
            for (channelIndex in 0 until dp.channelCount) {
                val preEq = dp.getPreEqByChannelIndex(channelIndex)
                val band = preEq.getBand(bandIndex)
                if (band.gain != bandGainInDecibels) {
                    band.gain = bandGainInDecibels
                    preEq.setBand(bandIndex, band)
                }
                dp.setPreEqByChannelIndex(channelIndex, preEq)
            }
        }
    }

    override fun setBandCount(bandCount: Int): Boolean {
        val oldIsEnabled = this.isEnabled
        val released = execOpCatching(dynamicsProcessing, false) { dp ->
            if (dp.getPreEqByChannelIndex(dp.channelCount - 1).bandCount == bandCount) {
                return true
            }
            dp.setEnabled(false)
            dp.release()
            true
        }
        if (released) {
            dynamicsProcessing = createDynamicsProcessing(bandCount)
            return execOpCatching(dynamicsProcessing, false) { newDp ->
                newDp.setEnabled(oldIsEnabled)
                newDp.getPreEqByChannelIndex(newDp.channelCount - 1).bandCount == bandCount
            }
        }
        return false
    }

    override fun setInputGain(inputGain: Float): Boolean {
        return execOpCatching(dynamicsProcessing, false) { dp ->
            dp.setInputGainAllChannelsTo(inputGain)
            true
        }
    }

    override fun setCompressorState(state: CompressorState): Boolean {
        return if (this.compressorState != state) {
            this.compressorState = state

            execOpCatching(dynamicsProcessing, false) { dp ->
                for (channelIndex in 0 until CHANNEL_COUNT) {
                    val channel = dp.getChannelByChannelIndex(channelIndex)
                    val mbc = DynamicsProcessing.Mbc(channel.mbc)

                    if (mbc.isEnabled != state.enabled)
                        mbc.isEnabled = state.enabled

                    for (bandIndex in 0 until mbc.bandCount) {
                        val band = mbc.getBand(bandIndex)
                        if (band.ratio != state.ratio)
                            band.ratio = state.ratio
                        if (band.expanderRatio != state.expanderRatio)
                            band.expanderRatio = state.expanderRatio
                        if (band.threshold != state.threshold)
                            band.threshold = state.threshold
                        if (band.attackTime != state.attackTimeMs)
                            band.attackTime = state.attackTimeMs
                        if (band.releaseTime != state.releaseTimeMs)
                            band.releaseTime = state.releaseTimeMs
                        if (band.preGain != state.preGain)
                            band.preGain = state.preGain
                        if (band.postGain != state.postGain)
                            band.postGain = state.postGain
                        if (band.kneeWidth != state.kneeWidth)
                            band.kneeWidth = state.kneeWidth
                        if (band.noiseGateThreshold != state.noiseGateThreshold)
                            band.noiseGateThreshold = state.noiseGateThreshold
                    }

                    channel.mbc = mbc
                }
                true
            }
        } else false
    }

    override fun setLimiterState(state: LimiterState): Boolean {
        return if (this.limiterState != state) {
            this.limiterState = state

            execOpCatching(dynamicsProcessing, false) { dp ->
                for (channelIndex in 0 until CHANNEL_COUNT) {
                    val channel = dp.getChannelByChannelIndex(channelIndex)
                    val limiter = DynamicsProcessing.Limiter(channel.limiter)

                    if (limiter.isEnabled != state.enabled)
                        limiter.isEnabled = state.enabled
                    if (limiter.ratio != state.ratio)
                        limiter.ratio = state.ratio
                    if (limiter.threshold != state.threshold)
                        limiter.threshold = state.threshold
                    if (limiter.attackTime != state.attackTimeMs)
                        limiter.attackTime = state.attackTimeMs
                    if (limiter.releaseTime != state.releaseTimeMs)
                        limiter.releaseTime = state.releaseTimeMs
                    if (limiter.postGain != state.postGain)
                        limiter.postGain = state.postGain

                    channel.limiter = limiter
                }
                true
            }
        } else false
    }

    override fun release() {
        super.release()
        execOpCatching(dynamicsProcessing, Unit) {
            it.setEnabled(false)
        }
        dynamicsProcessing?.release()
        dynamicsProcessing = null
    }

    private fun createDynamicsProcessing(bandCount: Int) =
        execOpCatchingWithTag("Create DynamicsProcessing") {
            val builder = DynamicsProcessing.Config.Builder(
                /* variant */ DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                /* channelCount */ CHANNEL_COUNT,
                /* preEqInUse */ true,
                /* preEqBandCount */ bandCount,
                /* mbcInUse */ false,
                /* mbcBandCount */ 0,
                /* postEqInUse */ false,
                /* postEqBandCount */ 0,
                /* limiterInUse */ false //disabled for now
            )
            DynamicsProcessing(0, sessionId, builder.build()).also { dp ->
                for (channelIndex in 0 until dp.channelCount) {
                    val preEq = dp.getPreEqByChannelIndex(channelIndex)
                    val freqInHz = getFreqInHzByBandCount(preEq.bandCount)
                    for (bandIndex in 0 until preEq.bandCount) {
                        preEq.getBand(bandIndex).isEnabled = true
                        preEq.getBand(bandIndex).cutoffFrequency = freqInHz[bandIndex].toFloat()
                    }
                    preEq.isEnabled = true
                    dp.setPreEqByChannelIndex(channelIndex, preEq)
                }
            }
        }

    private fun getFreqInHzByBandCount(bandCount: Int) = when (bandCount) {
        15 -> BAND_FREQUENCIES_IN_HZ_15
        10 -> BAND_FREQUENCIES_IN_HZ_10
        else -> BAND_FREQUENCIES_IN_HZ_5
    }

    companion object {
        private const val CHANNEL_COUNT = 2

        private val BAND_RANGE = -15f..15f
        private val BAND_FREQUENCIES_IN_HZ_5 = intArrayOf(
            60, 230, 910, 3600, 14000
        )
        private val BAND_FREQUENCIES_IN_HZ_10 = intArrayOf(
            31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000
        )
        private val BAND_FREQUENCIES_IN_HZ_15 = intArrayOf(
            25, 40, 63, 100, 160, 250, 400, 630, 1000,
            1600, 2500, 4000, 6300, 10000, 16000
        )
    }
}