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

package com.mardous.booming.extensions

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState

@Composable
fun <T> SharedPreferences.observeKeyAsState(
    key: String,
    defaultValue: T
): State<T> {
    return produceState(initialValue = defaultValue, key1 = key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                @Suppress("UNCHECKED_CAST")
                value = when (defaultValue) {
                    is Boolean -> getBoolean(key, defaultValue) as T
                    is String -> getString(key, defaultValue) as T
                    is Int -> getInt(key, defaultValue) as T
                    is Float -> getFloat(key, defaultValue) as T
                    is Long -> getLong(key, defaultValue) as T
                    else -> defaultValue
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        value = when (defaultValue) {
            is Boolean -> getBoolean(key, defaultValue) as T
            is String -> getString(key, defaultValue) as T
            is Int -> getInt(key, defaultValue) as T
            is Float -> getFloat(key, defaultValue) as T
            is Long -> getLong(key, defaultValue) as T
            else -> defaultValue
        }

        registerOnSharedPreferenceChangeListener(listener)

        awaitDispose {
            unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}