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

package com.mardous.booming.viewmodels.lyrics.model

import androidx.compose.runtime.Immutable
import com.mardous.booming.lyrics.Lyrics
import com.mardous.booming.lyrics.LyricsSource

class DisplayableLyrics<T>(val content: T?, val source: LyricsSource) {
    fun edit(newContent: String?) = EditableLyrics(newContent, source)
}

class EditableLyrics(val content: String?, val source: LyricsSource)

@Immutable
class LyricsResult(
    val id: Long,
    val plainLyrics: DisplayableLyrics<String> = DisplayableLyrics(null, LyricsSource.Embedded),
    val syncedLyrics: DisplayableLyrics<Lyrics> = DisplayableLyrics(null, LyricsSource.Downloaded),
    val loading: Boolean = false,
) {
    val sources = listOf(plainLyrics.source, syncedLyrics.source)
    val hasPlainLyrics: Boolean get() = !plainLyrics.content.isNullOrEmpty()
    val hasSyncedLyrics: Boolean get() = syncedLyrics.content?.hasContent == true
    val isEmpty: Boolean get() = !hasPlainLyrics && !hasSyncedLyrics

//    init {
//        check(sources.distinctBy { it.applicableButtonId }.size == 2) {
//            "Applicable IDs must be unique"
//        }
//    }

    companion object {
        val Empty = LyricsResult(-1)
    }
}