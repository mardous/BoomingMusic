package com.mardous.booming.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.FloatRange
import androidx.core.net.toUri
import com.mardous.booming.service.playback.Playback
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

abstract class LocalPlayback(val context: Context) : Playback, MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

    private val mutex = Mutex()
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

    protected fun updateVolume() {
        var leftVol = mBalance[0]
        var rightVol = mBalance[1]

        if (!mReplayGain.isNaN()) {
            // Convert dB to linear gain
            val gain = 10.0f.pow(mReplayGain / 20.0f)
            val volume = max(0.0f, min(1.0f, gain))
            leftVol *= volume
            rightVol *= volume
        }

        setVolume(leftVol, rightVol)
    }


    /**
     * @param player The [MediaPlayer] to use
     * @param path The path of the file, or the http/rtsp URL of the stream you want to play
     * @return True if the <code>player</code> has been prepared and is ready to play, false otherwise
     */
    protected suspend fun setDataSourceImpl(
        player: MediaPlayer,
        path: String,
        completion: suspend (success: Boolean) -> Unit,
    ) = withContext(IO) {
        mutex.withLock {
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
                player.prepare()
                completion(true)
            } catch (e: Exception) {
                completion(false)
                e.printStackTrace()
            }
            player.setOnCompletionListener(this@LocalPlayback)
            player.setOnErrorListener(this@LocalPlayback)
        }
    }
}