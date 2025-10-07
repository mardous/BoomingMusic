package com.mardous.booming.util

import com.mardous.booming.data.model.Song

class SongPlayCountHelper {

    private val stopWatch = StopWatch()
    var song = Song.Companion.emptySong
        private set

    var called = 0

    fun shouldBumpPlayCount(): Boolean {
        return song.duration * 0.5 < stopWatch.elapsedTime
    }

    fun notifySongChanged(song: Song, isPlaying: Boolean) {
        synchronized(this) {
            stopWatch.reset()
            if (isPlaying) {
                stopWatch.start()
            }
            this.song = song
        }
    }

    fun notifyPlayStateChanged(isPlaying: Boolean) {
        synchronized(this) {
            if (isPlaying) {
                stopWatch.start()
            } else {
                stopWatch.pause()
            }
        }
    }

    companion object {
        val TAG: String = SongPlayCountHelper::class.java.simpleName
    }
}