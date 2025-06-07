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
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.mardous.booming.R
import com.mardous.booming.extensions.execSafe
import com.mardous.booming.extensions.hasPie
import com.mardous.booming.extensions.showToast
import com.mardous.booming.recordException
import com.mardous.booming.service.playback.Playback
import com.mardous.booming.util.Preferences
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

open class MultiPlayer(private val context: Context) : Playback,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

    private var mCallbacks: Playback.PlaybackCallbacks? = null
    private var mCurrentMediaPlayer = MediaPlayer()
    private var mNextMediaPlayer: MediaPlayer? = null

    private var mDynamicsProcessing: DynamicsProcessing? = null

    private var mIsInitialized = false

    // Store balance values here:
    // Index 0 = left volume.
    // Index 1 = right volume.
    private val mBalance = FloatArray(2)
    private var mReplayGain = Float.NaN

    override fun setDataSource(path: String, completion: (success: Boolean) -> Unit) {
        mIsInitialized = false
        setDataSourceImpl(mCurrentMediaPlayer, path) { success ->
            mIsInitialized = success
            if (mIsInitialized) {
                setNextDataSource(null)
            }
            completion(mIsInitialized)
        }
    }

    /**
     * Set the MediaPlayer to start when this MediaPlayer finishes playback.
     *
     * @param path The path of the file, or the http/rtsp url of the stream
     * you want to play
     */
    override fun setNextDataSource(path: String?) {
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
        if (path == null) {
            return
        }
        if (Preferences.gaplessPlayback) {
            mNextMediaPlayer = MediaPlayer()
            mNextMediaPlayer?.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            mNextMediaPlayer?.audioSessionId = getAudioSessionId()
            setDataSourceImpl(mNextMediaPlayer!!, path) { success ->
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
     * @param player The [MediaPlayer] to use
     * @param path The path of the file, or the http/rtsp URL of the stream you want to play
     * @return True if the <code>player</code> has been prepared and is ready to play, false otherwise
     */
    private fun setDataSourceImpl(
        player: MediaPlayer,
        path: String,
        completion: (success: Boolean) -> Unit,
    ) {
        player.reset()
        try {
            if (path.startsWith("content://")) {
                player.setDataSource(context, path.toUri())
            } else {
                player.setDataSource(path)
            }
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.setOnPreparedListener {
                player.setAuxEffectSendLevel(1.0f)
                player.setOnPreparedListener(null)
                completion(true)
            }
            player.prepare()
        } catch (e: Exception) {
            completion(false)
            e.printStackTrace()
        }
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)
    }

    override fun getCallbacks(): Playback.PlaybackCallbacks? {
        return mCallbacks
    }

    /**
     * Sets the callbacks
     *
     * @param callbacks The callbacks to use
     */
    override fun setCallbacks(callbacks: Playback.PlaybackCallbacks) {
        this.mCallbacks = callbacks
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
        mDynamicsProcessing?.release()
        mDynamicsProcessing = null
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

    override fun seek(whereto: Int) {
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
        mBalance[0] = left // Index: 0 = left balance.
        mBalance[1] = right // Index: 1 = right balance.
        updateVolume()
    }

    override fun setReplayGain(replayGain: Float) {
        mReplayGain = replayGain
        updateVolume()
    }

    override fun setVolume(leftVol: Float, rightVol: Float) {
        mCurrentMediaPlayer.execSafe {
            setVolume(leftVol, rightVol)
        }
    }

    private fun updateVolume() {
        var leftVol = mBalance[0]
        var rightVol = mBalance[1]

        if (!mReplayGain.isNaN()) {
            // setVolume uses a linear scale
            val rgResult = (10.0f.pow((mReplayGain / 20.0f)))
            max(0.0f, min(1.0f, rgResult)).let { volume ->
                leftVol *= volume
                rightVol *= volume
            }
        }

        if (hasPie()) {
            try {
                applyReplayGainOnDynamicsProcessing()
                // DynamicsProcessing is in charge of replay gain, revert volume to default values
                leftVol = mBalance[0]
                rightVol = mBalance[1]
            } catch (error: RuntimeException) {
                // This can happen with:
                // - UnsupportedOperationException: an external equalizer is in use
                // - RuntimeException: AudioEffect: set/get parameter error
                // Fallback to volume modification in this case
                recordException(error)
            }
        }

        setVolume(leftVol, rightVol)
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private fun applyReplayGainOnDynamicsProcessing() {
        if (mReplayGain.isNaN()) {
            mDynamicsProcessing?.release()
            mDynamicsProcessing = null
        } else {
            if (mDynamicsProcessing == null) {
                mDynamicsProcessing = DynamicsProcessing(mCurrentMediaPlayer.audioSessionId).also {
                    it.setEnabled(true)
                }
            }
            // setInputGainAllChannelsTo uses a dB scale
            mDynamicsProcessing?.setInputGainAllChannelsTo(mReplayGain)
        }
    }

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