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
import com.mardous.booming.service.equalizer.EqualizerManager.Companion.PREFERENCES_NAME
import com.mardous.booming.util.PLAYBACK_PITCH
import com.mardous.booming.util.PLAYBACK_SPEED
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SoundSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _balanceFlow = MutableStateFlow(createBalanceState())
    val balanceFlow: StateFlow<EqEffectState<BalanceLevel>> get() = _balanceFlow
    val balance: BalanceLevel get() = _balanceFlow.value.value

    private val _tempoFlow = MutableStateFlow(createTempoState())
    val tempoFlow: StateFlow<EqEffectState<TempoLevel>> get() = _tempoFlow
    val tempo: TempoLevel get() = _tempoFlow.value.value

    private val _crossfadeFlow = MutableStateFlow(createCrossfadeState())
    val crossfadeFlow: StateFlow<EqEffectState<CrossfadeState>> get() = _crossfadeFlow
    val crossfade: CrossfadeState get() = _crossfadeFlow.value.value

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

    suspend fun setCrossfade(update: EqEffectUpdate<CrossfadeState>, apply: Boolean) {
        val newState = update.toState().also {
            if (apply) it.apply()
        }
        _crossfadeFlow.emit(newState)
    }

    suspend fun applyPendingState() {
        balanceFlow.value.apply()
        tempoFlow.value.apply()
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

    private fun createCrossfadeState(): EqEffectState<CrossfadeState> {
        val tempo = CrossfadeState(
            apply = true,
            crossfadeDuration = prefs.getInt(CROSSFADE_DURATION, 0),
            audioFadeDuration = prefs.getInt(AUDIO_FADE_DURATION, 0)
        )
        return EqEffectState(
            isSupported = true,
            isEnabled = true,
            value = tempo,
            onCommitEffect = {
                prefs.edit {
                    putInt(CROSSFADE_DURATION, it.value.crossfadeDuration)
                    putInt(AUDIO_FADE_DURATION, it.value.audioFadeDuration)
                }
            }
        )
    }

    companion object {
        private const val LEFT_BALANCE = "equalizer.balance.left"
        private const val RIGHT_BALANCE = "equalizer.balance.right"
        private const val SPEED = PLAYBACK_SPEED
        private const val PITCH = PLAYBACK_PITCH
        private const val IS_FIXED_PITCH = "equalizer.pitch.fixed"
        private const val CROSSFADE_DURATION = "equalizer.crossfade.duration"
        private const val AUDIO_FADE_DURATION = "equalizer.fade.duration"
    }
}