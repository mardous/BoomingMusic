/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.core.audio

import android.content.Context
import androidx.core.content.edit
import com.mardous.booming.core.model.equalizer.*
import com.mardous.booming.data.model.replaygain.ReplayGainMode
import com.mardous.booming.playback.equalizer.EqualizerManager.Companion.PREFERENCES_NAME
import com.mardous.booming.util.PLAYBACK_PITCH
import com.mardous.booming.util.PLAYBACK_SPEED
import com.mardous.booming.util.Preferences.enumValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SoundSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _balanceFlow = MutableStateFlow(createBalanceState())
    val balanceFlow = _balanceFlow.asStateFlow()
    val balance get() = _balanceFlow.value.value

    private val _tempoFlow = MutableStateFlow(createTempoState())
    val tempoFlow = _tempoFlow.asStateFlow()
    val tempo get() = _tempoFlow.value.value

    private val _replayGainStateFlow = MutableStateFlow(createReplayGainState())
    val replayGainStateFlow = _replayGainStateFlow.asStateFlow()
    val replayGainState get() = replayGainStateFlow.value.value

    private val _audioFloatOutputFlow = MutableStateFlow(prefs.getBoolean(AUDIO_FLOAT_OUTPUT, false))
    val audioFloatOutputFlow = _audioFloatOutputFlow.asStateFlow()

    private val _skipSilenceFlow = MutableStateFlow(prefs.getBoolean(SKIP_SILENCE, false))
    val skipSilenceFlow = _skipSilenceFlow.asStateFlow()

    suspend fun setEnableAudioFloatOutput(enable: Boolean) {
        _audioFloatOutputFlow.emit(enable)
        prefs.edit(commit = true) { putBoolean(AUDIO_FLOAT_OUTPUT, enable) }
    }

    suspend fun setEnableSkipSilence(enable: Boolean) {
        _skipSilenceFlow.emit(enable)
        prefs.edit(commit = true) { putBoolean(SKIP_SILENCE, enable) }
    }

    suspend fun setBalance(update: EqEffectUpdate<BalanceLevel>, apply: Boolean) {
        val newState = update.toState().also {
            if (apply) it.apply()
        }
        _balanceFlow.emit(newState)
    }

    suspend fun setTempo(update: EqEffectUpdate<TempoLevel>, apply: Boolean) {
        val newState = update.toState().also {
            if (apply) it.apply()
        }
        _tempoFlow.emit(newState)
    }

    suspend fun setReplayGain(update: EqEffectUpdate<ReplayGainState>, apply: Boolean) {
        val newState = update.toState().also {
            if (apply) it.apply()
        }
        _replayGainStateFlow.emit(newState)
    }

    suspend fun applyPendingState() {
        balanceFlow.value.apply()
        tempoFlow.value.apply()
        replayGainStateFlow.value.apply()
    }

    private fun createBalanceState(): EqEffectState<BalanceLevel> {
        val balance = BalanceLevel(
            left = prefs.getFloat(LEFT_BALANCE, MAX_BALANCE),
            right = prefs.getFloat(RIGHT_BALANCE, MAX_BALANCE)
        )
        return EqEffectState(
            isSupported = true,
            isEnabled = true,
            value = balance,
            onCommitEffect = {
                prefs.edit {
                    putFloat(LEFT_BALANCE, it.value.left)
                    putFloat(RIGHT_BALANCE, it.value.right)
                }
            }
        )
    }

    private fun createTempoState(): EqEffectState<TempoLevel> {
        val tempo = TempoLevel(
            speed = prefs.getFloat(SPEED, 1f),
            pitch = prefs.getFloat(PITCH, 1f),
            isFixedPitch = prefs.getBoolean(IS_FIXED_PITCH, true)
        )
        return EqEffectState(
            isSupported = true,
            isEnabled = true,
            value = tempo,
            onCommitEffect = {
                prefs.edit {
                    putFloat(SPEED, it.value.speed)
                    putFloat(PITCH, it.value.pitch)
                    putBoolean(IS_FIXED_PITCH, it.value.isFixedPitch)
                }
            }
        )
    }

    private fun createReplayGainState(): EqEffectState<ReplayGainState> {
        val replayGain = ReplayGainState(
            mode = prefs.enumValue(REPLAYGAIN_MODE, ReplayGainMode.Off),
            preamp = prefs.getFloat(REPLAYGAIN_PREAMP, 0f),
            preampWithoutGain = prefs.getFloat(REPLAYGAIN_PREAMP_WITHOUT_GAIN, 0f)
        )
        return EqEffectState(
            isSupported = true,
            isEnabled = replayGain.mode.isOn,
            value = replayGain,
            onCommitEffect = {
                prefs.edit {
                    putFloat(REPLAYGAIN_PREAMP, it.value.preamp)
                    putFloat(REPLAYGAIN_PREAMP_WITHOUT_GAIN, it.value.preampWithoutGain)
                    putString(REPLAYGAIN_MODE, it.value.mode.name)
                }
            }
        )
    }

    companion object {
        private const val AUDIO_FLOAT_OUTPUT = "audio.float_output"
        private const val SKIP_SILENCE = "audio.skip_silence"
        private const val REPLAYGAIN_MODE = "replaygain.mode"
        private const val REPLAYGAIN_PREAMP = "replaygain.preamp"
        private const val REPLAYGAIN_PREAMP_WITHOUT_GAIN = "replaygain.preamp.without_gain"
        private const val LEFT_BALANCE = "equalizer.balance.left"
        private const val RIGHT_BALANCE = "equalizer.balance.right"
        private const val SPEED = PLAYBACK_SPEED
        private const val PITCH = PLAYBACK_PITCH
        private const val IS_FIXED_PITCH = "equalizer.pitch.fixed"
    }
}