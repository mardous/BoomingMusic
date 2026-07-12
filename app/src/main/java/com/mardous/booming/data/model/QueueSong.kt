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

package com.mardous.booming.data.model

import kotlinx.parcelize.Parcelize

@Parcelize
class QueueSong(
    val key: Pair<Long, Int>,
    override val id: Long,
    override val data: String,
    override val title: String,
    override val trackNumber: Int,
    override val year: Int,
    override val size: Long,
    override val duration: Long,
    override val dateAdded: Long,
    override val rawDateModified: Long,
    override val albumId: Long,
    override val albumName: String,
    override val artistId: Long,
    override val artistName: String,
    override val albumArtistName: String?,
    override val genreName: String?,
    override val volumeName: String? = null
) : Song(
    id = id,
    data = data,
    title = title,
    trackNumber = trackNumber,
    year = year,
    size = size,
    duration = duration,
    dateAdded = dateAdded,
    rawDateModified = rawDateModified,
    albumId = albumId,
    albumName = albumName,
    artistId = artistId,
    artistName = artistName,
    albumArtistName = albumArtistName,
    genreName = genreName,
    volumeName = volumeName
) {
    constructor(key: Pair<Long, Int>, song: Song) : this(
        key = key,
        id = song.id,
        data = song.data,
        title = song.title,
        trackNumber = song.trackNumber,
        year = song.year,
        size = song.size,
        duration = song.duration,
        dateAdded = song.dateAdded,
        rawDateModified = song.rawDateModified,
        albumId = song.albumId,
        albumName = song.albumName,
        artistId = song.artistId,
        artistName = song.artistName,
        albumArtistName = song.albumArtistName,
        genreName = song.genreName,
        volumeName = song.volumeName
    )
}