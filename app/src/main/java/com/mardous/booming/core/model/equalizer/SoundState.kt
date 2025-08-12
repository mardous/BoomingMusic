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

data class TempoLevel(val speed: Float, val pitch: Float, val isFixedPitch: Boolean) {
    val actualPitch: Float
        get() = if (isFixedPitch) speed else pitch
}

data class VolumeState(
    val currentVolume: Int,
    val maxVolume: Int,
    val minVolume: Int,
    val isFixed: Boolean
) {
    companion object {
        val Unspecified = VolumeState(0, 1, 0, false)
    }
}

data class BalanceLevel(val left: Float, val right: Float)