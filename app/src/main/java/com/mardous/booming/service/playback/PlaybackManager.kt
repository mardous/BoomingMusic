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

import android.content.Context
import android.media.audiofx.AudioEffect
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.mardous.booming.core.audio.SoundSettings
import com.mardous.booming.data.model.Song
import com.mardous.booming.service.equalizer.EqualizerManager
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaybackManager(
    private val context: Context,
    private val equalizerManager: EqualizerManager,
    private val soundSettings: SoundSettings
): Playback {

    private val _progressFlow = MutableStateFlow(-1)
    val progressFlow = _progressFlow.asStateFlow()

    private val _durationFlow = MutableStateFlow(-1)
    val durationFlow = _durationFlow.asStateFlow()

    var pendingQuit = false
    var gaplessPlayback = Preferences.gaplessPlayback

    val isCrossfading: Boolean = false

    private val progressObserver = ProgressObserver(intervalMs = 100)
    private var playback: ExoPlayer? = null

    fun play(onNotInitialized: () -> Unit) {
        if (playback != null && !isPlaying()) {
            if (!isInitialized()) {
                onNotInitialized()
            } else {
                if (playback == null)
                    return

                playback!!.play()
                progressObserver.start { updateProgress() }
                updateBalance()
                updateTempo()
                if (equalizerManager.eqState.isEnabled) {
                    //Shutdown any existing external audio sessions
                    closeAudioEffectSession(false)

                    //Start internal equalizer session (will only turn on if enabled)
                    openAudioEffectSession(true)
                } else {
                    openAudioEffectSession(false)
                }
            }
        }
    }

    override fun isInitialized(): Boolean = playback?.playbackState == Player.STATE_READY

    override fun isPlaying(): Boolean = playback?.isPlaying == true

    override fun start(): Boolean {
        throw RuntimeException("Calling start() directly is not allowed, use play(() -> Unit) instead.")
    }

    override fun pause(): Boolean {
        if (playback != null && isPlaying()) {
            if (playback == null)
                return false

            playback!!.pause()
            progressObserver.stop()
            closeAudioEffectSession(false)
            return true
        }
    }

    override fun stop() {
        playback?.stop()
    }

    override fun position() = playback?.currentPosition ?: -1L

    override fun duration() = playback?.duration ?: -1

    override fun seek(whereto: Long, force: Boolean) {
        playback?.seekTo(whereto)
        updateProgress(progress = whereto)
    }

    override suspend fun setDataSource(song: Song, force: Boolean, completion: (success: Boolean) -> Unit) {
        playback?.setDataSource(song, force, completion)
    }

    override suspend fun setNextDataSource(song: Song?) {
        playback?.setNextDataSource(song)
    }

    override fun setCrossFadeDuration(duration: Int) {
    }

    override fun setReplayGain(replayGain: Float) {
    }

    @OptIn(UnstableApi::class)
    override fun getAudioSessionId(): Int =
        playback?.audioSessionId ?: AudioEffect.ERROR_BAD_VALUE

    @OptIn(UnstableApi::class)
    override fun setAudioSessionId(sessionId: Int): Boolean {
        playback?.audioSessionId = sessionId
    }

    override fun getSpeed(): Float = playback?.playbackParameters?.speed ?: 1f

    override fun setTempo(speed: Float, pitch: Float) {

    }

    override fun setBalance(left: Float, right: Float) {

    }

    override fun setVolume(leftVol: Float, rightVol: Float) {
    }

    override fun release() {
        equalizerManager.release()
        progressObserver.stop()
        playback?.release()
        playback = null
        closeAudioEffectSession(true)
    }

    fun openAudioEffectSession(internal: Boolean) {
        equalizerManager.openAudioEffectSession(getAudioSessionId(), internal)
    }

    fun closeAudioEffectSession(internal: Boolean) {
        equalizerManager.closeAudioEffectSession(getAudioSessionId(), internal)
    }

    fun updateBalance(
        left: Float = soundSettings.balance.left,
        right: Float = soundSettings.balance.right
    ) {
    }

    fun updateTempo(
        speed: Float = soundSettings.tempo.speed,
        pitch: Float = soundSettings.tempo.actualPitch
    ) {
    }

    fun updateCrossfade(crossFadeDuration: Int) {

    }

    private fun updateProgress(progress: Long = position(), duration: Long = duration()) {
        _progressFlow.value = progress
        _durationFlow.value = duration
    }
}