/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mardous.booming.playback.equalizer

import android.media.audiofx.*
import android.util.Log

@Suppress("DEPRECATION")
class EffectSet(sessionId: Int) {

    private var eqNumPresets: Short = -1
    private var eqNumBands: Short = -1

    val equalizer: Equalizer? =
        createEffect("Equalizer") { Equalizer(0, sessionId) }

    private val bassBoost: BassBoost? =
        createEffect("BassBoost") { BassBoost(0, sessionId) }

    private val virtualizer: Virtualizer? =
        createEffect("Virtualizer") { Virtualizer(0, sessionId) }

    private val loudnessEnhancer: LoudnessEnhancer? =
        createEffect("LoudnessEnhancer") { LoudnessEnhancer(sessionId) }

    private val presetReverb: PresetReverb? = null

    private inline fun <T> createEffect(tag: String, block: () -> T): T? = try {
        block()
    } catch (t: Throwable) {
        Log.e("EffectSet", "$tag init failed", t)
        null
    }

    fun enableEqualizer(enable: Boolean) {
        equalizer?.let {
            if (enable != it.enabled) {
                if (!enable) resetEqualizer(it)
                it.enabled = enable
            }
        }
    }

    private fun resetEqualizer(eq: Equalizer) {
        for (i in 0 until getNumEqualizerBands()) {
            eq.setBandLevel(i.toShort(), 0)
        }
    }

    fun setEqualizerLevels(levels: ShortArray) {
        equalizer?.takeIf { it.enabled }?.let { eq ->
            levels.forEachIndexed { i, level ->
                if (eq.getBandLevel(i.toShort()) != level) {
                    eq.setBandLevel(i.toShort(), level)
                }
            }
        }
    }

    fun getNumEqualizerBands(): Short {
        if (equalizer == null) return 0
        if (eqNumBands < 0) {
            eqNumBands = equalizer.numberOfBands.coerceAtMost(6)
        }
        return eqNumBands
    }

    fun getNumEqualizerPresets(): Short {
        if (equalizer == null) return 0
        if (eqNumPresets < 0) {
            eqNumPresets = equalizer.numberOfPresets
        }
        return eqNumPresets
    }

    fun enableBassBoost(enable: Boolean) {
        bassBoost?.let {
            if (enable != it.enabled) {
                if (!enable) it.setStrength(0)
                it.enabled = enable
            }
        }
    }

    fun setBassBoostStrength(strength: Short) {
        bassBoost?.takeIf { it.enabled && it.roundedStrength != strength }
            ?.setStrength(strength)
    }

    fun enableVirtualizer(enable: Boolean) {
        virtualizer?.let {
            if (enable != it.enabled) {
                if (!enable) it.setStrength(0)
                it.enabled = enable
            }
        }
    }

    fun setVirtualizerStrength(strength: Short) {
        virtualizer?.takeIf { it.enabled && it.roundedStrength != strength }
            ?.setStrength(strength)
    }

    fun enablePresetReverb(enable: Boolean) {
        presetReverb?.let {
            if (enable != it.enabled) {
                if (!enable) it.preset = PresetReverb.PRESET_NONE
                it.enabled = enable
            }
        }
    }

    fun setReverbPreset(preset: Short) {
        presetReverb?.takeIf { it.enabled && it.preset != preset }
            ?.preset = preset
    }

    fun enableLoudness(enable: Boolean) {
        loudnessEnhancer?.let {
            if (enable != it.enabled) {
                if (!enable) it.setTargetGain(0)
                it.enabled = enable
            }
        }
    }

    fun setLoudnessGain(gainmDB: Int) {
        loudnessEnhancer?.takeIf { it.enabled && it.targetGain != gainmDB.toFloat() }
            ?.setTargetGain(gainmDB)
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        presetReverb?.release()
        loudnessEnhancer?.release()
    }
}
