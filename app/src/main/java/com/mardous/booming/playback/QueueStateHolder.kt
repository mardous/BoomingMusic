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

package com.mardous.booming.playback

import androidx.media3.common.Player
import com.mardous.booming.core.model.queue.QueuePosition
import com.mardous.booming.data.model.QueueSong
import com.mardous.booming.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlin.concurrent.atomics.ExperimentalAtomicApi

const val QUEUE_DEBOUNCE = 150L

/**
 * A reactive container for the playback queue's state.
 *
 * [QueueStateHolder] acts as a single source of truth for the current state of the playback queue,
 * including the list of songs, current position, and playback modes (shuffle/repeat).
 *
 * It exposes this state through [kotlinx.coroutines.flow.StateFlow]s, allowing UI components
 * and other services to react to changes in the queue without being directly tied to the
 * [androidx.media3.common.Player] or [PlaybackService].
 */
@OptIn(ExperimentalAtomicApi::class)
class QueueStateHolder {

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueSong>>(emptyList())
    val queue = _queue.asStateFlow()

    private val _position = MutableStateFlow(QueuePosition.Undefined)
    val position = _position.asStateFlow()

    val currentSong = combine(queue, position) { queue, position ->
        queue.getOrElse(position.current) { Song.emptySong }
    }

    val nextSong = combine(queue, position) { queue, position ->
        queue.getOrElse(position.next) { Song.emptySong }
    }

    /** The total number of items currently in the playback queue. */
    val queueSize: Int
        get() = queue.value.size

    /** Updates the current shuffle mode state. */
    fun submitShuffleMode(shuffleMode: Boolean) { _shuffleMode.value = shuffleMode }

    /** Updates the current repeat mode state. */
    fun submitRepeatMode(repeatMode: Int) { _repeatMode.value = repeatMode }

    /**
     * Updates the entire queue and its corresponding position mapping.
     *
     * @param queue The new list of songs.
     * @param position The initial [QueuePosition] mapping for this queue.
     */
    fun submitQueue(queue: List<QueueSong>, position: QueuePosition) {
        _queue.value = queue
        _position.value = position
    }

    /**
     * Updates the current position index in the queue.
     *
     * Use this when the player moves to a different item to update the visual "current" marker.
     *
     * @param index The structural timeline index of the item now playing.
     */
    fun setPlayerIndex(index: Int) {
        _position.update { it.setCurrentIndex(index) }
    }
}
