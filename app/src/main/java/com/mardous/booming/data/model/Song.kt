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

package com.mardous.booming.data.model

import android.content.ContentUris
import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mardous.booming.core.model.filesystem.FileSystemItem
import com.mardous.booming.data.local.repository.RealSongRepository.Companion.getAudioContentUri
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.Objects

@Parcelize
open class Song(
    open val id: Long,
    open val data: String,
    open val title: String,
    open val trackNumber: Int,
    open val year: Int,
    open val size: Long,
    open val duration: Long,
    open val dateAdded: Long,
    open val rawDateModified: Long,
    open val albumId: Long,
    open val albumName: String,
    open val artistId: Long,
    open val artistName: String,
    open val albumArtistName: String?,
    open val genreName: String?
) : Parcelable, FileSystemItem {

    val uri: Uri
        get() = ContentUris.withAppendedId(getAudioContentUri(), id)

    val albumCoverUri: Uri
        get() = ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), albumId)

    @IgnoredOnParcel
    val dateModified by lazy { Date(rawDateModified * 1000) }

    override val fileName: String
        get() = data.substringAfterLast("/", title)

    override val filePath: String
        get() = data

    override val fileDateAdded: Long
        get() = dateAdded

    override val fileDateModified: Long
        get() = rawDateModified

    protected constructor(song: Song) : this(
        song.id,
        song.data,
        song.title,
        song.trackNumber,
        song.year,
        song.size,
        song.duration,
        song.dateAdded,
        song.rawDateModified,
        song.albumId,
        song.albumName,
        song.artistId,
        song.artistName,
        song.albumArtistName,
        song.genreName
    )

    fun toMediaItem(itemId: String = id.toString()) = if (this == emptySong) {
        MediaItem.EMPTY
    } else {
        MediaItem.Builder()
            .setUri(uri)
            .setMediaId(itemId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsPlayable(true)
                    .setArtworkUri(uri) // IMPORTANT must use Song's uri
                    .setTitle(title)
                    .setAlbumTitle(albumName)
                    .setArtist(artistName)
                    .setAlbumArtist(albumArtistName)
                    .setGenre(genreName)
                    .setTrackNumber(trackNumber)
                    .setReleaseYear(year)
                    .setDurationMs(duration.coerceAtLeast(0))
                    .build()
            )
            .build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val song = other as Song
        if (id != song.id) return false
        if (trackNumber != song.trackNumber) return false
        if (year != song.year) return false
        if (size != song.size) return false
        if (duration != song.duration) return false
        if (dateAdded != song.dateAdded) return false
        if (rawDateModified != song.rawDateModified) return false
        if (albumId != song.albumId) return false
        if (albumName != song.albumName) return false
        if (artistId != song.artistId) return false
        if (artistName != song.artistName) return false
        if (data != song.data) return false
        if (title != song.title) return false
        if (albumArtistName != song.albumArtistName) return false
        return genreName == song.genreName
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id,
            data,
            title,
            trackNumber,
            year,
            size,
            duration,
            dateAdded,
            rawDateModified,
            albumId,
            albumName,
            artistId,
            artistName,
            albumArtistName,
            genreName
        )
    }

    override fun toString(): String {
        return "Song{" +
                "id=" + id +
                ", data='" + data + '\'' +
                ", title='" + title + '\'' +
                ", asReadableTrackNumber=" + trackNumber +
                ", year=" + year +
                ", size=" + size +
                ", duration=" + duration +
                ", dateAdded=" + dateAdded +
                ", rawDateModified=" + rawDateModified +
                ", albumId=" + albumId +
                ", albumName='" + albumName + '\'' +
                ", artistId=" + artistId +
                ", artistName='" + artistName + '\'' +
                ", albumArtistName='" + albumArtistName + '\'' +
                ", genreName='" + genreName + '\'' +
                '}'
    }

    companion object {
        val emptySong = Song(-1, "", "", -1, -1, -1, -1, -1, -1, -1, "", -1, "", "", "")
    }
}