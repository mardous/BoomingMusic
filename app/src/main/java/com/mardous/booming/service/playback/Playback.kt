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

package com.mardous.booming.service.playback

import android.media.AudioDeviceInfo
import android.os.Build
import androidx.annotation.RequiresApi

interface Playback {

    class RepeatMode {
        companion object {
            const val OFF = 0
            const val CURRENT = 1
            const val ALL = 2
        }
    }

    class ShuffleMode {
        companion object {
            const val OFF = 0
            const val ON = 1
        }
    }

    interface PlaybackCallbacks {
        fun onTrackWentToNext()

        fun onTrackEnded()

        fun onPlayStateChanged()
    }

    fun setDataSource(path: String, completion: (success: Boolean) -> Unit)
    fun setNextDataSource(path: String?)
    fun getCallbacks(): PlaybackCallbacks?
    fun setCallbacks(callbacks: PlaybackCallbacks)
    fun isInitialized(): Boolean
    fun start(): Boolean
    fun stop()
    fun release()
    fun pause(): Boolean
    fun isPlaying(): Boolean
    fun duration(): Int
    fun position(): Int
    fun seek(whereto: Int)
    fun setTempo(speed: Float, pitch: Float)
    fun setBalance(left: Float, right: Float)
    fun setReplayGain(replayGain: Float)
    fun setVolume(leftVol: Float, rightVol: Float)
    fun setAudioSessionId(sessionId: Int): Boolean
    fun getAudioSessionId(): Int
    fun getSpeed(): Float

    @RequiresApi(Build.VERSION_CODES.P)
    fun getRoutedDevice(): AudioDeviceInfo?
}