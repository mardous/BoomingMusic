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

package com.mardous.booming.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mardous.booming.extensions.media.albumArtistName
import com.mardous.booming.data.model.Song
import kotlinx.serialization.Serializable

@Serializable
@Entity(indices = [Index("song_title", "song_artist", unique = true)])
class LyricsEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "song_title")
    val title: String,
    @ColumnInfo(name = "song_artist")
    val artist: String,
    @ColumnInfo(name = "synced_lyrics")
    val syncedLyrics: String,
    @ColumnInfo(name = "auto_download")
    val autoDownload: Boolean,
    @ColumnInfo(name = "user_cleared")
    val userCleared: Boolean
)

fun Song.toLyricsEntity(syncedLyrics: String, autoDownload: Boolean = false, userCleared: Boolean = false): LyricsEntity {
    return LyricsEntity(id, title, albumArtistName(), syncedLyrics, autoDownload, userCleared)
}