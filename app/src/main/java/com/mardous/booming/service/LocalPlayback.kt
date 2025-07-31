package com.mardous.booming.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.mardous.booming.service.playback.Playback
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

abstract class LocalPlayback(val context: Context) : Playback, MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

    private var mDynamicsProcessing: DynamicsProcessing? = null

    protected var mCallbacks: Playback.PlaybackCallbacks? = null
        private set

    // Store balance values here:
    // Index 0 = left volume.
    // Index 1 = right volume.
    protected val mBalance = FloatArray(2)
    protected var mReplayGain = Float.NaN

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

    override fun setBalance(
        @FloatRange(from = 0.0, to = 1.0) left: Float,
        @FloatRange(from = 0.0, to = 1.0) right: Float
    ) {
        mBalance[0] = left
        mBalance[1] = right
    }

    override fun setReplayGain(replayGain: Float) {
        mReplayGain = replayGain
    }

    override fun release() {
        mDynamicsProcessing?.release()
        mDynamicsProcessing = null
    }

    protected fun updateVolume(player: MediaPlayer) {
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

        /*if (hasPie()) {
            try {
                applyReplayGainOnDynamicsProcessing(player)
                // DynamicsProcessing is in charge of replay gain, revert volume to default values
                leftVol = mBalance[0]
                rightVol = mBalance[1]
            } catch (error: RuntimeException) {
                // This can happen with:
                // - UnsupportedOperationException: an external equalizer is in use
                // - RuntimeException: AudioEffect: set/get parameter error
                // Fallback to volume modification in this case
            }
        }*/

        setVolume(leftVol, rightVol)
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    protected fun applyReplayGainOnDynamicsProcessing(player: MediaPlayer) {
        if (mReplayGain.isNaN()) {
            mDynamicsProcessing?.release()
            mDynamicsProcessing = null
        } else {
            if (mDynamicsProcessing == null) {
                mDynamicsProcessing = DynamicsProcessing(player.audioSessionId).also {
                    it.setEnabled(true)
                }
            }
            // setInputGainAllChannelsTo uses a dB scale
            mDynamicsProcessing?.setInputGainAllChannelsTo(mReplayGain)
        }
    }

    /**
     * @param player The [MediaPlayer] to use
     * @param path The path of the file, or the http/rtsp URL of the stream you want to play
     * @return True if the <code>player</code> has been prepared and is ready to play, false otherwise
     */
    protected fun setDataSourceImpl(
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
            player.prepareAsync()
        } catch (e: Exception) {
            completion(false)
            e.printStackTrace()
        }
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)
    }
}