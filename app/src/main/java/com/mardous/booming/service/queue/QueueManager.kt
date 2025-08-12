/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.mardous.booming.service.queue

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import com.mardous.booming.extensions.media.albumCoverUri
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.media.indexOfSong
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.SongProvider
import com.mardous.booming.core.legacy.PlaybackQueueStore
import com.mardous.booming.core.model.shuffle.GroupShuffleMode
import com.mardous.booming.core.model.shuffle.SpecialShuffleMode
import com.mardous.booming.data.mapper.toQueueSong
import com.mardous.booming.data.mapper.toQueueSongs
import com.mardous.booming.data.model.QueueSong
import com.mardous.booming.service.playback.Playback
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.sort.SortOrder
import com.mardous.booming.util.sort.sortedSongs

const val NO_POSITION = -1

private typealias MutablePlayQueue = MutableList<QueueSong>

enum class QueueChangeReason {
    Created, Modified, Cleared
}

class QueueManager {

    private val shuffleManager = ShuffleManager()

    private val lock = Any()
    private val observers = mutableListOf<QueueObserver>()

    private var _originalPlayingQueue = mutableListOf<QueueSong>()
    private var _playingQueue = mutableListOf<QueueSong>()

    private var _repeatMode = Playback.RepeatMode.Off
    private var _shuffleMode = Playback.ShuffleMode.Off

    val repeatMode get() = _repeatMode
    val shuffleMode get() = _shuffleMode

    var stopPosition = NO_POSITION
    var nextPosition = NO_POSITION

    var position = NO_POSITION
        private set

    val currentSong: Song
        get() = getSongAt(position)

    val nextSong: Song
        get() = getSongAt(getNextPosition(false))

    val playingQueue: List<Song> get() = _playingQueue
    val isEmpty get() = playingQueue.isEmpty()
    val isFirstTrack get() = position == 0
    val isLastTrack get() = position == playingQueue.lastIndex
    val isStopPosition get() = stopPosition == position

    private val lastUpcomingPosition: Int
        get() = synchronized(lock) {
            _playingQueue.indexOfLast { it.isUpcoming }
        }

    init {
        setSequentialMode(Preferences.queueNextSequentially)
    }

    fun addObserver(observer: QueueObserver) = synchronized(lock) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    fun removeObserver(observer: QueueObserver) = synchronized(lock) {
        observers.remove(observer)
    }

    var isSequentialQueue: Boolean = false
    fun setSequentialMode(isSequentialQueue: Boolean) {
        if (!isSequentialQueue) {
            removeAllRanges()
        }
        this.isSequentialQueue = isSequentialQueue
    }

    fun getMediaSessionQueue() = playingQueue.mapIndexed { index, song ->
        val mediaDescription = MediaDescriptionCompat.Builder()
            .setMediaId(song.id.toString())
            .setTitle(song.title)
            .setSubtitle(song.displayArtistName())
            .setIconUri(song.albumId.albumCoverUri())
            .setMediaUri(song.mediaStoreUri)
            .build()
        MediaSessionCompat.QueueItem(mediaDescription, song.hashCode().toLong() * index)
    }

    suspend fun open(
        queue: List<Song>,
        startPosition: Int,
        shuffleMode: Playback.ShuffleMode?
    ): Int {
        var position = startPosition
        val newShuffleMode = shuffleMode ?: if (Preferences.rememberShuffleMode) {
            this.shuffleMode
        } else {
            Playback.ShuffleMode.Off
        }
        val result = createQueue(queue, startPosition, newShuffleMode) {
            if (newShuffleMode == Playback.ShuffleMode.On) {
                position = 0
                makeShuffleList(this, startPosition)
            }
            this
        }
        if (result == SUCCESS) {
            setPositionTo(position)
        }
        return result
    }

    suspend fun specialShuffleQueue(songs: List<Song>, shuffleMode: SpecialShuffleMode): Boolean {
        val result = createQueue(songs, 0, Playback.ShuffleMode.On) {
            shuffleManager.applySmartShuffle(this, shuffleMode).toQueueSongs()
        }
        if (result == SUCCESS) {
            setPositionTo(0)
            return true
        }
        return false
    }

    suspend fun shuffleUsingProviders(
        providers: List<SongProvider>,
        shuffleMode: GroupShuffleMode,
        defaultSortKey: String? = null
    ): Boolean {
        val songs = providers.flatMap { it.songs }.sortedSongs(SortOrder.songSortOrder)
        val result = createQueue(songs, 0, Playback.ShuffleMode.On) {
            shuffleManager.shuffleByProvider(providers, shuffleMode, defaultSortKey).toQueueSongs()
        }
        if (result == SUCCESS) {
            setPositionTo(0)
            return true
        }
        return false
    }

    fun getSongAt(position: Int): Song = synchronized(lock) {
        return playingQueue.getOrElse(position) { Song.emptySong }
    }

    fun getDuration(position: Int) = synchronized(lock) {
        if (position >= 0) playingQueue.drop(position).sumOf { it.duration } else 0
    }

    fun getNextPosition(force: Boolean): Int {
        var nextPosition = position + 1
        when (repeatMode) {
            Playback.RepeatMode.All -> if (isLastTrack) {
                nextPosition = 0
            }

            Playback.RepeatMode.One -> if (force) {
                if (isLastTrack) {
                    nextPosition = 0
                }
            } else {
                nextPosition -= 1
            }

            Playback.RepeatMode.Off -> if (isLastTrack) {
                nextPosition -= 1
            }
        }
        return nextPosition
    }

    fun getPreviousPosition(force: Boolean): Int {
        var newPosition = this.position - 1
        when (repeatMode) {
            Playback.RepeatMode.All -> if (newPosition < 0) {
                newPosition = playingQueue.size - 1
            }

            Playback.RepeatMode.One -> if (force) {
                if (newPosition < 0) {
                    newPosition = playingQueue.size - 1
                }
            } else {
                newPosition = this.position
            }

            Playback.RepeatMode.Off -> if (newPosition < 0) {
                newPosition = 0
            }
        }
        return newPosition
    }

    fun queueNext(song: Song) {
        if (!isSequentialQueue) {
            addSong(position + 1, song)
        } else {
            modifyQueue { playingQueue, originalPlayingQueue ->
                val queueSong = song.toQueueSong(true)
                if (position == playingQueue.lastIndex) {
                    playingQueue.add(queueSong)
                    originalPlayingQueue.add(queueSong)
                } else for (i in (position + 1) until playingQueue.size) {
                    val item = playingQueue[i]
                    if (item.isUpcoming) {
                        if (i == playingQueue.lastIndex) {
                            playingQueue.add(queueSong)
                            originalPlayingQueue.add(queueSong)
                            break
                        }
                    } else {
                        playingQueue.add(i, queueSong)
                        originalPlayingQueue.add(i, queueSong)
                        break
                    }
                }
            }
        }
    }

    fun queueNext(songs: List<Song>) {
        if (!isSequentialQueue) {
            addSongs(position + 1, songs)
        } else {
            modifyQueue { playingQueue, originalPlayingQueue ->
                val queueSong = songs.toQueueSongs(true)
                if (position == playingQueue.lastIndex) {
                    playingQueue.addAll(queueSong)
                    originalPlayingQueue.addAll(queueSong)
                } else for (i in (position + 1) until playingQueue.size) {
                    val item = playingQueue[i]
                    if (item.isUpcoming) {
                        if (i == playingQueue.lastIndex) {
                            playingQueue.addAll(queueSong)
                            originalPlayingQueue.addAll(queueSong)
                            break
                        }
                    } else {
                        playingQueue.addAll(i, queueSong)
                        originalPlayingQueue.addAll(i, queueSong)
                        break
                    }
                }
            }
        }
    }

    fun addSong(position: Int, song: Song) {
        modifyQueue { playingQueue, originalPlayingQueue ->
            val isUpcomingRange = isInUpcomingRange(position)
            val queueSong = song.toQueueSong(isUpcomingRange)
            playingQueue.add(position, queueSong)
            originalPlayingQueue.add(position, queueSong)
        }
    }

    fun addSong(song: Song) {
        modifyQueue { playingQueue, originalPlayingQueue ->
            val queueSong = song.toQueueSong()
            playingQueue.add(queueSong)
            originalPlayingQueue.add(queueSong)
        }
    }

    fun addSongs(position: Int, songs: List<Song>) {
        modifyQueue { playingQueue, originalPlayingQueue ->
            val isUpcomingRange = isInUpcomingRange(position)
            val queueSongs = songs.toQueueSongs(isUpcomingRange)
            playingQueue.addAll(position, queueSongs)
            originalPlayingQueue.addAll(position, queueSongs)
        }
    }

    fun addSongs(songs: List<Song>) {
        modifyQueue { playingQueue, originalPlayingQueue ->
            val queueSongs = songs.toQueueSongs()
            playingQueue.addAll(queueSongs)
            originalPlayingQueue.addAll(queueSongs)
        }
    }

    fun moveSong(from: Int, to: Int) {
        if (from == to)
            return

        modifyQueue { playingQueue, originalPlayingQueue ->
            val lastUpcomingIndex = lastUpcomingPosition
            val currPosition = this.position
            val songToMove = playingQueue.removeAt(from)
            songToMove.isUpcoming = isInUpcomingRange(to)
            playingQueue.add(to, songToMove)
            if (shuffleMode == Playback.ShuffleMode.Off) {
                val tmpSong = originalPlayingQueue.removeAt(from)
                originalPlayingQueue.add(to, tmpSong)
            }
            when {
                currPosition in to until from -> {
                    setPositionTo(currPosition + 1)
                }
                currPosition in (from + 1)..to -> {
                    setPositionTo(currPosition - 1)
                }
                from == currPosition -> {
                    setPositionTo(to)
                    if (to < currPosition) {
                        realignUpcomingRange(lastIndex = lastUpcomingIndex)
                    } else if (to >= lastUpcomingIndex) {
                        removeAllRanges()
                    }
                }
            }
        }
    }

    fun removeSong(position: Int) {
        modifyQueue { playingQueue, originalPlayingQueue ->
            if (shuffleMode == Playback.ShuffleMode.Off) {
                playingQueue.removeAt(position)
                originalPlayingQueue.removeAt(position)
            } else {
                originalPlayingQueue.remove(playingQueue.removeAt(position))
            }
            rePosition(position, playingQueue)
        }
    }

    fun removeSong(song: Song) {
        modifyQueue { playingQueue, originalPlayingQueue ->
            playingQueue.removeSong(song)
            originalPlayingQueue.removeSong(song)
        }
    }

    fun removeSongs(songs: List<Song>) {
        modifyQueue { playingQueue, originalPlayingQueue ->
            for (song in songs) {
                playingQueue.removeSong(song)
                originalPlayingQueue.removeSong(song)
            }
        }
    }

    fun setPosition(newPosition: Int) {
        setPositionTo(newPosition, realign = true)
    }

    fun setPositionToNext() {
        setPositionTo(nextPosition, realign = true)
    }

    private fun setPositionTo(
        newPosition: Int,
        rePosition: Boolean = false,
        realign: Boolean = false
    ) {
        val oldPosition = this.position
        if (realign) {
            val lastUpcomingPosition = this.lastUpcomingPosition
            val isUpcomingRange = isInUpcomingRange(
                targetIndex = newPosition,
                firstIndex = oldPosition,
                lastIndex = lastUpcomingPosition
            )
            if (newPosition < oldPosition || isUpcomingRange) {
                // First we check if the new position is further back than the
                // current one or further forward but within the allowed range.
                // If so, it will only be necessary to realign the upcoming tracks.
                realignUpcomingRange(firstIndex = newPosition, lastIndex = lastUpcomingPosition)
            } else if (newPosition > lastUpcomingPosition) {
                // In case the new position has exceeded the allowed range,
                // we discard it completely.
                removeAllRanges()
            }
        }
        this.position = newPosition
        if (stopPosition < position) {
            stopPosition = NO_POSITION
        }
        doDispatchChange {
            it.queuePositionChanged(position, rePosition)
            it.songChanged(currentSong, nextSong)
        }
    }

    /**
     * Adjusts the current playback position after a song has been removed from the queue.
     *
     * This method ensures that the playback position remains valid and correctly reflects
     * the currently playing item after a deletion.
     *
     * @param deletedPosition the index of the item that was removed from the queue
     */
    private fun rePosition(deletedPosition: Int, playingQueue: List<Song>) {
        val currentPosition = this.position

        // Case 1: A song was removed before the current one.
        // Since the list shifts left, we need to decrement the position.
        if (deletedPosition < currentPosition) {
            setPositionTo(currentPosition - 1)
        }

        // Case 2: The currently playing song was removed.
        else if (deletedPosition == currentPosition) {
            if (playingQueue.size > deletedPosition) {
                // Sub-case: There is still a song at the same index (the list was longer).
                // In this case, keep the same position to continue playback with the new song now at that index.
                setPositionTo(position, rePosition = true, realign = true)
            } else {
                // Sub-case: The removed song was the last in the queue.
                // Move the position back to the previous song (if any).
                setPositionTo(position - 1, rePosition = true, realign = true)
            }
        }
    }


    fun shuffleQueue() = synchronized(lock) {
        val currentPosition = position
        val endPosition = playingQueue.lastIndex
        if (currentPosition == endPosition)
            return

        modifyQueue { playingQueue, _ ->
            // Check that there are at least two tracks to make a shuffle list
            if (endPosition - currentPosition >= 2) {
                playingQueue.subList(currentPosition + 1, endPosition + 1).shuffle()
                removeAllRanges()
            }
        }
    }

    fun restoreState(shuffleMode: Playback.ShuffleMode, repeatMode: Playback.RepeatMode) {
        this._shuffleMode = shuffleMode
        this._repeatMode = repeatMode
    }

    suspend fun restoreQueues(
        restoredQueue: List<Song>,
        restoredOriginalQueue: List<Song>,
        restoredPosition: Int
    ): Boolean {
        val result = createQueue(restoredOriginalQueue, restoredPosition, shuffleMode) {
            restoredQueue.toQueueSongs().toMutableList()
        }
        if (result == SUCCESS) {
            setPositionTo(restoredPosition)
            return true
        }
        return false
    }

    fun setRepeatMode(mode: Playback.RepeatMode) {
        this._repeatMode = mode
        doDispatchChange {
            it.songChanged(currentSong, nextSong)
            it.repeatModeChanged(mode)
        }
    }

    fun setShuffleMode(mode: Playback.ShuffleMode) {
        removeAllRanges()
        when (mode) {
            Playback.ShuffleMode.On -> synchronized(lock) {
                makeShuffleList(_playingQueue, position)
                setPositionTo(0)
            }

            Playback.ShuffleMode.Off -> synchronized(lock) {
                val currentSong = this.currentSong
                var newPosition = 0
                _playingQueue = _originalPlayingQueue.toMutableList()
                for (song in _playingQueue) {
                    if (song.id == currentSong.id) {
                        newPosition = _playingQueue.indexOf(song)
                    }
                }
                setPositionTo(newPosition)
            }
        }
        this._shuffleMode = mode
        setPlayingQueue(_playingQueue, QueueChangeReason.Modified)
        doDispatchChange {
            it.shuffleModeChanged(mode)
        }
    }

    private fun setPlayingQueue(
        playingQueue: MutableList<QueueSong>,
        changeReason: QueueChangeReason
    ) {
        this.stopPosition = NO_POSITION
        this._playingQueue = playingQueue
        doDispatchChange { 
            it.queueChanged(playingQueue.toList(), changeReason)
            if (changeReason != QueueChangeReason.Created) {
                it.songChanged(currentSong, nextSong)
            }
        }
    }

    fun clearQueue() {
        _originalPlayingQueue.clear()
        _playingQueue.clear()
        setPositionTo(NO_POSITION)
        this.stopPosition = NO_POSITION
        setPlayingQueue(_playingQueue, QueueChangeReason.Cleared)
    }

    fun updateSong(id: Long, song: Song) {
        modifyQueue { playingQueue, originalPlayingQueue ->
            if (playingQueue.isNotEmpty() && originalPlayingQueue.isNotEmpty()) {
                for (i in playingQueue.indices) {
                    val songAt = playingQueue[i]
                    if (songAt.id == id) {
                        playingQueue[i] = song.toQueueSong(songAt.isUpcoming)
                    }
                }
                for (i in originalPlayingQueue.indices) {
                    val songAt = playingQueue[i]
                    if (songAt.id == id) {
                        playingQueue[i] = song.toQueueSong(songAt.isUpcoming)
                    }
                }
            }
        }
        if (id == currentSong.id) {
            doDispatchChange {
                it.songChanged(currentSong, nextSong)
            }
        }
    }

    private fun modifyQueue(
        modifier: (playingQueue: MutablePlayQueue, originalPlayingQueue: MutablePlayQueue) -> Unit
    ) = synchronized(lock) {
        modifier(_playingQueue, _originalPlayingQueue)
        setPlayingQueue(_playingQueue, QueueChangeReason.Modified)
    }

    private suspend fun createQueue(
        source: List<Song>,
        startPosition: Int,
        shuffleMode: Playback.ShuffleMode,
        onCreated: suspend MutableList<QueueSong>.() -> List<QueueSong>
    ): Int {
        val filteredSource = source.filterNot { it == Song.emptySong }
        if (filteredSource.isNotEmpty() && startPosition >= 0 && startPosition < filteredSource.size) {
            val queueSongs = filteredSource.toQueueSongs().toMutableList()
            if (queueSongs == _originalPlayingQueue && shuffleMode == this.shuffleMode && !shuffleMode.isOn) {
                return HANDLED_SOURCE
            }
            // it is important to copy the playing queue here first as we might add/remove songs later
            _originalPlayingQueue = queueSongs
            setPlayingQueue(
                playingQueue = onCreated(_originalPlayingQueue.toMutableList()).toMutableList(),
                changeReason = QueueChangeReason.Created
            )
            this._shuffleMode = shuffleMode
            doDispatchChange {
                it.shuffleModeChanged(shuffleMode)
            }
            return SUCCESS
        }
        return EMPTY_SOURCE
    }

    private fun doDispatchChange(dispatch: Boolean = true, dispatcher: (QueueObserver) -> Unit) = synchronized(lock) {
        if (dispatch) {
            observers.forEach(dispatcher)
        }
    }

    private fun MutableList<QueueSong>.removeSong(song: Song) {
        var deletePosition = indexOfSong(song.id)
        while (deletePosition != -1) {
            removeAt(deletePosition)
            rePosition(deletePosition, this)
            deletePosition = indexOfSong(song.id)
        }
    }

    private fun <T : Song> makeShuffleList(listToShuffle: MutableList<T>, current: Int) {
        if (current >= 0) {
            val song = listToShuffle.removeAt(current)
            listToShuffle.shuffle()
            listToShuffle.add(0, song)
        } else {
            listToShuffle.shuffle()
        }
    }

    suspend fun saveQueues(queueStore: PlaybackQueueStore) {
        queueStore.saveQueues(_playingQueue, _originalPlayingQueue)
    }

    private fun isInUpcomingRange(
        targetIndex: Int,
        firstIndex: Int = position,
        lastIndex: Int = lastUpcomingPosition
    ): Boolean {
        if (!isSequentialQueue)
            return false

        if (lastIndex == -1) return false
        return targetIndex in (firstIndex + 1)..lastIndex
    }

    /**
     * Keeps the status of the upcoming tracks consistent. Basically it removes the
     * value from those tracks that are out of range (which starts from the position
     * immediately following the current one, and ends at the last track that has
     * been added).
     *
     * It is valid to specify a different range depending on the need of the situation.
     */
    private fun realignUpcomingRange(
        firstIndex: Int = position,
        lastIndex: Int = lastUpcomingPosition
    ) = synchronized(lock) {
        if (!isSequentialQueue)
            return

        if (lastIndex == -1) return // there is nothing to realign
        if (lastIndex == firstIndex) {
            _playingQueue[firstIndex].isUpcoming = false
            return
        }
        for ((i, song) in _playingQueue.withIndex()) {
            song.isUpcoming = !(i <= firstIndex || i > lastIndex)
        }
    }

    /**
     * Remove all upcoming tracks. However, this does not actually remove the items
     * from the list, it just removes the [QueueSong.isUpcoming] value from them.
     */
    private fun removeAllRanges() = synchronized(lock) {
        if (!isSequentialQueue)
            return

        _playingQueue.forEach { it.isUpcoming = false }
    }

    companion object {
        const val SUCCESS = 0
        const val EMPTY_SOURCE = 1
        const val HANDLED_SOURCE = 2
    }
}