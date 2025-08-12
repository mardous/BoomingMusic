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

/**
 * Wrapper for a list of [EQPreset]s that includes a timestamp to ensure distinct identity.
 *
 * This class is used to force emission of updates in a [StateFlow] even when the list content
 * hasn't changed. By updating the [lastUpdate] field (defaulting to the current time), a new
 * instance is created, ensuring that collectors are notified of a new value.
 *
 * @property list The actual list of equalizer presets.
 * @property lastUpdate A timestamp indicating when this wrapper was created or last updated.
 */
class EqPresetList(val list: List<EQPreset>, val lastUpdate: Long = System.currentTimeMillis())