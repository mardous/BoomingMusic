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

typealias OnCommit<T> = suspend (T) -> Unit

open class EqState(
    val isSupported: Boolean,
    val isEnabled: Boolean,
    private var isPending: Boolean = false,
    val onCommit: OnCommit<EqState>
) {
    val isUsable: Boolean get() = isSupported && isEnabled

    suspend fun apply() {
        if (!isPending)
            return

        isPending = false
        onCommit(this)
    }
}

open class EqEffectState<T>(
    isSupported: Boolean,
    isEnabled: Boolean,
    isPending: Boolean = false,
    val value: T,
    val onCommitEffect: OnCommit<EqEffectState<T>>
) : EqState(isSupported, isEnabled, isPending, { onCommitEffect(it as EqEffectState<T>) })

open class EqUpdate<T : EqState>(protected val state: T, val isEnabled: Boolean) {
    open fun toState(): EqState {
        if (state.isEnabled == isEnabled) {
            return state
        }
        return EqState(state.isSupported, isEnabled, isPending = true, state.onCommit)
    }
}

class EqEffectUpdate<V>(state: EqEffectState<V>, isEnabled: Boolean, val value: V) :
    EqUpdate<EqEffectState<V>>(state, isEnabled) {
    override fun toState(): EqEffectState<V> {
        if (state.isEnabled == isEnabled && state.value == value) {
            return state
        }
        return EqEffectState(state.isSupported, isEnabled, isPending = true, value, state.onCommitEffect)
    }
}