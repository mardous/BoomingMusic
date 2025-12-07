package com.mardous.booming.core.appwidgets.state

import kotlinx.serialization.Serializable

@Serializable
class PlaybackState(
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val isShuffleMode: Boolean = false,
    val currentTitle: String? = "",
    val currentArtist: String? = "",
    val currentAlbum: String? = "",
    val artworkUri: String? = null
) {
    companion object {
        val empty = PlaybackState()
    }
}