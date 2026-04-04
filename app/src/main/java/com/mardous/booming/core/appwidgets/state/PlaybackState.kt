package com.mardous.booming.core.appwidgets.state

import androidx.media3.common.Player
import kotlinx.serialization.Serializable

@Serializable
class PlaybackState(
    val isSimplifiedSmallLayout: Boolean = false,
    val isForeground: Boolean = false,
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val isShuffleMode: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val currentTitle: String? = "",
    val currentArtist: String? = "",
    val additionalInfo: String? = "",
    val artworkData: ByteArray? = null,
    val widgetTheme: WidgetTheme? = null,
    val imageCornerRadius: Float? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaybackState

        if (isSimplifiedSmallLayout != other.isSimplifiedSmallLayout) return false
        if (isForeground != other.isForeground) return false
        if (isPlaying != other.isPlaying) return false
        if (isFavorite != other.isFavorite) return false
        if (isShuffleMode != other.isShuffleMode) return false
        if (repeatMode != other.repeatMode) return false
        if (currentTitle != other.currentTitle) return false
        if (currentArtist != other.currentArtist) return false
        if (additionalInfo != other.additionalInfo) return false
        if (widgetTheme != other.widgetTheme) return false
        if (imageCornerRadius != other.imageCornerRadius) return false

        if (artworkData != null) {
            if (other.artworkData == null) return false
            if (!artworkData.contentEquals(other.artworkData)) return false
        } else if (other.artworkData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isSimplifiedSmallLayout.hashCode()
        result = 31 * result + isForeground.hashCode()
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + isShuffleMode.hashCode()
        result = 31 * result + repeatMode.hashCode()
        result = 31 * result + (currentTitle?.hashCode() ?: 0)
        result = 31 * result + (currentArtist?.hashCode() ?: 0)
        result = 31 * result + (additionalInfo?.hashCode() ?: 0)
        result = 31 * result + (widgetTheme?.hashCode() ?: 0)
        result = 31 * result + (imageCornerRadius?.hashCode() ?: 0)
        result = 31 * result + (artworkData?.contentHashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "PlaybackState(title=$currentTitle, artist=$currentArtist, isPlaying=$isPlaying, hasArtwork=${artworkData != null})"
    }

    companion object {
        val empty = PlaybackState()
    }
}

@Serializable
data class WidgetTheme(
    // Light theme colors
    val lightSurfaceColor: Int,
    val lightOnSurfaceColor: Int,
    val lightOnSurfaceVariantColor: Int,
    val lightPrimaryColor: Int,
    val lightOnPrimaryColor: Int,
    val lightPrimaryContainerColor: Int,
    val lightOnPrimaryContainerColor: Int,
    val lightTertiaryContainerColor: Int,
    val lightOnTertiaryContainerColor: Int,

    // Dark theme colors
    val darkSurfaceColor: Int,
    val darkOnSurfaceColor: Int,
    val darkOnSurfaceVariantColor: Int,
    val darkPrimaryColor: Int,
    val darkOnPrimaryColor: Int,
    val darkPrimaryContainerColor: Int,
    val darkOnPrimaryContainerColor: Int,
    val darkTertiaryContainerColor: Int,
    val darkOnTertiaryContainerColor: Int,
)