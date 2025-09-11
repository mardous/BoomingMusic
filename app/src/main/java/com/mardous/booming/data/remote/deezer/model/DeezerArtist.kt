package com.mardous.booming.data.remote.deezer.model

import com.mardous.booming.util.ImageSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DeezerArtist(
    @SerialName("data")
    val result: List<Result>,
    val total: Int
) {

    fun getImageUrl(requestedImageSize: String): String? {
        return result.firstOrNull()?.let {
            when (requestedImageSize) {
                ImageSize.LARGE -> it.largeImage ?: it.mediumImage
                ImageSize.SMALL -> it.smallImage ?: it.mediumImage
                else -> it.mediumImage
            }
        }?.takeIf { it.isNotBlank() && !it.contains("/images/artist//") }
    }

    @Serializable
    class Result(
        @SerialName("picture_small")
        val smallImage: String?,
        @SerialName("picture_medium")
        val mediumImage: String?,
        @SerialName("picture_big")
        val largeImage: String?
    )
}