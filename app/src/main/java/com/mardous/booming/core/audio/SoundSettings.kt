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
import com.mardous.booming.core.model.equalizer.BalanceLevel
import com.mardous.booming.core.model.equalizer.MAX_BALANCE
import com.mardous.booming.core.model.equalizer.ReplayGainState
import com.mardous.booming.core.model.equalizer.TempoLevel
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

    private val _tempoFlow = MutableStateFlow(createTempoState())
    val tempoFlow = _tempoFlow.asStateFlow()

    private val _replayGainStateFlow = MutableStateFlow(createReplayGainState())
    val replayGainStateFlow = _replayGainStateFlow.asStateFlow()

    private val _audioOffloadFlow = MutableStateFlow(prefs.getBoolean(AUDIO_OFFLOAD, false))
    val audioOffloadFlow = _audioOffloadFlow.asStateFlow()

    private val _audioFloatOutputFlow = MutableStateFlow(prefs.getBoolean(AUDIO_FLOAT_OUTPUT, false))
    val audioFloatOutputFlow = _audioFloatOutputFlow.asStateFlow()
    val audioFloatOutput get() = audioFloatOutputFlow.value

    private val _skipSilenceFlow = MutableStateFlow(prefs.getBoolean(SKIP_SILENCE, false))
    val skipSilenceFlow = _skipSilenceFlow.asStateFlow()
    val skipSilence get() = skipSilenceFlow.value

    suspend fun setEnableAudioOffload(enable: Boolean) {
        _audioOffloadFlow.emit(enable)
        prefs.edit(commit = true) { putBoolean(AUDIO_OFFLOAD, enable) }
    }

    suspend fun setEnableAudioFloatOutput(enable: Boolean) {
        _audioFloatOutputFlow.emit(enable)
        prefs.edit(commit = true) { putBoolean(AUDIO_FLOAT_OUTPUT, enable) }
    }

    suspend fun setEnableSkipSilence(enable: Boolean) {
        _skipSilenceFlow.emit(enable)
        prefs.edit(commit = true) { putBoolean(SKIP_SILENCE, enable) }
    }

    suspend fun setBalance(balance: BalanceLevel) {
        _balanceFlow.emit(balance)
        prefs.edit(commit = true) {
            putFloat(LEFT_BALANCE, balance.left)
            putFloat(RIGHT_BALANCE, balance.right)
        }
    }

    suspend fun setTempo(tempo: TempoLevel) {
        _tempoFlow.emit(tempo)
        prefs.edit(commit = true) {
            putFloat(SPEED, tempo.speed)
            putFloat(PITCH, tempo.pitch)
            putBoolean(IS_FIXED_PITCH, tempo.isFixedPitch)
        }
    }

    suspend fun setReplayGain(replayGain: ReplayGainState) {
        _replayGainStateFlow.emit(replayGain)
        prefs.edit(commit = true) {
            putFloat(REPLAYGAIN_PREAMP, replayGain.preamp)
            putFloat(REPLAYGAIN_PREAMP_WITHOUT_GAIN, replayGain.preampWithoutGain)
            putString(REPLAYGAIN_MODE, replayGain.mode.name)
        }
    }

    private fun createBalanceState(): BalanceLevel {
        return BalanceLevel(
            left = prefs.getFloat(LEFT_BALANCE, MAX_BALANCE),
            right = prefs.getFloat(RIGHT_BALANCE, MAX_BALANCE)
        )
    }

    private fun createTempoState(): TempoLevel {
        return TempoLevel(
            speed = prefs.getFloat(SPEED, 1f),
            pitch = prefs.getFloat(PITCH, 1f),
            isFixedPitch = prefs.getBoolean(IS_FIXED_PITCH, true)
        )
    }

    private fun createReplayGainState(): ReplayGainState {
        return ReplayGainState(
            mode = prefs.enumValue(REPLAYGAIN_MODE, ReplayGainMode.Off),
            preamp = prefs.getFloat(REPLAYGAIN_PREAMP, 0f),
            preampWithoutGain = prefs.getFloat(REPLAYGAIN_PREAMP_WITHOUT_GAIN, 0f)
        )
    }

    companion object {
        private const val AUDIO_OFFLOAD = "audio.offload"
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