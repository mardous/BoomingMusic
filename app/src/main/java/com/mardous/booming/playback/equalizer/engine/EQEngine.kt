@file:Suppress("DEPRECATION")
package com.mardous.booming.playback.equalizer.engine

import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.annotation.CallSuper
import com.mardous.booming.core.model.equalizer.BassBoostState
import com.mardous.booming.core.model.equalizer.CompressorState
import com.mardous.booming.core.model.equalizer.EqBandCapabilities
import com.mardous.booming.core.model.equalizer.EqProfile
import com.mardous.booming.core.model.equalizer.LimiterState
import com.mardous.booming.core.model.equalizer.LoudnessGainState
import com.mardous.booming.core.model.equalizer.VirtualizerState

abstract class EQEngine(val sessionId: Int) {

    private val bassBoost: BassBoost? =
        execOpCatchingWithTag("Create BassBoost") { BassBoost(0, sessionId) }

    private val virtualizer: Virtualizer? =
        execOpCatchingWithTag("Create Virtualizer") { Virtualizer(0, sessionId) }

    private val loudnessEnhancer: LoudnessEnhancer? =
        execOpCatchingWithTag("Create LoudnessEnhancer") { LoudnessEnhancer(sessionId) }

    abstract val isEnabled: Boolean
    abstract val isMBCSupported: Boolean
    abstract val isLimiterSupported: Boolean
    abstract val bandCapabilities: EqBandCapabilities
    abstract fun setEnabled(isEnabled: Boolean)
    abstract fun setProfile(profile: EqProfile)
    abstract fun setBandGain(bandIndex: Int, bandGainInDecibels: Float)
    abstract fun setBandCount(bandCount: Int): Boolean
    abstract fun setInputGain(inputGain: Float): Boolean
    abstract fun setCompressorState(state: CompressorState): Boolean
    abstract fun setLimiterState(state: LimiterState): Boolean

    fun setVirtualizerState(state: VirtualizerState): Boolean {
        return execOpCatching(virtualizer, false) {
            if (it.enabled != state.enabled) {
                it.enabled = state.enabled
                if (!state.enabled) {
                    it.setStrength(0)
                    return true
                }
            }
            val roundedStrength = state.strength.toInt().toShort()
            if (it.roundedStrength != roundedStrength) {
                it.setStrength(roundedStrength)
            }
            return true
        }
    }

    fun setBassBoostState(state: BassBoostState): Boolean {
        return execOpCatching(bassBoost, false) {
            if (it.enabled != state.enabled) {
                it.enabled = state.enabled
                if (!state.enabled) {
                    it.setStrength(0)
                    return true
                }
            }
            val roundedStrength = state.strength.toInt().toShort()
            if (it.roundedStrength != roundedStrength) {
                it.setStrength(roundedStrength)
            }
            return true
        }
    }

    fun setLoudnessGainState(state: LoudnessGainState): Boolean {
        return execOpCatching(loudnessEnhancer, false) {
            if (it.enabled != state.enabled) {
                it.enabled = state.enabled
                if (!state.enabled) {
                    it.setTargetGain(0)
                    return true
                }
            }
            val targetGain = (state.gainInDb * 100).coerceIn(0f, 4000f)
            if (targetGain != state.gainInDb) {
                it.setTargetGain(targetGain.toInt())
            }
            return true
        }
    }

    @CallSuper
    open fun release() {
        bassBoost?.release()
        virtualizer?.release()
        loudnessEnhancer?.release()
    }

    protected inline fun <R> execOpCatchingWithTag(tag: String, callback: () -> R): R? {
        synchronized(this) {
            return try {
                callback()
            } catch (t: Throwable) {
                Log.d("EQEngine", "$tag operation failed!", t)
                null
            }
        }
    }

    protected inline fun <A, reified R> execOpCatching(argument: A?, default: R, callback: (A) -> R): R {
        synchronized(this) {
            if (argument == null)
                return default

            return try {
                callback(argument)
            } catch (t: Throwable) {
                Log.d("EQEngine", "EQ operation failed!", t)
                default
            }
        }
    }
}