package com.mardous.booming.data.remote.deezer.model

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

    @Serializable
    data class TrackData(
        @SerialName("album")
        val album: Album
    ) {
        @Serializable
        data class Album(
            @SerialName("cover_small")
            val smallImage: String?,
            @SerialName("cover_medium")
            val mediumImage: String?,
            @SerialName("cover_big")
            val largeImage: String?
        )
    }
}