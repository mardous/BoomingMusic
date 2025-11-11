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

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

class EqualizerSession(
    private val context: Context,
    private val equalizerManager: EqualizerManager
) {

    private val audioSessions = ConcurrentHashMap<Int, EffectSet>()

    @Synchronized
    fun openInternalSession(sessionId: Int, closeExternal: Boolean = false) {
        if (sessionId == 0 || audioSessions.containsKey(sessionId))
            return

        audioSessions[sessionId]?.let {
            Log.d(TAG, "Internal session $sessionId already exists, skipping creation")
            return
        }

        if (closeExternal) {
            closeExternalSession(sessionId)
        }

        runCatching {
            EffectSet(sessionId).also { effectSet ->
                audioSessions[sessionId] = effectSet
            }
        }.onSuccess { effectSet ->
            updateDsp(effectSet)
        }.onFailure {
            Log.e(TAG, "Failed to open EQ session (EffectSet error)", it)
        }
    }

    fun openExternalSession(sessionId: Int, closeInternal: Boolean = false) {
        if (sessionId == 0) return

        Log.d(TAG, "Opening external EQ session for sessionId: $sessionId")

        if (closeInternal) {
            closeInternalSession(sessionId)
        }

        context.sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
                .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        )
    }

    @Synchronized
    fun closeInternalSession(sessionId: Int) {
        if (sessionId == 0) return

        audioSessions.remove(sessionId)?.release()
    }

    fun closeExternalSession(sessionId: Int) {
        if (sessionId == 0) return

        Log.d(TAG, "Closing external EQ session for sessionId: $sessionId")

        context.sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
                .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        )
    }

    @Synchronized
    fun update() {
        runCatching {
            for (session in audioSessions.values) {
                updateDsp(session)
            }
        }.onFailure {
            Log.e(TAG, "DSP update error", it)
        }
    }

    fun release() {
        audioSessions.values.forEach { it.release() }
        audioSessions.clear()
    }

    private fun updateDsp(session: EffectSet) {
        runCatching {
            val globalEnabled = equalizerManager.eqState.isUsable
            if (!globalEnabled) {
                disableAll(session)
                return
            }

            applyEqualizer(session)
            applyVirtualizer(session)
            applyBassBoost(session)
            applyReverb(session)
            applyLoudness(session)
        }.onFailure {
            Log.e(TAG, "Error enabling equalizer!", it)
        }
    }

    private fun applyEqualizer(session: EffectSet) {
        val eqPreset = equalizerManager.currentPreset
        val levels = ShortArray(eqPreset.numberOfBands) { eqPreset.getLevelShort(it) }

        session.enableEqualizer(true)
        session.setEqualizerLevels(levels)
    }

    private fun applyVirtualizer(session: EffectSet) {
        runCatching {
            val state = equalizerManager.virtualizerState
            if (state.isUsable) {
                session.enableVirtualizer(true)
                session.setVirtualizerStrength(state.value.toInt().toShort())
            } else {
                session.enableVirtualizer(false)
            }
        }.onFailure { Log.e(TAG, "Error setting up virtualizer!", it) }
    }

    private fun applyBassBoost(session: EffectSet) {
        runCatching {
            val state = equalizerManager.bassBoostState
            if (state.isUsable) {
                session.enableBassBoost(true)
                session.setBassBoostStrength(state.value.toInt().toShort())
            } else {
                session.enableBassBoost(false)
            }
        }.onFailure { Log.e(TAG, "Error setting up bass boost!", it) }
    }

    private fun applyReverb(session: EffectSet) {
        runCatching {
            val state = equalizerManager.presetReverbState
            if (state.isUsable) {
                session.enablePresetReverb(true)
                session.setReverbPreset(state.value.toShort())
            } else {
                session.enablePresetReverb(false)
            }
        }.onFailure { Log.e(TAG, "Error setting up reverb!", it) }
    }

    private fun applyLoudness(session: EffectSet) {
        runCatching {
            val state = equalizerManager.loudnessGainState
            if (state.isUsable) {
                session.enableLoudness(true)
                session.setLoudnessGain(state.value.toInt())
            } else {
                session.enableLoudness(false)
            }
        }.onFailure { Log.e(TAG, "Error setting up loudness enhancer!", it) }
    }

    private fun disableAll(session: EffectSet) {
        session.enableEqualizer(false)
        session.enableVirtualizer(false)
        session.enableBassBoost(false)
        session.enablePresetReverb(false)
        session.enableLoudness(false)
    }

    companion object {
        private const val TAG = "EqualizerSession"

        fun getZeroedBandsString(length: Int) = (0 until length).joinToString(";") { "0" }
    }
}
