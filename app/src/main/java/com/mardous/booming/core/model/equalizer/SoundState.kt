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
    val center: Float,
    val range: ClosedFloatingPointRange<Float>
) {
    val left  = (1f - center).coerceIn(0f, 1f)
    val right = (1f + center).coerceIn(0f, 1f)

    companion object {
        val Unspecified = BalanceState(0f, -1f..1f)
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
    val currentVolume: Float,
    val volumeRange: ClosedFloatingPointRange<Float>,
    val isFixed: Boolean = false
) {
    val volumePercent: Float
        get() = if (volumeRange.endInclusive > volumeRange.start) {
            ((currentVolume - volumeRange.start) / (volumeRange.endInclusive - volumeRange.start)) * 100f
        } else 0f

    companion object {
        val Unspecified = VolumeState(0f, 0f..1f)
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