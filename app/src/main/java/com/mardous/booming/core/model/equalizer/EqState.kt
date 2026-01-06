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
    val isDisabledByAudioOffload: Boolean,
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
    val valueMin: T,
    val valueMax: T,
    val onCommitEffect: OnCommit<EqEffectState<T>>
) : EqState(
    isSupported = isSupported,
    isEnabled = isEnabled,
    isDisabledByAudioOffload = false, // audio offload disables the overall EQ state
    isPending = isPending,
    onCommit = { onCommitEffect(it as EqEffectState<T>) }
)

open class EqUpdate<T : EqState>(
    protected val state: T,
    val isEnabled: Boolean,
    val isSupported: Boolean = state.isSupported,
    val isDisabledByAudioOffload: Boolean = state.isDisabledByAudioOffload,
    val isTransient: Boolean = false
) {
    open fun toState(): EqState {
        if (state.isSupported == isSupported && state.isEnabled == isEnabled) {
            return state
        }
        return EqState(
            isSupported = isSupported,
            isEnabled = isEnabled,
            isDisabledByAudioOffload = isDisabledByAudioOffload,
            isPending = !isTransient,
            onCommit = state.onCommit
        )
    }
}

class EqEffectUpdate<V>(
    state: EqEffectState<V>,
    isEnabled: Boolean,
    val value: V,
    isSupported: Boolean = state.isSupported,
) : EqUpdate<EqEffectState<V>>(state, isEnabled, isSupported) {
    override fun toState(): EqEffectState<V> {
        if (state.isSupported == isSupported && state.isEnabled == isEnabled && state.value == value) {
            return state
        }
        return EqEffectState(
            isSupported = isSupported,
            isEnabled = isEnabled,
            isPending = true,
            value = value,
            valueMin = state.valueMin,
            valueMax = state.valueMax,
            onCommitEffect = state.onCommitEffect
        )
    }
}