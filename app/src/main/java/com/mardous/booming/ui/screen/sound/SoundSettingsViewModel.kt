package com.mardous.booming.ui.screen.sound

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.core.audio.AudioOutputObserver
import com.mardous.booming.core.audio.SoundSettings
import com.mardous.booming.core.model.equalizer.BalanceLevel
import com.mardous.booming.core.model.equalizer.ReplayGainState
import com.mardous.booming.core.model.equalizer.TempoLevel
import com.mardous.booming.data.model.replaygain.ReplayGainMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundSettingsViewModel(
    private val audioOutputObserver: AudioOutputObserver,
    private val soundSettings: SoundSettings
) : ViewModel() {

    val volumeStateFlow get() = audioOutputObserver.volumeStateFlow
    val audioDeviceFlow get() = audioOutputObserver.audioDeviceFlow

    val balanceFlow = soundSettings.balanceFlow
    val tempoFlow = soundSettings.tempoFlow
    val replayGainStateFlow = soundSettings.replayGainStateFlow

    val audioOffloadFlow = soundSettings.audioOffloadFlow
    val audioFloatOutputFlow = soundSettings.audioFloatOutputFlow
    val skipSilenceFlow = soundSettings.skipSilenceFlow

    init {
        audioOutputObserver.startObserver()
    }

    override fun onCleared() {
        super.onCleared()
        audioOutputObserver.stopObserver()
    }

    fun setEnableAudioOffload(enable: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        soundSettings.setEnableAudioOffload(enable)
    }

    fun setEnableAudioFloatOutput(enable: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        soundSettings.setEnableAudioFloatOutput(enable)
    }

    fun setEnableSkipSilences(enable: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        soundSettings.setEnableSkipSilence(enable)
    }

    fun setVolume(volume: Int) {
        audioOutputObserver.audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    fun setBalance(
        left: Float = balanceFlow.value.left,
        right: Float = balanceFlow.value.right
    ) = viewModelScope.launch(Dispatchers.IO) {
        soundSettings.setBalance(BalanceLevel(left, right))
    }

    fun setTempo(
        speed: Float = tempoFlow.value.speed,
        pitch: Float = tempoFlow.value.pitch,
        isFixedPitch: Boolean = tempoFlow.value.isFixedPitch
    ) = viewModelScope.launch(Dispatchers.IO) {
        soundSettings.setTempo(TempoLevel(speed, pitch, isFixedPitch))
    }

    fun setReplayGain(
        mode: ReplayGainMode = replayGainStateFlow.value.mode,
        preamp: Float = replayGainStateFlow.value.preamp,
        preampWithoutGain: Float = replayGainStateFlow.value.preampWithoutGain
    ) = viewModelScope.launch(Dispatchers.IO) {
        soundSettings.setReplayGain(ReplayGainState(mode, preamp, preampWithoutGain))
    }

    fun applyPendingState() = viewModelScope.launch(Dispatchers.IO) {
    }

    fun showOutputDeviceSelector(context: Context) {
        audioOutputObserver.showOutputDeviceSelector(context)
    }
}