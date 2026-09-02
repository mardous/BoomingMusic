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

package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import com.mardous.booming.R

@Composable
fun <T> rememberMultiSelectState() = remember { MultiSelectState<T>() }

@Stable
inline fun <T> Modifier.multiSelectClickable(
    item: T,
    multiSelectState: MultiSelectState<T>,
    haptics: HapticFeedback,
    crossinline onClick: () -> Unit,
): Modifier {
    return combinedClickable(
        onClick = {
            if (multiSelectState.hasSelectedItems) {
                multiSelectState.toggleItem(item)
            } else {
                onClick()
            }
        },
        onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (multiSelectState.hasSelectedItems) {
                multiSelectState.uncheck(item)
            } else {
                multiSelectState.check(item)
            }
        },
    )
}

@Stable
class MultiSelectState<T> {

    private val selectedItems = mutableStateListOf<T>()
    val items: List<T> get() = selectedItems

    val hasSelectedItems: Boolean get() = selectedItems.isNotEmpty()
    val selectedItemCount: Int get() = selectedItems.size

    val selectionTitle: String
        @Composable get() = stringResource(R.string.x_selected, selectedItemCount)

    fun isSelected(item: T) = selectedItems.contains(item)

    fun checkAll(items: List<T>) {
        selectedItems.clear()
        selectedItems.addAll(items)
    }

    fun check(item: T) {
        if (!selectedItems.contains(item)) {
            selectedItems.add(item)
        }
    }

    fun uncheck(item: T) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        }
    }

    fun toggleItem(item: T): Boolean {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
            return false
        }
        return selectedItems.add(item)
    }

    fun finish() {
        selectedItems.clear()
    }
}