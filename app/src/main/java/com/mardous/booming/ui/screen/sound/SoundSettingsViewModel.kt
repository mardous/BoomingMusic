package com.mardous.booming.ui.screen.sound

import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.core.audio.AudioOutputObserver
import com.mardous.booming.core.audio.SoundSettings
import com.mardous.booming.core.model.equalizer.BalanceLevel
import com.mardous.booming.core.model.equalizer.EqEffectUpdate
import com.mardous.booming.core.model.equalizer.TempoLevel
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

    val balance get() = soundSettings.balance
    val minBalance: Float get() = soundSettings.minBalance
    val maxBalance: Float get() = soundSettings.maxBalance

    val tempo get() = soundSettings.tempo
    val maxSpeed: Float get() = soundSettings.maxSpeed
    val maxPitch: Float get() = soundSettings.maxPitch
    val minSpeed: Float get() = soundSettings.minSpeed
    val minPitch: Float get() = soundSettings.minPitch
    val defaultSpeed: Float get() = soundSettings.defaultSpeed
    val defaultPitch: Float get() = soundSettings.defaultPitch

    init {
        audioOutputObserver.startObserver()
    }

    override fun onCleared() {
        super.onCleared()
        audioOutputObserver.stopObserver()
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

    fun applyPendingState() = viewModelScope.launch(Dispatchers.IO) {
        soundSettings.applyPendingState()
    }
}