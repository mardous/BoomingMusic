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
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import com.mardous.booming.data.model.Song

interface Playback {

    enum class RepeatMode(val value: Int) {
        Off(PlaybackStateCompat.REPEAT_MODE_NONE),
        One(PlaybackStateCompat.REPEAT_MODE_ONE),
        All(PlaybackStateCompat.REPEAT_MODE_ALL);

        val isOn get() = this != Off

        companion object {
            fun fromOrdinal(ordinal: Int) = RepeatMode.entries.getOrElse(ordinal) { Off }
        }
    }

    enum class ShuffleMode(val value: Int) {
        Off(PlaybackStateCompat.SHUFFLE_MODE_NONE),
        On(PlaybackStateCompat.SHUFFLE_MODE_ALL);

        val isOn get() = this != Off

        companion object {
            fun fromOrdinal(ordinal: Int) = ShuffleMode.entries.getOrElse(ordinal) { Off }
        }
    }

    interface PlaybackCallbacks {
        fun onTrackWentToNext()

        fun onTrackEnded()

        fun onTrackEndedWithCrossFade()

        fun onPlayStateChanged()
    }

    suspend fun setDataSource(song: Song, force: Boolean, completion: (success: Boolean) -> Unit)
    suspend fun setNextDataSource(song: Song?)
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
    fun seek(whereto: Int, force: Boolean)
    fun setTempo(speed: Float, pitch: Float)
    fun setBalance(left: Float, right: Float)
    fun setReplayGain(replayGain: Float)
    fun setVolume(leftVol: Float, rightVol: Float)
    fun setCrossFadeDuration(duration: Int)
    fun setAudioSessionId(sessionId: Int): Boolean
    fun getAudioSessionId(): Int
    fun getSpeed(): Float

    @RequiresApi(Build.VERSION_CODES.P)
    fun getRoutedDevice(): AudioDeviceInfo?
}