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

package com.mardous.booming.service

import android.content.*
import android.media.audiofx.AudioEffect
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.mardous.booming.R
import com.mardous.booming.audio.AudioDevice
import com.mardous.booming.audio.getDeviceType
import com.mardous.booming.extensions.hasPie
import com.mardous.booming.extensions.showToast
import com.mardous.booming.model.Song
import com.mardous.booming.service.MusicService.MusicBinder
import com.mardous.booming.service.playback.Playback
import com.mardous.booming.util.Preferences
import java.util.WeakHashMap
import kotlin.random.Random

object MusicPlayer {

    private val mConnectionMap = WeakHashMap<Context, ServiceBinder>()
    internal var musicService: MusicService? = null
        private set

    val routedDevice: AudioDevice?
        get() {
            if (hasPie()) {
                val deviceInfo = musicService?.getRoutedDevice()
                if (deviceInfo != null) {
                    return AudioDevice(deviceInfo.type, deviceInfo.getDeviceType(), deviceInfo.productName)
                }
            }
            return null
        }

    val audioSessionId: Int
        get() = musicService?.getAudioSessionId() ?: AudioEffect.ERROR_BAD_VALUE

    val currentSong: Song
        get() = musicService?.getCurrentSong() ?: Song.emptySong

    val playingQueue: List<Song>
        get() = musicService?.getPlayingQueue() ?: ArrayList()

    val songProgressMillis: Int
        get() = musicService?.getSongProgressMillis() ?: -1

    val songDurationMillis: Int
        get() = musicService?.getSongDurationMillis() ?: -1

    var position: Int
        get() = musicService?.getPosition() ?: -1
        set(position) {
            musicService?.setPosition(position)
        }

    var repeatMode: Int
        get() = musicService?.getRepeatMode() ?: Playback.RepeatMode.OFF
        set(repeatMode) {
            musicService?.setRepeatMode(repeatMode)
        }

    var shuffleMode: Int
        get() = musicService?.getShuffleMode() ?: Playback.ShuffleMode.OFF
        set(shuffleMode) {
            musicService?.setShuffleMode(shuffleMode)
        }

    var isPlaying: Boolean
        get() = musicService?.isPlaying == true
        set(isPlaying) {
            if (isPlaying) musicService?.play()
            else musicService?.pause()
        }

    var pendingQuit: Boolean
        get() = musicService?.pendingQuit ?: false
        set(pendingQuit) {
            musicService?.pendingQuit = pendingQuit
        }

    fun togglePlayPause() {
        isPlaying = !isPlaying
    }

    fun playSongAt(position: Int) {
        musicService?.playSongAt(position)
    }

    fun playNextSong() {
        musicService?.playNextSong(true)
    }

    fun playPreviousSong() {
        musicService?.playPreviousSong(true)
    }

    fun back() {
        musicService?.back(true)
    }

    fun restorePlayback() {
        musicService?.restorePlayback()
    }

    fun openQueue(
        queue: List<Song>,
        position: Int = 0,
        startPlaying: Boolean = true,
        keepShuffleMode: Boolean = Preferences.rememberShuffleMode
    ) {
        musicService?.openQueue(queue, position, startPlaying)
        if (!keepShuffleMode) {
            shuffleMode = Playback.ShuffleMode.OFF
        }
    }

    fun openQueueShuffle(queue: List<Song>, startPlaying: Boolean = true) {
        var startPosition = 0
        if (queue.isNotEmpty()) {
            startPosition = Random.Default.nextInt(queue.size)
        }
        if (!tryToHandleOpenPlayingQueue(queue, startPosition, startPlaying) && musicService != null) {
            openQueue(queue, startPosition, startPlaying)
            shuffleMode = Playback.ShuffleMode.ON
        }
    }

    private fun tryToHandleOpenPlayingQueue(queue: List<Song?>, startPosition: Int, startPlaying: Boolean): Boolean {
        if (playingQueue === queue) {
            if (startPlaying) {
                playSongAt(startPosition)
            } else {
                position = startPosition
            }
            return true
        }
        return false
    }

    fun getSongAt(position: Int): Song = musicService?.getSongAt(position) ?: Song.emptySong

    fun getQueueDurationMillis(position: Int): Long = musicService?.getQueueDurationMillis(position) ?: -1

    fun getQueueDurationInfo(): String? =
        musicService?.getQueueDurationInfo()

    fun getNextSong() = musicService?.getNextSong()

    fun getNextSongInfo(context: Context) = musicService?.getNextSongInfo(context)

    fun seekTo(millis: Int) {
        musicService?.seek(millis)
    }

    fun cycleRepeatMode() {
        musicService?.cycleRepeatMode()
    }

    fun toggleShuffleMode() {
        musicService?.toggleShuffle()
    }

    fun playNext(song: Song, play: Boolean = false) {
        if (musicService != null) {
            if (playingQueue.isNotEmpty()) {
                musicService!!.playNext(song)
            } else {
                openQueue(listOf(song), startPlaying = false)
            }
            if (play) {
                playNextSong()
            } else {
                musicService?.showToast(musicService!!.getString(R.string.added_title_to_playing_queue))
            }
        }
    }

    fun playNext(songs: List<Song>) {
        if (musicService != null) {
            if (playingQueue.isNotEmpty()) {
                musicService!!.playNext(songs)
            } else {
                openQueue(songs, startPlaying = false)
            }

            musicService?.showToast(
                if (songs.size == 1)
                    musicService!!.getString(R.string.added_title_to_playing_queue)
                else musicService!!.getString(R.string.added_x_titles_to_playing_queue, songs.size)
            )
        }
    }

    fun enqueue(song: Song, toPosition: Int = -1) {
        if (musicService != null) {
            if (playingQueue.isNotEmpty()) {
                if (toPosition >= 0) {
                    musicService!!.addSong(toPosition, song)
                } else musicService!!.addSong(song)
            } else {
                openQueue(listOf(song), startPlaying = false)
            }
            musicService?.showToast(musicService!!.getString(R.string.added_title_to_playing_queue))
        }
    }

    fun enqueue(songs: List<Song>) {
        if (musicService != null) {
            if (playingQueue.isNotEmpty()) {
                musicService!!.addSongs(songs)
            } else {
                openQueue(songs, startPlaying = false)
            }
            musicService?.showToast(
                if (songs.size == 1)
                    musicService!!.getString(R.string.added_title_to_playing_queue)
                else musicService!!.getString(R.string.added_x_titles_to_playing_queue, songs.size)
            )
        }
    }

    fun removeFromQueue(song: Song) {
        musicService?.removeSong(song)
    }

    fun removeFromQueue(position: Int) {
        musicService?.removeSong(position)
    }

    fun removeFromQueue(songs: List<Song>): Boolean {
        if (musicService != null) {
            musicService!!.removeSongs(songs)
            return true
        }
        return false
    }

    fun moveToNextPosition(fromPosition: Int) {
        val nextPosition = musicService?.getNextPosition(false)
        if (nextPosition != null) {
            moveSong(fromPosition, nextPosition)
        }
    }

    private fun areValidPositions(vararg positions: Int) =
        positions.all { playingQueue.indices.contains(it) }

    fun moveSong(from: Int, to: Int) {
        if (areValidPositions(from, to)) {
            musicService?.moveSong(from, to)
        }
    }

    fun clearQueue() {
        if (playingQueue.isNotEmpty()) {
            musicService?.clearQueue()
        }
    }

    fun shuffleQueue() {
        musicService?.shuffleQueue()
    }

    /**
     * Set the position of the last song to play before stopping playback.
     *
     * @return *true* if there was a previous "pending stop", *false* otherwise.
     */
    fun setStopPosition(stopPosition: Int): Boolean {
        if (musicService != null) {
            if ((position..playingQueue.lastIndex).contains(stopPosition)) {
                var canceled = false
                if (musicService!!.stopPosition == stopPosition) {
                    musicService!!.setStopPosition(-1)
                    canceled = true
                } else {
                    musicService!!.setStopPosition(stopPosition)
                }

                val messageRes: Int =
                    if (canceled) R.string.sleep_timer_stop_after_x_canceled else R.string.sleep_timer_stop_after_x

                musicService?.showToast(
                    musicService!!.getString(messageRes, musicService!!.getSongAt(stopPosition).title)
                )

                return canceled
            }
        }
        return false
    }

    fun bindToService(context: Context, callback: ServiceConnection): ServiceToken? {
        val contextWrapper = ContextWrapper(context)
        val intent = Intent(contextWrapper, MusicService::class.java)

        // https://issuetracker.google.com/issues/76112072#comment184
        // Workaround for ForegroundServiceDidNotStartInTimeException
        try {
            context.startService(intent)
        } catch (e: Exception) {
            ContextCompat.startForegroundService(context, intent)
        }

        val binder = ServiceBinder(callback)
        if (contextWrapper.bindService(intent, binder, Context.BIND_AUTO_CREATE)) {
            mConnectionMap[contextWrapper] = binder
            return ServiceToken(contextWrapper)
        }
        return null
    }

    fun unbindFromService(token: ServiceToken?) {
        if (token == null) {
            return
        }
        val mContextWrapper = token.mWrappedContext
        val mBinder = mConnectionMap.remove(mContextWrapper) ?: return
        mContextWrapper.unbindService(mBinder)
        if (mConnectionMap.isEmpty()) {
            musicService = null
        }
    }

    class ServiceBinder internal constructor(private val mConnection: ServiceConnection?) :
        ServiceConnection {
        override fun onServiceConnected(component: ComponentName, binder: IBinder) {
            val musicBinder = binder as MusicBinder
            musicService = musicBinder.service
            mConnection?.onServiceConnected(component, binder)
        }

        override fun onServiceDisconnected(component: ComponentName) {
            mConnection?.onServiceDisconnected(component)
            musicService = null
        }
    }

    class ServiceToken internal constructor(val mWrappedContext: ContextWrapper)
}