package com.mardous.booming.ui.screen.sound

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.core.audio.AudioOutputObserver
import com.mardous.booming.core.audio.SoundSettings
import com.mardous.booming.core.model.equalizer.BalanceLevel
import com.mardous.booming.core.model.equalizer.CrossfadeState
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
    val balance get() = soundSettings.balance

    val tempoFlow = soundSettings.tempoFlow
    val tempo get() = soundSettings.tempo

    val crossfadeFlow = soundSettings.crossfadeFlow
    val crossfade get() = soundSettings.crossfade

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

    fun setCrossfade(
        crossfadeDuration: Int = crossfade.crossfadeDuration,
        audioFadeDuration: Int = crossfade.audioFadeDuration,
        apply: Boolean = true
    ) = viewModelScope.launch(Dispatchers.IO) {
        val newState = CrossfadeState(apply, crossfadeDuration, audioFadeDuration)
        val update = EqEffectUpdate(crossfadeFlow.value, true, newState)
        soundSettings.setCrossfade(update, apply)
    }

    fun applyPendingState() = viewModelScope.launch(Dispatchers.IO) {
        soundSettings.applyPendingState()
    }

    fun showOutputDeviceSelector(context: Context) {
        audioOutputObserver.showOutputDeviceSelector(context)
    }
}