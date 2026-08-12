/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.core.model.queue

import androidx.media3.common.MediaItem
import com.mardous.booming.data.model.QueueSong
import com.mardous.booming.data.model.Song

/** Represents the current state of the queue. */
data class QueueSnapshot(
    /**
     * Play queue items ordered according to the current state of Shuffle mode.
     */
    val mediaItems: List<MediaItem>,
    /**
     * Actual indices of [MediaItem]s in the [Timeline] ordered according to
     * the current state of Shuffle mode.
     */
    val indicesInTimeline: IntArray,
    /**
     * Position of the currently playing [MediaItem] in [mediaItems].
     */
    val positionInMediaItems: Int
) {
    /** Whether the queue is empty. */
    val isEmpty: Boolean
        get() = mediaItems.isEmpty()

    /** The number of items in the queue. */
    val size: Int
        get() = mediaItems.size

    fun createPosition() = QueuePosition(positionInMediaItems, indicesInTimeline)

    fun deriveQueueSongs(songs: List<Song>): List<QueueSong> {
        val occurrences = mutableMapOf<Long, Int>()
        return songs.map { song ->
            val count = occurrences.getOrDefault(song.id, 0)
            occurrences[song.id] = count + 1
            QueueSong(key = song.id to count, song)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueueSnapshot

        if (positionInMediaItems != other.positionInMediaItems) return false
        if (mediaItems != other.mediaItems) return false
        if (!indicesInTimeline.contentEquals(other.indicesInTimeline)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = positionInMediaItems
        result = 31 * result + mediaItems.hashCode()
        result = 31 * result + indicesInTimeline.contentHashCode()
        return result
    }

    companion object {
        /** An empty queue. */
        val Empty = QueueSnapshot(emptyList(), intArrayOf(), -1)
    }
}