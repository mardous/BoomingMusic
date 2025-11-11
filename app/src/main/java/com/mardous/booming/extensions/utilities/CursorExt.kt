/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.mardous.booming.extensions.utilities

import android.database.Cursor
import android.util.Log

val Cursor?.isNullOrEmpty: Boolean
    get() = this == null || this.count == 0

fun Cursor?.iterateIfValid(consumer: Cursor.() -> Unit) {
    if (this?.moveToFirst() == true) {
        do {
            consumer(this)
        } while (moveToNext())
    }
}

fun <T> Cursor?.mapIfValid(consumer: Cursor.() -> T?): List<T> {
    val list = mutableListOf<T>()
    iterateIfValid {
        try {
            consumer(this)?.let { list.add(it) }
        } catch (e: Exception) {
            Log.e("CursorExt", "Discarding row due to exception: ${e.message}", e)
        }
    }
    return list
}

fun <T> Cursor?.takeOrDefault(default: T, consumer: Cursor.() -> T): T {
    return if (this != null && this.moveToFirst()) consumer(this) else default
}

fun Cursor.getBoolean(columnIndex: Int): Boolean {
    if (columnIndex != -1) {
        return getInt(columnIndex) == 1
    }
    return false
}

fun Cursor.getLongSafe(column: String): Long {
    val index = getColumnIndex(column)
    return if (index != -1) getLong(index) else -1
}

fun Cursor.getStringSafe(column: String): String? {
    val index = getColumnIndex(column)
    return if (index != -1) getString(index) else null
}