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

package com.mardous.booming.data.mapper

import com.mardous.booming.data.local.room.HistoryEntity
import com.mardous.booming.data.local.room.PlayCountEntity
import com.mardous.booming.data.local.room.PlaylistEntity
import com.mardous.booming.data.local.room.SongEntity
import com.mardous.booming.data.model.Song

fun List<HistoryEntity>.fromHistoryToSongs(): List<Song> {
    return map {
        it.toSong()
    }
}

fun List<SongEntity>.toSongs(): List<Song> {
    return map {
        it.toSong()
    }
}

fun Song.toHistoryEntity(timePlayed: Long): HistoryEntity {
    return HistoryEntity(
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        dateModified = rawDateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtistName,
        genreName = genreName,
        timePlayed = timePlayed
    )
}

fun Song.toSongEntity(playListId: Long): SongEntity {
    return SongEntity(
        playlistCreatorId = playListId,
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        dateModified = rawDateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtist = albumArtistName,
        genreName = genreName
    )
}

fun SongEntity.toSong(): Song {
    return Song(
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        rawDateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtist,
        genreName = genreName
    )
}

fun PlayCountEntity.toSong(): Song {
    return Song(
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        rawDateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtistName,
        genreName = genreName
    )
}

fun HistoryEntity.toSong(): Song {
    return Song(
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        rawDateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtistName,
        genreName = genreName
    )
}

fun Song.toPlayCount(timePlayed: Long = -1, playCount: Int = 0, skipCount: Int = 0): PlayCountEntity {
    return PlayCountEntity(
        id = id,
        data = data,
        title = title,
        trackNumber = trackNumber,
        year = year,
        size = size,
        duration = duration,
        dateAdded = dateAdded,
        dateModified = rawDateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        albumArtistName = albumArtistName,
        genreName = genreName,
        timePlayed = timePlayed,
        playCount = playCount,
        skipCount = skipCount
    )
}

fun List<Song>.toSongsEntity(playlistEntity: PlaylistEntity): List<SongEntity> {
    return map {
        it.toSongEntity(playlistEntity.playListId)
    }
}

fun List<Song>.toSongsEntity(playlistId: Long): List<SongEntity> {
    return map {
        it.toSongEntity(playlistId)
    }
}