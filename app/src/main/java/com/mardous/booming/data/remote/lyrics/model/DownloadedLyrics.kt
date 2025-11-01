/*
 * Copyright (c) 2024-2025 Christians Martínez Alvarado
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

package com.mardous.booming.data.remote.lyrics.model

import com.mardous.booming.data.model.Song
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Christians M. A. (mardous)
 */
@Serializable
data class DownloadedLyrics(
    val id: Int,
    @SerialName("trackName")
    val title: String,
    @SerialName("artistName")
    val artist: String,
    @SerialName("albumName")
    val album: String,
    val duration: Double,
    val plainLyrics: String?,
    val syncedLyrics: String?
) {

    val isSynced: Boolean
        get() = !syncedLyrics.isNullOrEmpty()

    val hasMultiOptions: Boolean
        get() = !plainLyrics.isNullOrEmpty() && !syncedLyrics.isNullOrEmpty()
}

fun Song.toDownloadedLyrics(plainLyrics: String? = null, syncedLyrics: String? = null) =
    DownloadedLyrics(id.toInt(), title, artistName, albumName, (duration / 1000).toDouble(), plainLyrics, syncedLyrics)