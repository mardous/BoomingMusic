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

package com.mardous.booming.ui.screen.library.home

import com.mardous.booming.data.model.Suggestion

data class SuggestedResult(
    val state: State = State.Idle,
    val data: List<Suggestion> = arrayListOf()
) {
    val isLoading: Boolean
        get() = state == State.Loading

    enum class State {
        Ready,
        Loading,
        Idle
    }

    companion object {
        val Idle = SuggestedResult(State.Idle)
    }
}

