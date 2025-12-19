package com.mardous.booming.playback

import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import org.koin.core.component.KoinComponent

@UnstableApi
class AdvancedForwardingPlayer(
    player: Player
) : ForwardingPlayer(player), KoinComponent {

    private val listeners = mutableListOf<Player.Listener>()

    private var sequentialTimelineEnabled = false

    private val internalListener = object : Player.Listener {
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            val lastUpcomingIndex = getLastUpcomingItemIndex()
            if (newPosition.mediaItemIndex > lastUpcomingIndex) {
                onClearUpcomingRange()
                return
            }
            val isUpcomingRange = isIndexInUpcomingRange(
                index = newPosition.mediaItemIndex,
                rangeStart = oldPosition.mediaItemIndex,
                rangeEnd = lastUpcomingIndex
            )
            if (isUpcomingRange) {
                onRealignUpcomingRange(
                    rangeStart = newPosition.mediaItemIndex,
                    rangeEnd = lastUpcomingIndex
                )
            }
        }
    }

    val exoPlayer get() = wrappedPlayer as ExoPlayer

    init {
        wrappedPlayer.addListener(internalListener)
    }

    override fun release() {
        wrappedPlayer.removeListener(internalListener)
        super.release()
    }

    override fun addListener(listener: Player.Listener) {
        listeners.add(listener)
        super.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        listeners.remove(listener)
        super.removeListener(listener)
    }

    fun setSequentialTimelineEnabled(sequentialTimelineEnabled: Boolean) {
        if (sequentialTimelineEnabled != this.sequentialTimelineEnabled) {
            if (!sequentialTimelineEnabled) {
                onClearUpcomingRange()
            }
        }
        this.sequentialTimelineEnabled = sequentialTimelineEnabled
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        if (!sequentialTimelineEnabled) {
            super.addMediaItem(index, mediaItem)
            return
        }

        val nextMediaItemIndex = this.nextMediaItemIndex
        if (index == nextMediaItemIndex) {
            val mediaItemCount = this.mediaItemCount
            if (!hasNextMediaItem()) {
                addMediaItem(mediaItem.getUpcomingMediaItem(true))
            } else for (i in nextMediaItemIndex until mediaItemCount) {
                val item = getMediaItemAt(i)
                if (item.isUpcoming()) {
                    if (i == (mediaItemCount - 1)) {
                        addMediaItem(mediaItem.getUpcomingMediaItem(true))
                        break
                    }
                } else {
                    super.addMediaItem(i, mediaItem.withExtras {
                        putBoolean(IS_UPCOMING, true)
                        putInt(NORMAL_INDEX, i)
                    })
                    break
                }
            }
            return
        } else {
            if (isIndexInUpcomingRange(index)) {
                super.addMediaItem(index, mediaItem.withExtras {
                    putBoolean(IS_UPCOMING, true)
                    putInt(NORMAL_INDEX, index)
                })
                return
            }
        }
        super.addMediaItem(index, mediaItem)
    }

    override fun addMediaItems(index: Int, mediaItems: List<MediaItem>) {
        if (!sequentialTimelineEnabled) {
            super.addMediaItems(index, mediaItems)
            return
        }

        val nextMediaItemIndex = this.nextMediaItemIndex
        if (index == nextMediaItemIndex) {
            val mediaItemCount = this.mediaItemCount
            if (!hasNextMediaItem()) {
                addMediaItems(mediaItems.map {
                    it.getUpcomingMediaItem(true)
                })
            } else for (i in nextMediaItemIndex until mediaItemCount) {
                val item = getMediaItemAt(i)
                if (item.isUpcoming()) {
                    if (i == (mediaItemCount - 1)) {
                        addMediaItems(mediaItems.map {
                            it.getUpcomingMediaItem(true)
                        })
                        break
                    }
                } else {
                    super.addMediaItems(i, mediaItems.mapIndexed { originalIndex, item ->
                        item.withExtras {
                            putBoolean(IS_UPCOMING, true)
                            putInt(NORMAL_INDEX, i + originalIndex)
                        }
                    })
                    break
                }
            }
            return
        } else {
            if (isIndexInUpcomingRange(index)) {
                super.addMediaItems(index, mediaItems.mapIndexed { originalIndex, item ->
                    item.withExtras {
                        putBoolean(IS_UPCOMING, true)
                        putInt(NORMAL_INDEX, index + originalIndex)
                    }
                })
                return
            }
        }
        super.addMediaItems(index, mediaItems)
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        if (!sequentialTimelineEnabled) {
            super.moveMediaItem(currentIndex, newIndex)
            return
        }

        // First, we check that the moved item belongs to the "upcoming" range.
        // This range starts just after the currently playing item (currentMediaItemIndex)
        // and ends at the value of getLastUpcomingMediaItemIndex(). Anything outside this
        // range is not upcoming and shouldn't be checked here.
        val lastUpcomingIndex = getLastUpcomingItemIndex()
        if (isIndexInUpcomingRange(currentIndex, rangeEnd = lastUpcomingIndex)) {
            val currentMediaItemIndex = this.currentMediaItemIndex
            if (newIndex < currentIndex) {
                if (currentIndex == currentMediaItemIndex) { // The item was moved back in the list
                    // The moved item is the one currently playing, this item marks the
                    // start of the "upcoming" range so everything that was left in
                    // between its new position and the last item in the range should
                    // also be marked as upcoming
                    onRealignUpcomingRange(
                        rangeStart = newIndex,
                        rangeEnd = lastUpcomingIndex
                    )
                } else {
                    if (newIndex < currentMediaItemIndex) {
                        // The moved item is another item that was marked as upcoming
                        // but was moved behind our current item, so it has gone out of range.
                        // We realign it again with the items that were left inside.
                        onRealignUpcomingRange(
                            rangeStart = currentMediaItemIndex,
                            rangeEnd = lastUpcomingIndex
                        )
                    }
                }
            } else if (newIndex > currentIndex) { // The item was moved forward in the list
                if (currentIndex == currentMediaItemIndex) {
                    if (newIndex > lastUpcomingIndex) {
                        // The moved item is the item being played and it moved beyond
                        // the range, so we just discard the range we had.
                        onClearUpcomingRange()
                    } else {
                        // The moved item is the item being played and it has moved further
                        // into range but hasn't left range yet, so we just realign.
                        onRealignUpcomingRange(
                            rangeStart = currentMediaItemIndex,
                            rangeEnd = lastUpcomingIndex
                        )
                    }
                } else {
                    if (newIndex > lastUpcomingIndex) {
                        // The moved item was taken out of range, we realign what we have.
                        onRealignUpcomingRange(
                            rangeStart = currentMediaItemIndex,
                            rangeEnd = lastUpcomingIndex
                        )
                    }
                }
            }
        }
        super.moveMediaItem(currentIndex, newIndex)
    }

    private fun getLastUpcomingItemIndex(): Int {
        if (mediaItemCount == 0) return C.INDEX_UNSET

        return (0 until mediaItemCount)
            .map { index -> getMediaItemAt(index) }
            .indexOfLast { item -> item.isUpcoming() }
            .takeIf { value -> value > -1 } ?: C.INDEX_UNSET
    }

    private fun isIndexInUpcomingRange(
        index: Int,
        rangeStart: Int = currentMediaItemIndex,
        rangeEnd: Int = getLastUpcomingItemIndex()
    ): Boolean {
        if (!sequentialTimelineEnabled ||
            rangeStart > rangeEnd ||
            rangeEnd == C.INDEX_UNSET) return false

        return index in (rangeStart + 1)..rangeEnd
    }

    private fun onRealignUpcomingRange(
        rangeStart: Int = currentMediaItemIndex,
        rangeEnd: Int = getLastUpcomingItemIndex()
    ) {
        if (!sequentialTimelineEnabled) return

        val mediaItemCount = this.mediaItemCount
        if (mediaItemCount == 0 ||
            rangeStart < 0 ||
            rangeEnd == C.INDEX_UNSET ||
            rangeEnd >= mediaItemCount) return

        if (rangeEnd == rangeStart) {
            replaceMediaItem(rangeStart, getMediaItemAt(rangeStart).getUpcomingMediaItem(false))
            return
        }

        var firstIndex = C.INDEX_UNSET
        var lastIndex = C.INDEX_UNSET

        val updatedMediaItems = (0 until mediaItemCount).map { i ->
            val mediaItem = getMediaItemAt(i)
            val shouldBeUpcoming = (i in (rangeStart + 1)..rangeEnd)
            if (mediaItem.isUpcoming() != shouldBeUpcoming) {
                lastIndex = (i + 1)
                if (firstIndex == C.INDEX_UNSET) {
                    firstIndex = i
                }
                mediaItem.getUpcomingMediaItem(shouldBeUpcoming)
            } else {
                mediaItem
            }
        }

        if (firstIndex != C.INDEX_UNSET && lastIndex != C.INDEX_UNSET) {
            replaceMediaItems(firstIndex, lastIndex, updatedMediaItems.subList(firstIndex, lastIndex))
        }
    }

    private fun onClearUpcomingRange() {
        if (!sequentialTimelineEnabled) return

        var hasChanges = false
        val updatedMediaItems = (0 until mediaItemCount).map { i ->
            val mediaItem = getMediaItemAt(i)
            if (mediaItem.isUpcoming()) {
                hasChanges = true
                mediaItem.getUpcomingMediaItem(false)
            } else {
                mediaItem
            }
        }

        if (hasChanges) {
            replaceMediaItems(0, mediaItemCount, updatedMediaItems)
        }
    }

    private fun MediaItem.isUpcoming() =
        mediaMetadata.extras?.getBoolean(IS_UPCOMING, false) == true

    private fun MediaItem.getUpcomingMediaItem(isUpcoming: Boolean) =
        withExtras { putBoolean(IS_UPCOMING, isUpcoming) }

    companion object {
        private const val NORMAL_INDEX = "__normal_index__"
        private const val IS_UPCOMING = "__is_upcoming__"
    }
}