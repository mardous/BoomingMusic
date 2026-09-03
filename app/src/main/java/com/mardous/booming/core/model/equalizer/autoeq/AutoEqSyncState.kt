/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.mardous.booming.core.model.equalizer.autoeq

private const val INDETERMINATE_PROGRESS = -1

sealed interface AutoEqSyncState {
    data object Idle : AutoEqSyncState
    data class Syncing(
        val progress: Int = INDETERMINATE_PROGRESS,
        val count: Int = INDETERMINATE_PROGRESS
    ) : AutoEqSyncState {
        val isIndeterminate: Boolean =
            progress == INDETERMINATE_PROGRESS || count == INDETERMINATE_PROGRESS

        val fraction: Float =
            if (count > 0) (progress.toFloat() / count.toFloat()).coerceIn(0f, 1f) else 0f
    }
    data class Success(val count: Int) : AutoEqSyncState
    data class Error(val message: String?) : AutoEqSyncState
}