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

package com.mardous.booming.core.model.equalizer

import androidx.compose.runtime.Immutable
import com.mardous.booming.data.model.replaygain.ReplayGainMode

@Immutable
data class BalanceState(
    val left: Float,
    val right: Float,
    val range: ClosedFloatingPointRange<Float>
) {
    companion object {
        val Unspecified = BalanceState(0f, 0f, 0f..1f)
    }
}

@Immutable
data class TempoState(
    val speed: Float,
    val speedRange: ClosedFloatingPointRange<Float>,
    val pitch: Float,
    val pitchRange: ClosedFloatingPointRange<Float>,
    val isFixedPitch: Boolean
) {
    val actualPitch: Float
        get() = if (isFixedPitch) speed else pitch

    companion object {
        val Unspecified = TempoState(1f, 1f..1f, 1f, 1f..1f, false)
    }
}

@Immutable
data class VolumeState(
    val currentVolume: Int,
    val maxVolume: Int,
    val minVolume: Int,
    val isFixed: Boolean
) {
    val range get() = minVolume.toFloat()..maxVolume.toFloat()
    val volumePercent: Float
        get() = if (maxVolume > minVolume) {
            ((currentVolume - minVolume).toFloat() / (maxVolume - minVolume).toFloat()) * 100f
        } else 0f

    companion object {
        val Unspecified = VolumeState(0, 1, 0, false)
    }
}

@Immutable
data class ReplayGainState(
    val mode: ReplayGainMode,
    val preamp: Float,
    val preampWithoutGain: Float
) {
    val availableModes = ReplayGainMode.entries.toTypedArray()

    companion object {
        val Unspecified = ReplayGainState(ReplayGainMode.Off, 0f, 0f)
    }
}

const val MIN_SPEED = .5f
const val MIN_SPEED_NO_PITCH = .8f
const val MAX_SPEED = 2f
const val MAX_SPEED_NO_PITCH = 1.5f
const val MIN_PITCH = .5f
const val MAX_PITCH = 2f
const val MIN_BALANCE = 0f
const val MAX_BALANCE = 1f