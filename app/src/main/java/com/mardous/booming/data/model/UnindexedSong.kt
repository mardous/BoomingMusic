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

import android.net.Uri
import androidx.media3.common.MediaItem
import com.mardous.booming.playback.buildPlayableMediaItem
import kotlinx.parcelize.Parcelize

@Parcelize
class UnindexedSong(
    override val uri: Uri,
    override val id: Long,
    override val data: String,
    override val title: String,
    override val size: Long,
    override val duration: Long,
    override val albumName: String,
    override val artistName: String
) : Song(
    id = id,
    data = data,
    title = title,
    trackNumber = -1,
    year = -1,
    size = size,
    duration = duration,
    dateAdded = -1,
    rawDateModified = -1,
    albumId = -1,
    albumName = albumName,
    artistId = -1,
    artistName = artistName,
    albumArtistName = null,
    genreName = null,
    resolvedFromFile = true
) {

    override fun toMediaItem(): MediaItem {
        val mediaItem = buildPlayableMediaItem(song = this, id = data)
        return mediaItem.buildUpon()
            .setMediaMetadata(
                mediaItem.mediaMetadata.buildUpon()
                    .setArtworkUri(uri)
                    .build()
            )
            .build()
    }
}