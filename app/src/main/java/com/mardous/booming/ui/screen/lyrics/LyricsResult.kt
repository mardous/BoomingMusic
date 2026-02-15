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

package com.mardous.booming.ui.screen.lyrics

import androidx.compose.runtime.Immutable
import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.data.model.lyrics.LyricsSource

data class DisplayableLyrics<T>(val content: T?, val source: LyricsSource) {
    val isEditable = source.isEditable

    fun edit(newContent: String?): EditableLyrics? {
        if (isEditable) {
            val originalContent = if (content is Lyrics) {
                content.rawText
            } else {
                content?.toString().orEmpty()
            }
            return EditableLyrics(originalContent, newContent, source)
        }
        return null
    }
}

data class EditableLyrics(
    private val originalContent: String,
    val content: String?,
    val source: LyricsSource
) {
    val hasChanged: Boolean
        get() = originalContent != content
}

@Immutable
data class LyricsResult(
    val id: Long,
    val plainLyrics: DisplayableLyrics<String> = DisplayableLyrics(null, LyricsSource.Embedded),
    val syncedLyrics: DisplayableLyrics<Lyrics> = DisplayableLyrics(null, LyricsSource.Downloaded),
    val instrumental: Boolean = false,
    val loading: Boolean = false,
) {
    val sources = listOf(plainLyrics.source, syncedLyrics.source)
    val hasPlainLyrics: Boolean get() = !plainLyrics.content.isNullOrEmpty()
    val hasSyncedLyrics: Boolean get() = syncedLyrics.content?.hasContent == true
    val isEmpty: Boolean get() = !hasPlainLyrics && !hasSyncedLyrics

    fun setSources(
        plainSource: LyricsSource = this.plainLyrics.source,
        syncedSource: LyricsSource = this.syncedLyrics.source
    ): LyricsResult {
        if (plainSource == plainLyrics.source && syncedSource == syncedLyrics.source)
            return this

        return copy(
            plainLyrics = plainLyrics.copy(source = plainSource),
            syncedLyrics = syncedLyrics.copy(source = syncedSource)
        )
    }

    companion object {
        val Empty = LyricsResult(-1)
    }
}

data class SaveLyricsResult(
    val plainLyricsState: State = State.None,
    val syncedLyricsState: State = State.None
) {

    val hasChanged: Boolean = plainLyricsState != State.None || syncedLyricsState != State.None

    val isSuccess: Boolean = when {
        plainLyricsState == State.Failed || syncedLyricsState == State.Failed -> false
        plainLyricsState == State.Wrote || syncedLyricsState == State.Wrote -> true
        else -> false
    }

    enum class State {
        Wrote, Failed, None
    }
}