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
import com.mardous.booming.core.model.equalizer.EqEffectState
import com.mardous.booming.core.model.equalizer.EqEffectUpdate
import com.mardous.booming.core.model.equalizer.TempoLevel
import com.mardous.booming.service.equalizer.EqualizerManager.Companion.PREFERENCES_NAME
import com.mardous.booming.service.equalizer.OpenSLESConstants.*
import com.mardous.booming.util.PLAYBACK_PITCH
import com.mardous.booming.util.PLAYBACK_SPEED
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SoundSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _balanceFlow = MutableStateFlow(createBalanceState())
    private val _tempoFlow = MutableStateFlow(createTempoState())

    val balanceFlow: StateFlow<EqEffectState<BalanceLevel>> get() = _balanceFlow
    val tempoFlow: StateFlow<EqEffectState<TempoLevel>> get() = _tempoFlow

    val balance: BalanceLevel get() = _balanceFlow.value.value
    val minBalance get() = MIN_BALANCE
    val maxBalance get() = MAX_BALANCE

    val tempo: TempoLevel get() = _tempoFlow.value.value
    val maxSpeed get() = if (tempo.isFixedPitch) MAXIMUM_SPEED else MAXIMUM_SPEED_NO_PITCH
    val minSpeed get() = if (tempo.isFixedPitch) MINIMUM_SPEED else MINIMUM_SPEED_NO_PITCH
    val maxPitch get() = MAXIMUM_PITCH
    val minPitch get() = MINIMUM_PITCH
    val defaultSpeed get() = DEFAULT_SPEED
    val defaultPitch get() = DEFAULT_PITCH

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

    suspend fun applyPendingState() {
        balanceFlow.value.apply()
        tempoFlow.value.apply()
    }

    private fun createBalanceState(): EqEffectState<BalanceLevel> {
        val balance = BalanceLevel(
            left = prefs.getFloat(LEFT_BALANCE, maxBalance),
            right = prefs.getFloat(RIGHT_BALANCE, maxBalance)
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
            speed = prefs.getFloat(SPEED, defaultSpeed),
            pitch = prefs.getFloat(PITCH, defaultPitch),
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

    companion object {
        private const val LEFT_BALANCE = "equalizer.balance.left"
        private const val RIGHT_BALANCE = "equalizer.balance.right"
        private const val SPEED = PLAYBACK_SPEED
        private const val PITCH = PLAYBACK_PITCH
        private const val IS_FIXED_PITCH = "equalizer.pitch.fixed"
    }
}