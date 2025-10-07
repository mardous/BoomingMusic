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

@Immutable
data class TempoLevel(val speed: Float, val pitch: Float, val isFixedPitch: Boolean) {

    val maxSpeed = if (isFixedPitch) MAX_SPEED else MAX_SPEED_NO_PITCH
    val minSpeed = if (isFixedPitch) MIN_SPEED else MIN_SPEED_NO_PITCH
    val speedRange = minSpeed..maxSpeed

    val maxPitch = MAX_PITCH
    val minPitch = MIN_PITCH
    val pitchRange = minPitch..maxPitch

    val actualPitch: Float
        get() = if (isFixedPitch) speed else pitch
}

@Immutable
data class VolumeState(
    val currentVolume: Int,
    val maxVolume: Int,
    val minVolume: Int,
    val isFixed: Boolean
) {
    val range get() = minVolume.toFloat()..maxVolume.toFloat()

    companion object {
        val Unspecified = VolumeState(0, 1, 0, false)
    }
}

@Immutable
data class BalanceLevel(val left: Float, val right: Float) {
    val range get() = MIN_BALANCE..MAX_BALANCE
}

const val MIN_SPEED = .5f
const val MIN_SPEED_NO_PITCH = .8f
const val MAX_SPEED = 2f
const val MAX_SPEED_NO_PITCH = 1.5f
const val MIN_PITCH = .5f
const val MAX_PITCH = 2f
const val MIN_BALANCE = 0f
const val MAX_BALANCE = 1f