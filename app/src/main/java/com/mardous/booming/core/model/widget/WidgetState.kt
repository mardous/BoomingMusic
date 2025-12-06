package com.mardous.booming.core.model.widget

import kotlinx.serialization.Serializable

@Serializable
class WidgetState(
    val title: String,
    val artist: String,
    val album: String,
    val artwork: String?,
    val isPlaying: Boolean,
    val isFavorite: Boolean
) {
    companion object {
        val empty = WidgetState("", "", "", null, isPlaying = false, isFavorite = false)
    }
}