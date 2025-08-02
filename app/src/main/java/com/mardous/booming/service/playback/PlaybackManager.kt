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
import com.mardous.booming.audio.SoundSettings
import com.mardous.booming.model.Song
import com.mardous.booming.service.AudioFader
import com.mardous.booming.service.CrossFadePlayer
import com.mardous.booming.service.MultiPlayer
import com.mardous.booming.service.equalizer.EqualizerManager
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaybackManager(
    private val context: Context,
    private val equalizerManager: EqualizerManager,
    private val soundSettings: SoundSettings
) {

    private val _progressFlow = MutableStateFlow(-1)
    val progressFlow = _progressFlow.asStateFlow()

    private val _durationFlow = MutableStateFlow(-1)
    val durationFlow = _durationFlow.asStateFlow()

    var pendingQuit = false
    var gaplessPlayback = Preferences.gaplessPlayback

    private var playback: Playback? = null
    private val equalizerEnabled: Boolean
        get() = equalizerManager.eqState.isUsable

    private val crossFadeDuration: Int
        get() = Preferences.crossFadeDuration

    private val audioFadeDuration: Int
        get() = Preferences.audioFadeDuration

    private var progressObserver: Job? = null
    val isProgressObserverRunning: Boolean
        get() = progressObserver?.isActive == true

    private fun createLocalPlayback(): Playback {
        // Set MultiPlayer when crossfade duration is 0 i.e. off
        return if (crossFadeDuration == 0) {
            MultiPlayer(context)
        } else {
            CrossFadePlayer(context)
        }
    }

    fun initialize(callbacks: Playback.PlaybackCallbacks, coroutineScope: CoroutineScope) {
        playback = createLocalPlayback()
        playback?.setCallbacks(callbacks)
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

    fun getAudioSessionId(): Int = playback?.getAudioSessionId() ?: AudioEffect.ERROR_BAD_VALUE

    fun getPlaybackSpeed(): Float = playback?.getSpeed() ?: 1f

    @RequiresApi(Build.VERSION_CODES.P)
    fun getRoutedDevice(): AudioDeviceInfo? = playback?.getRoutedDevice()

    fun isPlaying(): Boolean = playback?.isPlaying() == true

    fun play(force: Boolean = false, coroutineScope: CoroutineScope, onNotInitialized: () -> Unit) {
        if (playback != null && !playback!!.isPlaying()) {
            if (!playback!!.isInitialized()) {
                onNotInitialized()
            } else {
                playback?.start()
                playback?.getCallbacks()?.onPlayStateChanged()

                updateBalance()
                updateTempo()

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

                if (!isProgressObserverRunning) {
                    startProgressObserver(coroutineScope)
                }

                if (equalizerEnabled) {
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

    fun pause(force: Boolean) {
        if (playback != null && playback!!.isPlaying()) {
            if (force) {
                playback?.pause()
                playback?.getCallbacks()?.onPlayStateChanged()
                stopProgressObserver()
                closeAudioEffectSession(false)
            } else {
                AudioFader.startFadeAnimator(
                    playback = playback!!,
                    fadeDuration = audioFadeDuration,
                    balanceLeft = soundSettings.balance.left,
                    balanceRight = soundSettings.balance.right,
                    fadeIn = false
                ) {
                    playback?.pause()
                    playback?.getCallbacks()?.onPlayStateChanged()
                    stopProgressObserver()
                    closeAudioEffectSession(false)
                }
            }
        }
    }

    fun position() = playback?.position() ?: -1

    fun duration(): Int = playback?.duration() ?: -1

    fun seek(millis: Int, force: Boolean) {
        if (!isProgressObserverRunning) {
            triggerProgressUpdate(millis, duration())
        }
        playback?.seek(millis, force)
    }

    fun setCallbacks(callbacks: Playback.PlaybackCallbacks) {
        playback?.setCallbacks(callbacks)
    }

    fun setDataSource(song: Song, force: Boolean, completion: (success: Boolean) -> Unit) {
        playback?.setDataSource(song, force, completion)
    }

    fun setNextDataSource(song: Song?) {
        playback?.setNextDataSource(song)
    }

    fun setCrossFadeDuration(duration: Int) {
        playback?.setCrossFadeDuration(duration)
    }

    fun setCrossFadeNextDataSource(song: Song) {
        if (playback is CrossFadePlayer) {
            playback?.setNextDataSource(song)
        }
    }

    fun setReplayGain(rg: Float) {
        playback?.setReplayGain(rg)
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

    fun release() {
        progressObserver?.cancel()
        progressObserver = null
        equalizerManager.release()
        playback?.release()
        playback = null
        closeAudioEffectSession(true)
    }

    private fun startProgressObserver(coroutineScope: CoroutineScope) {
        progressObserver = coroutineScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (isPlaying()) {
                    triggerProgressUpdate(position(), duration())
                    playback?.setProgressState(progressFlow.value, durationFlow.value)
                    delay(50L)
                } else {
                    delay(300L)
                }
            }
        }
    }

    private fun triggerProgressUpdate(progress: Int, duration: Int) {
        _progressFlow.value = progress
        _durationFlow.value = duration
    }

    private fun stopProgressObserver() {
        progressObserver?.cancel()
        progressObserver = null
    }
}