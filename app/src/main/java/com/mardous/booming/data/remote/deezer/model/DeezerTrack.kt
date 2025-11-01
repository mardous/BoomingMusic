package com.mardous.booming.data.remote.deezer.model

import com.mardous.booming.util.ImageSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeezerTrack(
    @SerialName("data")
    val data: List<TrackData>
) {
    val imageUrl: String?
        get() = data.firstOrNull()?.let {
            it.album.largeImage ?: it.album.mediumImage ?: it.album.smallImage
        }

    fun getBestImage(requestedImageSize: String): String? {
        val track = data.firstOrNull() ?: return null
        val image = when (requestedImageSize) {
            ImageSize.LARGE -> track.album.largeImage
            ImageSize.SMALL -> track.album.smallImage
            else -> track.album.mediumImage
        } ?: track.album.image
        return image?.takeIf { it.isNotBlank() && !it.contains("/images/artist//") }
    }

    @Serializable
    data class TrackData(
        @SerialName("album")
        val album: Album
    ) {
        @Serializable
        data class Album(
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
}