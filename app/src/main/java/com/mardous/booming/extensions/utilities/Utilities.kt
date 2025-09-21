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

import android.util.Log
import kotlinx.serialization.json.Json
import java.text.Normalizer

private val SPACES_REGEX = Regex("\\s+")
const val DEFAULT_INFO_DELIMITER = " • "

fun String.collapseSpaces() = trim().replace(SPACES_REGEX, " ")

fun String.normalize(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{M}".toRegex(), "")
        .trim()
        .replace(Regex("\\s+"), " ")

fun buildInfoString(vararg parts: String?, delimiter: String = DEFAULT_INFO_DELIMITER): String {
    val sb = StringBuilder()
    if (parts.isNotEmpty()) {
        for (part in parts) {
            if (part.isNullOrEmpty()) {
                continue
            }
            if (sb.isNotEmpty()) {
                sb.append(delimiter)
            }
            sb.append(part)
        }
    }
    return sb.toString()
}

inline fun <reified T : Enum<T>> String.toEnum() =
    enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) }

inline fun <reified T> String?.deserialize(defaultValue: T): T {
    val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    return if (!isNullOrEmpty())
        runCatching<T> { lenientJson.decodeFromString(this) }
            .onFailure { Log.d("BoomingUtilities", "Json.decodeFromString($this): error", it) }
            .getOrDefault(defaultValue)
    else defaultValue
}

inline fun <reified T> T.serialize(): String = Json.encodeToString(this)