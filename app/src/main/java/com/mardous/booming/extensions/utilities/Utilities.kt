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

import android.os.Build
import android.util.Log
import kotlinx.serialization.json.Json
import java.text.Normalizer

private val SPACES_REGEX = Regex("\\s+")
const val DEFAULT_INFO_DELIMITER = " • "

private val arabicBlocks: Set<Character.UnicodeBlock> by lazy {
    mutableSetOf(
        Character.UnicodeBlock.ARABIC,
        Character.UnicodeBlock.ARABIC_SUPPLEMENT,
        Character.UnicodeBlock.ARABIC_EXTENDED_A,
        Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A,
        Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            add(Character.UnicodeBlock.ARABIC_EXTENDED_B)
        }
    }
}

fun String?.isWhitespace() = this != null && this.length == 1 && this[0] == ' '

fun String.collapseSpaces() = trim().replace(SPACES_REGEX, " ")

fun String.normalize(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{M}".toRegex(), "")
        .trim()
        .replace(Regex("\\s+"), " ")

fun CharSequence.sanitize(): String {
    return toString().replace("/", "_")
        .replace(":", "_")
        .replace("*", "_")
        .replace("?", "_")
        .replace("\"", "_")
        .replace("<", "_")
        .replace(">", "_")
        .replace("|", "_")
        .replace("\\", "_")
        .replace("&", "_")
}

fun Char.isArabic(): Boolean {
    val unicodeBlock = Character.UnicodeBlock.of(this) ?: return false
    return unicodeBlock in arabicBlocks
}

fun String.isRtl(): Boolean {
    return any { it.isArabic() }
}

fun buildInfoString(vararg parts: Any?, delimiter: String = DEFAULT_INFO_DELIMITER): String {
    val sb = StringBuilder()
    if (parts.isNotEmpty()) {
        for (part in parts) {
            val str = part?.toString()
            if (str.isNullOrEmpty()) {
                continue
            }
            if (sb.isNotEmpty()) {
                sb.append(delimiter)
            }
            sb.append(str)
        }
    }
    return sb.toString()
}

inline fun <reified T : Enum<T>> String.toEnum() =
    enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) }

inline fun <reified T : Enum<T>> Int.toEnum() =
    enumValues<T>().firstOrNull { it.ordinal == this }

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