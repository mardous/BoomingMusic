package com.mardous.booming.playback

import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaSession
import com.mardous.booming.coil.CoverProvider
import com.mardous.booming.coil.CoverProvider.Companion.getImageUri
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.media.songInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extra parameter indicating when a MediaItem was generated from a Song
 * opened from an external app.
 */
const val RESOLVED_FROM_FILE = "resolved_from_file"

/**
 * A Pair containing a MediaItem object and its index within the current Timeline.
 */
typealias QueueItem = Pair<MediaItem, Int>

@OptIn(UnstableApi::class)
internal fun MediaSession.isRemoteController(controller: MediaSession.ControllerInfo): Boolean {
    return isMediaNotificationController(controller) ||
            isAutoCompanionController(controller) ||
            isAutomotiveController(controller)
}

val Player.mediaItems: List<MediaItem>
    get() = (0 until mediaItemCount).map { getMediaItemAt(it) }

fun Player.getQueueItems(shuffleMode: Boolean = this.shuffleModeEnabled): List<QueueItem> {
    val timeline = currentTimeline
    if (timeline.isEmpty) return emptyList()

    val result = mutableListOf<QueueItem>()
    var index = timeline.getFirstWindowIndex(shuffleMode)
    while (index != C.INDEX_UNSET) {
        result.add(QueueItem(getMediaItemAt(index), index))
        index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffleMode)
    }

    return result
}

val MediaItem.resolvedFromFile: Boolean
    get() = mediaMetadata.extras?.getBoolean(RESOLVED_FROM_FILE) == true

fun MediaItem.withExtras(consumer: Bundle.() -> Unit) = buildUpon()
    .setMediaMetadata(mediaMetadata.withExtras(consumer))
    .build()

fun MediaMetadata.withExtras(consumer: Bundle.() -> Unit) = buildUpon()
    .setExtras(getOrCreateExtras().apply(consumer))
    .build()

fun MediaMetadata.getOrCreateExtras() = extras ?: Bundle()

@OptIn(UnstableApi::class)
fun buildBrowsableMediaItem(
    type: Int,
    id: String,
    title: String,
    subtitle: String? = null,
    artworkUri: Uri? = null,
    showAsGrid: Boolean = false
): MediaItem {
    val gridExtras = if (showAsGrid) {
        Bundle().apply {
            putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
            putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
        }
    } else {
        Bundle.EMPTY
    }
    return MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setMediaType(type)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setArtworkUri(artworkUri)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setExtras(gridExtras)
                .build()
        )
        .build()
}

fun buildPlayableMediaItem(id: String): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .build()
}

fun buildPlayableMediaItem(song: Song, id: String = song.id.toString()): MediaItem {
    return MediaItem.Builder()
        .setUri(song.uri)
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setArtworkUri(getImageUri(CoverProvider.SONG_COVER_PATH, song.id))
                .setTitle(song.title)
                .setSubtitle(song.songInfo())
                .setAlbumTitle(song.albumName)
                .setArtist(song.artistName)
                .setAlbumArtist(song.albumArtistName)
                .setGenre(song.genreName)
                .setTrackNumber(song.trackNumber)
                .setReleaseYear(song.year)
                .setDurationMs(song.duration.coerceAtLeast(0))
                .setExtras(
                    Bundle().apply { putBoolean(RESOLVED_FROM_FILE, song.resolvedFromFile) }
                )
                .build()
        )
        .build()
}