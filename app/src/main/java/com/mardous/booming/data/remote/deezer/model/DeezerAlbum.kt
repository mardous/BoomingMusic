/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.data.remote.deezer.model

import com.mardous.booming.extensions.utilities.normalize
import com.mardous.booming.util.ImageSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeezerAlbum(
    @SerialName("data")
    val data: List<AlbumData>
) {
    val imageUrl: String?
        get() = data.firstOrNull()?.let { it.largeImage ?: it.mediumImage ?: it.smallImage ?: it.image }

    fun getBestImage(requestedName: String, requestedImageSize: String): Pair<Boolean, String?> {
        val normRequested = requestedName.normalize()
        val best = data.map { album ->
            val normArtist = album.title.normalize()
            val score = DeezerArtist.JW_SIMILARITY.apply(normArtist, normRequested)
            album to score
        }
            .maxByOrNull { it.second }

        if (best == null || best.second < 0.90) {
            return false to null
        }

        val bestMatch = best.first
        val tentativeImage = when (requestedImageSize) {
            ImageSize.LARGE -> bestMatch.largeImage
            ImageSize.SMALL -> bestMatch.smallImage
            else -> bestMatch.mediumImage
        } ?: bestMatch.image
        return true to tentativeImage
            ?.takeIf { it.isNotBlank() && !it.contains("/images/cover//") }
    }

    @Serializable
    data class AlbumData(
        @SerialName("title")
        val title: String,
        @SerialName("cover")
        val image: String?,
        @SerialName("cover_small")
        val smallImage: String?,
        @SerialName("cover_medium")
        val mediumImage: String?,
        @SerialName("cover_big")
        val largeImage: String?
    )
}