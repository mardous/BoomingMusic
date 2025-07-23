package com.mardous.booming.service

import android.animation.Animator
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.PowerManager
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.mardous.booming.R
import com.mardous.booming.extensions.execSafe
import com.mardous.booming.extensions.showToast
import com.mardous.booming.model.Song
import com.mardous.booming.service.AudioFader.Companion.createFadeAnimator
import com.mardous.booming.util.Preferences

/** @author Prathamesh M */

/*
* To make Crossfade work we need two MediaPlayer's
* Basically, we switch back and forth between those two mp's
* e.g. When song is about to end (Reaches Crossfade duration) we let current mediaplayer
* play but with decreasing volume and start the player with the next song with increasing volume
* and vice versa for upcoming song and so on.
*/
class CrossFadePlayer(context: Context) : LocalPlayback(context) {

    private var currentPlayer: CurrentPlayer = CurrentPlayer.NOT_SET
    private var player1 = MediaPlayer()
    private var player2 = MediaPlayer()

    private var crossFadeAnimator: Animator? = null

    private var mIsInitialized = false
    private var hasDataSource: Boolean = false /* Whether first player has DataSource */
    private var nextDataSource: String? = null

    private var albumDataSource: AlbumDataSource? = null

    private var crossFadeDuration = 0
    private var observeProgress = true
    var isCrossFading = false

    init {
        player1.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        player2.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        currentPlayer = CurrentPlayer.PLAYER_ONE
        setCrossFadeDuration(Preferences.crossFadeDuration)
    }

    override fun isInitialized(): Boolean {
        return mIsInitialized
    }

    override fun isPlaying(): Boolean {
        return mIsInitialized && getCurrentPlayer()?.isPlaying == true
    }

    override fun start(): Boolean {
        resumeFade()
        return try {
            getCurrentPlayer()?.start()
            if (isCrossFading) {
                getNextPlayer()?.start()
            }
            true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }

    override fun release() {
        stop()
        cancelFade()
        getCurrentPlayer()?.release()
        getNextPlayer()?.release()
    }

    override fun stop() {
        getCurrentPlayer()?.reset()
        mIsInitialized = false
    }

    override fun pause(): Boolean {
        pauseFade()
        getCurrentPlayer()?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        getNextPlayer()?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        return true
    }

    override fun seek(whereto: Int, force: Boolean) {
        if (force) {
            endFade()
        }
        getNextPlayer()?.stop()
        try {
            getCurrentPlayer()?.seekTo(whereto)
        } catch (e: java.lang.IllegalStateException) {
            e.printStackTrace()
        }
    }

    override fun setDataSource(song: Song, force: Boolean, completion: (Boolean) -> Unit) {
        if (force) hasDataSource = false
        mIsInitialized = false
        /* We've already set DataSource if initialized is true in setNextDataSource */
        if (!hasDataSource) {
            getCurrentPlayer()?.let {
                setDataSourceImpl(it, song.mediaStoreUri.toString()) { success ->
                    mIsInitialized = success
                    if (mIsInitialized) {
                        albumDataSource = AlbumDataSource(song.albumId, song.trackNumber)
                    }
                    completion(success)
                }
            }
            hasDataSource = true
        } else {
            completion(true)
            mIsInitialized = true
        }
    }

    override fun setNextDataSource(song: Song?) {
        nextDataSource = if (song != null && albumDataSource?.avoidCrossfade(song) == true) {
            null
        } else {
            song?.mediaStoreUri?.toString()
        }
    }

    override fun getAudioSessionId(): Int {
        return getCurrentPlayer()?.audioSessionId ?: AudioEffect.ERROR_BAD_VALUE
    }

    override fun setAudioSessionId(sessionId: Int): Boolean {
        return try {
            getCurrentPlayer()?.audioSessionId = sessionId
            true
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Gets the duration of the file.
     *
     * @return The duration in milliseconds
     */
    override fun duration(): Int {
        return if (!mIsInitialized) {
            -1
        } else try {
            getCurrentPlayer()?.duration!!
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * Gets the current position in audio.
     * @return The position in milliseconds
     */
    override fun position(): Int {
        return if (!mIsInitialized) {
            -1
        } else try {
            getCurrentPlayer()?.currentPosition!!
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            -1
        }
    }

    override fun getSpeed(): Float {
        return getCurrentPlayer()?.execSafe { this.playbackParams.speed } ?: 1f
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getRoutedDevice(): AudioDeviceInfo? {
        if (isPlaying()) {
            return getCurrentPlayer()?.execSafe { this.routedDevice }
        }
        return null
    }

    override fun setBalance(
        @FloatRange(from = 0.0, to = 1.0) left: Float,
        @FloatRange(from = 0.0, to = 1.0) right: Float
    ) {
        super.setBalance(left, right)
        if (!isCrossFading) {
            getCurrentPlayer()?.let { updateVolume(it) }
        }
    }

    override fun setReplayGain(replayGain: Float) {
        super.setReplayGain(replayGain)
        if (!isCrossFading) {
            getCurrentPlayer()?.let { updateVolume(it) }
        }
    }

    override fun setTempo(speed: Float, pitch: Float) {
        getCurrentPlayer()?.setPlaybackSpeedPitch(speed, pitch)
        if (getNextPlayer()?.isPlaying == true) {
            getNextPlayer()?.setPlaybackSpeedPitch(speed, pitch)
        }
    }

    override fun setVolume(leftVol: Float, rightVol: Float) {
        cancelFade()
        getCurrentPlayer()?.execSafe {
            setVolume(leftVol, rightVol)
        }
    }

    override fun setCrossFadeDuration(duration: Int) {
        crossFadeDuration = duration
    }

    override fun setProgressState(progress: Int, duration: Int) {
        if (!mIsInitialized || !observeProgress)
            return

        if (progress > 0 && (duration - progress).div(1000) == crossFadeDuration) {
            getNextPlayer()?.let { player ->
                nextDataSource?.let {
                    setDataSourceImpl(player, it.toUri().toString()) { success ->
                        if (success) switchPlayer()
                    }
                }
            }
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (mp == getCurrentPlayer()) {
            mCallbacks?.onTrackEnded()
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mIsInitialized = false
        mp?.release()
        player1 = MediaPlayer()
        player2 = MediaPlayer()
        mIsInitialized = true
        mp?.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        context.showToast(R.string.unplayable_file)
        return false
    }

    private fun getCurrentPlayer(): MediaPlayer? {
        return when (currentPlayer) {
            CurrentPlayer.PLAYER_ONE -> {
                player1
            }
            CurrentPlayer.PLAYER_TWO -> {
                player2
            }
            CurrentPlayer.NOT_SET -> {
                null
            }
        }
    }

    private fun getNextPlayer(): MediaPlayer? {
        return when (currentPlayer) {
            CurrentPlayer.PLAYER_ONE -> {
                player2
            }
            CurrentPlayer.PLAYER_TWO -> {
                player1
            }
            CurrentPlayer.NOT_SET -> {
                null
            }
        }
    }

    private fun crossFade(fadeInMp: MediaPlayer, fadeOutMp: MediaPlayer) {
        observeProgress = false
        isCrossFading = true
        crossFadeAnimator = createFadeAnimator(context, fadeInMp, fadeOutMp, crossFadeDuration, mBalance) {
            crossFadeAnimator = null
            observeProgress = true
            isCrossFading = false
        }
        crossFadeAnimator?.start()
    }

    private fun endFade() {
        crossFadeAnimator?.end()
        crossFadeAnimator = null
    }

    private fun cancelFade() {
        crossFadeAnimator?.cancel()
        crossFadeAnimator = null
    }

    private fun pauseFade() {
        crossFadeAnimator?.pause()
    }

    private fun resumeFade() {
        if (crossFadeAnimator?.isPaused == true) {
            crossFadeAnimator?.resume()
        }
    }

    enum class CurrentPlayer {
        PLAYER_ONE,
        PLAYER_TWO,
        NOT_SET
    }

    private fun switchPlayer() {
        getNextPlayer()?.start()
        crossFade(getNextPlayer()!!, getCurrentPlayer()!!)
        currentPlayer =
            if (currentPlayer == CurrentPlayer.PLAYER_ONE || currentPlayer == CurrentPlayer.NOT_SET) {
                CurrentPlayer.PLAYER_TWO
            } else {
                CurrentPlayer.PLAYER_ONE
            }
        mCallbacks?.onTrackEndedWithCrossFade()
    }

    private class AlbumDataSource(private val id: Long, private val trackNumber: Int) {
        fun avoidCrossfade(song: Song): Boolean {
            if (Preferences.noCrossFadeOnAlbums) {
                return song.albumId == id && song.trackNumber > trackNumber
            }
            return false
        }
    }

    companion object {
        val TAG: String = CrossFadePlayer::class.java.simpleName
    }
}

fun MediaPlayer.setPlaybackSpeedPitch(speed: Float, pitch: Float) = execSafe {
    val wasPlaying = isPlaying
    playbackParams = PlaybackParams().setSpeed(speed).setPitch(pitch)
    if (!wasPlaying) {
        pause()
    }
}