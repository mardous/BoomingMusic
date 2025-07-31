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

package com.mardous.booming.glide.playlistPreview

import com.mardous.booming.database.PlaylistEntity
import com.mardous.booming.database.PlaylistWithSongs
import com.mardous.booming.database.toSongs
import com.mardous.booming.model.Song

class PlaylistPreview(private val playlistWithSongs: PlaylistWithSongs) {

    val playlistEntity: PlaylistEntity
        get() = playlistWithSongs.playlistEntity

    val songs: List<Song>
        get() = playlistWithSongs.songs.toSongs()

    override fun equals(other: Any?): Boolean {
        println("Glide equals $this $other")
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaylistPreview
        if (other.playlistEntity.playListId != playlistEntity.playListId) return false
        if (other.playlistEntity.customCoverUri != playlistEntity.customCoverUri) return false
        if (other.songs.size != songs.size) return false
        return true
    }

    override fun hashCode(): Int {
        var result = playlistEntity.playListId.hashCode()
        result = 31 * result + (playlistEntity.customCoverUri?.hashCode() ?: 0)
        result = 31 * result + playlistWithSongs.songs.size
        println("Glide $result")
        return result
    }
}