package com.mardous.booming.playback

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.mardous.booming.data.model.Song

@OptIn(UnstableApi::class)
internal fun MediaSession.isRemoteController(controller: MediaSession.ControllerInfo): Boolean {
    return isMediaNotificationController(controller) ||
            isAutoCompanionController(controller) ||
            isAutomotiveController(controller)
}

val Player.mediaItems: List<MediaItem>
    get() = (0 until mediaItemCount).map { getMediaItemAt(it) }

val Player.actualMediaItems: List<MediaItem>
    get() {
        val shuffleMode = shuffleModeEnabled
        val items = mutableListOf<MediaItem>()
        var index = currentTimeline.getFirstWindowIndex(shuffleMode)
        while (index != C.INDEX_UNSET) {
            items.add(getMediaItemAt(index))
            index = currentTimeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffleMode)
        }
        return items
    }

fun List<Song>.toMediaItems() = map { it.toMediaItem() }

fun MediaItem.withExtras(consumer: Bundle.() -> Unit) = buildUpon()
    .setMediaMetadata(mediaMetadata.withExtras(consumer))
    .build()

fun MediaMetadata.withExtras(consumer: Bundle.() -> Unit) = buildUpon()
    .setExtras(getOrCreateExtras().apply(consumer))
    .build()

fun MediaMetadata.getOrCreateExtras() = extras ?: bundleOf()

val MediaItem?.song: Song
    get() = (this?.localConfiguration?.tag as? Song) ?: Song.emptySong