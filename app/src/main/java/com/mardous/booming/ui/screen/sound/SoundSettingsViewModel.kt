package com.mardous.booming.ui.screen.sound

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.core.audio.AudioOutputObserver
import com.mardous.booming.core.audio.SoundSettings
import com.mardous.booming.core.model.equalizer.BalanceLevel
import com.mardous.booming.core.model.equalizer.EqEffectUpdate
import com.mardous.booming.core.model.equalizer.ReplayGainState
import com.mardous.booming.core.model.equalizer.TempoLevel
import com.mardous.booming.data.local.ReplayGainMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundSettingsViewModel(
    private val audioOutputObserver: AudioOutputObserver,
    private val soundSettings: SoundSettings
) : ViewModel() {

    val volumeStateFlow get() = audioOutputObserver.volumeStateFlow
    val audioDeviceFlow get() = audioOutputObserver.audioDeviceFlow

    val balanceFlow = soundSettings.balanceFlow
    val balance get() = soundSettings.balance

    val tempoFlow = soundSettings.tempoFlow
    val tempo get() = soundSettings.tempo

    val replayGainStateFlow = soundSettings.replayGainStateFlow
    val replayGainState get() = soundSettings.replayGainState

    val audioFloatOutputFlow = soundSettings.audioFloatOutputFlow
    val skipSilenceFlow = soundSettings.skipSilenceFlow

    init {
        audioOutputObserver.startObserver()
    }

    override fun onCleared() {
        super.onCleared()
        audioOutputObserver.stopObserver()
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
        right: Float = balance.right,
        left: Float = balance.left,
        apply: Boolean = true
    ) = viewModelScope.launch(Dispatchers.IO) {
        val update = EqEffectUpdate(balanceFlow.value, true, BalanceLevel(left, right))
        soundSettings.setBalance(update, apply)
    }

    fun setTempo(
        speed: Float = tempo.speed,
        pitch: Float = tempo.pitch,
        isFixedPitch: Boolean = tempo.isFixedPitch,
        apply: Boolean = true
    ) = viewModelScope.launch(Dispatchers.IO) {
        val update = EqEffectUpdate(tempoFlow.value, true, TempoLevel(speed, pitch, isFixedPitch))
        soundSettings.setTempo(update, apply)
    }

    fun setReplayGain(
        mode: ReplayGainMode = replayGainState.mode,
        preamp: Float = replayGainState.preamp,
        preampWithoutGain: Float = replayGainState.preampWithoutGain,
        apply: Boolean = true
    ) = viewModelScope.launch(Dispatchers.IO) {
        val update = EqEffectUpdate(
            state = replayGainStateFlow.value,
            isEnabled = mode.isOn,
            value = ReplayGainState(
                mode = mode,
                preamp = preamp,
                preampWithoutGain = preampWithoutGain
            )
        )
        soundSettings.setReplayGain(update, apply)
    }

    fun applyPendingState() = viewModelScope.launch(Dispatchers.IO) {
        soundSettings.applyPendingState()
    }

    fun showOutputDeviceSelector(context: Context) {
        audioOutputObserver.showOutputDeviceSelector(context)
    }
}