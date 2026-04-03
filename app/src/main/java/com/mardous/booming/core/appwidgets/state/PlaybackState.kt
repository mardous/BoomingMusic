package com.mardous.booming.core.appwidgets.state

import kotlinx.serialization.Serializable

@Serializable
class PlaybackState(
    val isSimplifiedSmallLayout: Boolean = false,
    val isForeground: Boolean = false,
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val isShuffleMode: Boolean = false,
    val currentTitle: String? = "",
    val currentArtist: String? = "",
    val additionalInfo: String? = "",
    val artworkData: ByteArray? = null,
    val widgetTheme: WidgetTheme? = null,
    val imageCornerRadius: Float? = null
) {
    companion object {
        val empty = PlaybackState()
    }
}

@Serializable
class WidgetTheme(
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