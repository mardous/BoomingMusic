package com.mardous.booming.core.appwidgets.state

import kotlinx.serialization.Serializable

@Serializable
class PlaybackState(
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val currentTitle: String = "",
    val currentArtist: String = "",
    val currentAlbum: String = "",
    val currentProgress: Long = -1,
    val currentDuration: Long = -1,
    val artworkUri: String? = null
)