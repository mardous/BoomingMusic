package com.mardous.booming.core.appwidgets.state

import com.mardous.booming.core.appwidgets.config.SongSource
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTitle: String? = null,
    val currentArtist: String? = null,
    /** id avoids resending a bitmap in every RemoteViews update */
    val songId: Long? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val seedColor: Int? = null,
    val isFavorite: Boolean = false,
    val isShuffleMode: Boolean = false,
    val repeatMode: Int = 0,
    /** Only the sources a placed widget actually shows, so an unused list costs no query. */
    val songIds: Map<SongSource, List<Long>> = emptyMap()
) {

    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    fun songIdsFor(source: SongSource): List<Long> = songIds[source].orEmpty()
}
