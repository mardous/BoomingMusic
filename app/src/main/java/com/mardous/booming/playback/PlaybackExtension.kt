package com.mardous.booming.playback

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaSession
import com.mardous.booming.coil.CoverProvider
import com.mardous.booming.coil.CoverProvider.Companion.getImageUri
import com.mardous.booming.core.model.queue.QueueSnapshot
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.media.songInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

private const val TAG = "PlaybackExtension"

/**
 * Extra parameter indicating when a MediaItem was generated from a Song
 * opened from an external app.
 */
const val RESOLVED_FROM_FILE = "resolved_from_file"

@OptIn(UnstableApi::class)
internal fun MediaSession.isRemoteController(controller: MediaSession.ControllerInfo): Boolean {
    return isMediaNotificationController(controller) ||
            isAutoCompanionController(controller) ||
            isAutomotiveController(controller)
}

/** Whether a controller may browse the library and issue commands that change stored data */
@OptIn(UnstableApi::class)
internal fun MediaSession.isTrustedController(controller: MediaSession.ControllerInfo): Boolean {
    return controller.isTrusted || isRemoteController(controller)
}

/** The order the repeat button cycles through */
internal fun nextRepeatMode(current: Int): Int = when (current) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

val Player.mediaItems: List<MediaItem>
    get() = (0 until mediaItemCount).map { getMediaItemAt(it) }

suspend fun Player.captureQueueSnapshot(
    timeline: Timeline = this.currentTimeline,
    currentMediaItemIndex: Int = this.currentMediaItemIndex,
    shuffleMode: Boolean = this.shuffleModeEnabled
): QueueSnapshot = withContext(Dispatchers.Default) {
    if (timeline.isEmpty) {
        QueueSnapshot.Empty
    } else {
        val windowCount = timeline.windowCount

        var queuePosition = -1
        val mediaItems = ArrayList<MediaItem>(windowCount)
        val indicesInTimeline = IntArray(windowCount)

        var pos = 0
        var windowIndex = timeline.getFirstWindowIndex(shuffleMode)
        val window = Timeline.Window()

        while (windowIndex != C.INDEX_UNSET && pos < windowCount) {
            if (currentMediaItemIndex == windowIndex) queuePosition = pos

            mediaItems.add(timeline.getWindow(windowIndex, window).mediaItem)
            indicesInTimeline[pos] = windowIndex

            pos++
            windowIndex = timeline.getNextWindowIndex(windowIndex, Player.REPEAT_MODE_OFF, shuffleMode)
        }

        QueueSnapshot(mediaItems, indicesInTimeline, queuePosition)
    }
}

fun Player.removeMediaItemsById(ids: Set<String>) {
    for (index in mediaItemCount - 1 downTo 0) {
        if (getMediaItemAt(index).mediaId in ids) removeMediaItem(index)
    }
}

/** The player validates against an internal count we cannot read: a dropped order is harmless, a crash is not. */
@OptIn(UnstableApi::class)
internal fun ExoPlayer.applyShuffleOrder(order: ShuffleOrder) {
    try {
        shuffleOrder = order
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Rejected shuffle order: length=${order.length}, items=$mediaItemCount", e)
    }
}

/** Reshuffles the current queue, keeping the current item first. */
@OptIn(UnstableApi::class)
internal fun ExoPlayer.applyRandomShuffleOrder() {
    val itemCount = mediaItemCount
    applyShuffleOrder(
        ImprovedShuffleOrder(
            firstIndex = currentMediaItemIndex.coerceIn(0, maxOf(itemCount - 1, 0)),
            length = itemCount,
            randomSeed = Random.nextLong()
        )
    )
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