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

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import com.mardous.booming.R
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.execSafe
import com.mardous.booming.extensions.showToast
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext

open class MultiPlayer(context: Context) : LocalPlayback(context) {

    private var mCurrentMediaPlayer = MediaPlayer()
    private var mNextMediaPlayer: MediaPlayer? = null

    private var mIsInitialized = false

    override suspend fun setDataSource(
        song: Song,
        force: Boolean,
        completion: (success: Boolean) -> Unit
    ) {
        mIsInitialized = false
        setDataSourceImpl(mCurrentMediaPlayer, song.mediaStoreUri.toString()) { success ->
            mIsInitialized = success
            if (mIsInitialized) {
                setNextDataSource(null)
            }
            withContext(Main) {
                completion(mIsInitialized)
            }
        }
    }

    /**
     * Set the MediaPlayer to start when this MediaPlayer finishes playback.
     *
     * @param song The path of the file, or the http/rtsp url of the stream
     * you want to play
     */
    override suspend fun setNextDataSource(song: Song?) {
        try {
            mCurrentMediaPlayer.setNextMediaPlayer(null)
        } catch (e: IllegalArgumentException) {
            Log.i(TAG, "Next media player is current one, continuing")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Media player not initialized!")
            return
        }
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer?.release()
            mNextMediaPlayer = null
        }
        if (song == null) {
            return
        }
        if (Preferences.gaplessPlayback) {
            mNextMediaPlayer = MediaPlayer()
            mNextMediaPlayer?.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            mNextMediaPlayer?.audioSessionId = getAudioSessionId()
            setDataSourceImpl(mNextMediaPlayer!!, song.mediaStoreUri.toString()) { success ->
                if (success) {
                    try {
                        mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer)
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "setNextDataSource: setNextMediaPlayer()", e)
                        if (mNextMediaPlayer != null) {
                            mNextMediaPlayer?.release()
                            mNextMediaPlayer = null
                        }
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "setNextDataSource: setNextMediaPlayer()", e)
                        if (mNextMediaPlayer != null) {
                            mNextMediaPlayer?.release()
                            mNextMediaPlayer = null
                        }
                    }
                } else {
                    if (mNextMediaPlayer != null) {
                        mNextMediaPlayer?.release()
                        mNextMediaPlayer = null
                    }
                }
            }
        }
    }

    /**
     * @return True if the player is ready to go, false otherwise
     */
    override fun isInitialized(): Boolean {
        return mIsInitialized
    }

    /**
     * Starts or resumes playback.
     */
    override fun start(): Boolean {
        return mCurrentMediaPlayer.execSafe {
            start()
            true
        } ?: false
    }

    /**
     * Resets the MediaPlayer to its uninitialized state.
     */
    override fun stop() {
        mCurrentMediaPlayer.reset()
        mIsInitialized = false
    }

    /**
     * Releases resources associated with this MediaPlayer object.
     */
    override fun release() {
        stop()
        mCurrentMediaPlayer.release()
        mNextMediaPlayer?.release()
    }

    /**
     * Pauses playback. Call start() to resume.
     */
    override fun pause(): Boolean {
        return mCurrentMediaPlayer.execSafe {
            pause()
            true
        } ?: false
    }

    /**
     * Checks whether the MultiPlayer is playing.
     */
    override fun isPlaying(): Boolean {
        return mIsInitialized && mCurrentMediaPlayer.isPlaying
    }

    /**
     * Gets the duration of the file.
     *
     * @return The duration in milliseconds
     */
    override fun duration(): Int {
        return if (!mIsInitialized) {
            -1
        } else mCurrentMediaPlayer.execSafe { this.duration } ?: -1
    }

    /**
     * Gets the current playback position.
     *
     * @return The current position in milliseconds
     */
    override fun position(): Int {
        return if (!mIsInitialized) {
            -1
        } else mCurrentMediaPlayer.execSafe { this.currentPosition } ?: -1
    }

    override fun seek(whereto: Int, force: Boolean) {
        mCurrentMediaPlayer.execSafe {
            seekTo(whereto)
        }
    }

    override fun setTempo(speed: Float, pitch: Float) {
        if (isPlaying()) {
            mCurrentMediaPlayer.execSafe {
                playbackParams = playbackParams
                    .setPitch(pitch)
                    .setSpeed(speed)
            }
        }
    }

    /**
     * Sets the audio session ID.
     *
     * @param sessionId The audio session ID
     */
    override fun setAudioSessionId(sessionId: Int): Boolean {
        return mCurrentMediaPlayer.execSafe {
            this.audioSessionId = sessionId
            true
        } ?: false
    }

    /**
     * Returns the audio session ID.
     *
     * @return The current audio session ID.
     */
    override fun getAudioSessionId(): Int {
        return mCurrentMediaPlayer.execSafe { this.audioSessionId } ?: AudioEffect.ERROR_BAD_VALUE
    }

    override fun getSpeed(): Float {
        return mCurrentMediaPlayer.execSafe { this.playbackParams.speed } ?: 1f
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getRoutedDevice(): AudioDeviceInfo? {
        if (isPlaying()) {
            return mCurrentMediaPlayer.execSafe { this.routedDevice }
        }
        return null
    }

    override fun setBalance(
        @FloatRange(from = 0.0, to = 1.0) left: Float,
        @FloatRange(from = 0.0, to = 1.0) right: Float
    ) {
        super.setBalance(left, right)
        updateVolume()
    }

    override fun setReplayGain(replayGain: Float) {
        super.setReplayGain(replayGain)
        updateVolume()
    }

    override fun setVolume(leftVol: Float, rightVol: Float) {
        mCurrentMediaPlayer.execSafe {
            setVolume(leftVol, rightVol)
        }
    }

    override fun setCrossFadeDuration(duration: Int) {}

    /**
     * {@inheritDoc}
     */
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        Log.e(TAG, "onError(): what=$what, extra=$extra")
        if (mp === mCurrentMediaPlayer) {
            context.showToast(context.getString(R.string.unplayable_file_code_x, what))
            mIsInitialized = false
            mCurrentMediaPlayer.release()
            if (mNextMediaPlayer != null) {
                mCurrentMediaPlayer = mNextMediaPlayer!!
                mIsInitialized = true
                mNextMediaPlayer = null

                mCallbacks?.onTrackWentToNext()
            } else {
                mCurrentMediaPlayer = MediaPlayer()
                mCurrentMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            }
        } else {
            mIsInitialized = false
            mCurrentMediaPlayer.release()
            mCurrentMediaPlayer = MediaPlayer()
            mCurrentMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            context.showToast(context.getString(R.string.unplayable_file_code_x, what))
        }
        return false
    }

    /**
     * {@inheritDoc}
     */
    override fun onCompletion(mp: MediaPlayer) {
        if (mp === mCurrentMediaPlayer && mNextMediaPlayer != null) {
            mIsInitialized = false
            mCurrentMediaPlayer.release()
            mCurrentMediaPlayer = mNextMediaPlayer!!
            mIsInitialized = true
            mNextMediaPlayer = null
            mCallbacks?.onTrackWentToNext()
        } else {
            mCallbacks?.onTrackEnded()
        }
    }

    companion object {
        private val TAG = MultiPlayer::class.java.simpleName
    }

    /**
     * Constructor of `MultiPlayer`
     */
    init {
        mCurrentMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
    }
}