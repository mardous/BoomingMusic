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
import android.media.AudioDeviceInfo
import android.media.audiofx.AudioEffect
import android.os.Build
import androidx.annotation.RequiresApi
import com.mardous.booming.core.audio.SoundSettings
import com.mardous.booming.data.model.Song
import com.mardous.booming.service.AudioFader
import com.mardous.booming.service.CrossFadePlayer
import com.mardous.booming.service.MultiPlayer
import com.mardous.booming.service.equalizer.EqualizerManager
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main wrapper that implements [Playback] and manages the actual playback backend
 * (such as [MultiPlayer] or [CrossFadePlayer]) based on user settings.
 *
 * `PlaybackManager` centralizes playback control (play, pause, seek, etc.), applies
 * audio settings like tempo, balance, and ReplayGain, manages audio effect sessions
 * (e.g., equalizer), and exposes progress/duration flows for external observation.
 *
 * This class **does not implement audio playback directly**, but delegates everything
 * to an internal [Playback] instance that can be dynamically swapped as needed.
 *
 * ## Features:
 * - Observes and applies changes in [SoundSettings] automatically.
 * - Supports playback control with or without fading (via [AudioFader]).
 * - Can switch between [MultiPlayer] and [CrossFadePlayer] on the fly with [maybeSwitchToCrossFade].
 * - Exposes [progressFlow] and [durationFlow] for UI/state synchronization.
 * - Prevents misuse of low-level methods like [start] and [pause]; prefers high-level controls.
 *
 * ## Typical Usage:
 * 1. Call [initialize] with required callbacks and coroutine scope.
 * 2. Use [play] and [pause] to control playback.
 * 3. Observe [progressFlow] and [durationFlow] in the UI layer.
 *
 * ## Warning:
 * Do **not** call [start] or [pause] directly. Use [play(boolean, () -> Unit)] and [pause(boolean)] instead.
 * Calling internal methods manually may throw exceptions if misused.
 *
 * @param context Application context.
 * @param equalizerManager The equalizer/session manager.
 * @param soundSettings Flows for audio configuration (tempo, balance, etc.).
 */
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

    val isCrossfading: Boolean
        get() = (playback as? CrossFadePlayer)?.isCrossFading == true

    private val progressObserver = ProgressObserver(intervalMs = 100)
    private var playback: Playback? = null

    private val crossFadeDuration: Int
        get() = Preferences.crossFadeDuration

    private val audioFadeDuration: Int
        get() = Preferences.audioFadeDuration

    fun initialize(callbacks: Playback.PlaybackCallbacks, coroutineScope: CoroutineScope) {
        playback = createLocalPlayback()
        setCallbacks(callbacks)
        coroutineScope.launch {
            soundSettings.balanceFlow.collect { balance ->
                updateBalance(balance.value.left, balance.value.right)
            }
        }
        coroutineScope.launch {
            soundSettings.tempoFlow.collect { tempo ->
                updateTempo(tempo.value.speed, tempo.value.actualPitch)
            }
        }
    }

    fun play(force: Boolean = false, onNotInitialized: () -> Unit) {
        if (playback != null && !isPlaying()) {
            if (!isInitialized()) {
                onNotInitialized()
            } else {
                startInternal()
                if (!force) {
                    if (playback is CrossFadePlayer) {
                        if (!(playback as CrossFadePlayer).isCrossFading) {
                            AudioFader.startFadeAnimator(
                                playback = playback!!,
                                fadeDuration = audioFadeDuration,
                                balanceLeft = soundSettings.balance.left,
                                balanceRight = soundSettings.balance.right,
                                fadeIn = true
                            )
                        }
                    } else {
                        AudioFader.startFadeAnimator(
                            playback = playback!!,
                            fadeDuration = audioFadeDuration,
                            balanceLeft = soundSettings.balance.left,
                            balanceRight = soundSettings.balance.right,
                            fadeIn = true
                        )
                    }
                }
            }
        }
    }

    fun pause(force: Boolean) {
        if (playback != null && isPlaying()) {
            if (force) {
                pauseInternal()
            } else {
                AudioFader.startFadeAnimator(
                    playback = playback!!,
                    fadeDuration = audioFadeDuration,
                    balanceLeft = soundSettings.balance.left,
                    balanceRight = soundSettings.balance.right,
                    fadeIn = false
                ) {
                    pauseInternal()
                }
            }
        }
    }

    suspend fun setCrossFadeNextDataSource(song: Song) {
        if (playback is CrossFadePlayer) {
            playback?.setNextDataSource(song)
        }
    }

    override fun isInitialized(): Boolean = playback?.isInitialized() == true

    override fun isPlaying(): Boolean = playback?.isPlaying() == true

    override fun start(): Boolean {
        throw RuntimeException("Calling start() directly is not allowed, use play(boolean, () -> Unit) instead.")
    }

    private fun startInternal(): Boolean {
        if (playback == null)
            return false

        val started = playback!!.start()
        if (started) {
            getCallbacks()?.onPlayStateChanged()
        }
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
        return started
    }

    override fun pause(): Boolean {
        throw RuntimeException("Calling pause() directly is not allowed, use pause(boolean) instead.")
    }

    private fun pauseInternal(): Boolean {
        if (playback == null)
            return false

        val paused = playback!!.pause()
        if (paused) {
            getCallbacks()?.onPlayStateChanged()
        }
        progressObserver.stop()
        closeAudioEffectSession(false)
        return paused
    }

    override fun stop() {
        playback?.stop()
    }

    override fun position() = playback?.position() ?: -1

    override fun duration(): Int = playback?.duration() ?: -1

    override fun seek(whereto: Int, force: Boolean) {
        playback?.seek(whereto, force)
        updateProgress(progress = whereto)
    }

    override suspend fun setDataSource(song: Song, force: Boolean, completion: (success: Boolean) -> Unit) {
        playback?.setDataSource(song, force, completion)
    }

    override suspend fun setNextDataSource(song: Song?) {
        playback?.setNextDataSource(song)
    }

    override fun setCrossFadeDuration(duration: Int) {
        playback?.setCrossFadeDuration(duration)
    }

    override fun setReplayGain(replayGain: Float) {
        playback?.setReplayGain(replayGain)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getRoutedDevice(): AudioDeviceInfo? = playback?.getRoutedDevice()

    override fun getCallbacks(): Playback.PlaybackCallbacks? {
        return playback?.getCallbacks()
    }

    override fun setCallbacks(callbacks: Playback.PlaybackCallbacks) {
        playback?.setCallbacks(callbacks)
    }

    override fun getAudioSessionId(): Int = playback?.getAudioSessionId() ?: AudioEffect.ERROR_BAD_VALUE

    override fun setAudioSessionId(sessionId: Int): Boolean {
        return playback?.setAudioSessionId(sessionId) ?: false
    }

    override fun getSpeed(): Float = playback?.getSpeed() ?: 1f

    override fun setTempo(speed: Float, pitch: Float) {
        playback?.setTempo(speed, pitch)
    }

    override fun setBalance(left: Float, right: Float) {
        playback?.setBalance(left, right)
    }

    override fun setVolume(leftVol: Float, rightVol: Float) {
        playback?.setVolume(leftVol, rightVol)
    }

    override fun release() {
        equalizerManager.release()
        progressObserver.stop()
        playback?.release()
        playback = null
        closeAudioEffectSession(true)
    }

    fun maybeSwitchToCrossFade(crossFadeDuration: Int): Boolean {
        /* Switch to MultiPlayer if CrossFade duration is 0 and
           Playback is not an instance of MultiPlayer
        */
        if (playback !is MultiPlayer && crossFadeDuration == 0) {
            if (playback != null) {
                playback?.release()
            }
            playback = null
            playback = MultiPlayer(context)
            return true
        } else if (playback !is CrossFadePlayer && crossFadeDuration > 0) {
            if (playback != null) {
                playback?.release()
            }
            playback = null
            playback = CrossFadePlayer(context)
            playback?.setCrossFadeDuration(crossFadeDuration)
            return true
        }
        return false
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
        playback?.setBalance(left, right)
    }

    fun updateTempo(
        speed: Float = soundSettings.tempo.speed,
        pitch: Float = soundSettings.tempo.actualPitch
    ) {
        playback?.setTempo(speed, pitch)
    }

    private fun updateProgress(progress: Int = position(), duration: Int = duration()) {
        _progressFlow.value = progress
        _durationFlow.value = duration
    }

    private fun createLocalPlayback(): Playback {
        // Set MultiPlayer when crossfade duration is 0 i.e. off
        return if (crossFadeDuration == 0) {
            MultiPlayer(context)
        } else {
            CrossFadePlayer(context)
        }
    }
}