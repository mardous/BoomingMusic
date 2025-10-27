package com.mardous.booming.data.remote.deezer.model

import com.mardous.booming.extensions.utilities.normalize
import com.mardous.booming.util.ImageSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.commons.text.similarity.JaroWinklerSimilarity

@Serializable
class DeezerArtist(
    @SerialName("data")
    val result: List<Result>,
    val total: Int
) {

    fun getBestImage(requestedName: String, requestedImageSize: String): Pair<Boolean, String?> {
        val normRequested = requestedName.normalize()
        val best = result.map { artist ->
                val normArtist = artist.artistName.normalize()
                val score = JW_SIMILARITY.apply(normArtist, normRequested)
                artist to score
            }
            .maxByOrNull { it.second }

        if (best == null || best.second < 0.90) {
            return false to null
        }

        val artist = best.first
        val tentativeImage = when (requestedImageSize) {
            ImageSize.LARGE -> artist.largeImage
            ImageSize.SMALL -> artist.smallImage
            else -> artist.mediumImage
        } ?: artist.image
        return true to tentativeImage
            ?.takeIf { it.isNotBlank() && !it.contains("/images/artist//") }
    }

    @Serializable
    class Result(
        @SerialName("name")
        val artistName: String,
        @SerialName("picture")
        val image: String?,
        @SerialName("picture_small")
        val smallImage: String?,
        @SerialName("picture_medium")
        val mediumImage: String?,
        @SerialName("picture_big")
        val largeImage: String?
    )

    companion object {
        val JW_SIMILARITY = JaroWinklerSimilarity()
    }
}