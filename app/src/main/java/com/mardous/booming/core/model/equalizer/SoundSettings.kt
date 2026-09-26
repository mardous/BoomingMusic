package com.mardous.booming.core.model.equalizer

import androidx.compose.runtime.Immutable

@Immutable
data class SoundSettings(
    val volume: VolumeState,
    val balance: BalanceState,
    val tempo: TempoState,
    val replayGain: ReplayGainState,
    val bitPerfect: Boolean,
    val audioOffload: Boolean,
    val audioFloatOutput: Boolean,
    val skipSilence: Boolean,
) {
    companion object {
        val Unspecified = SoundSettings(
            volume = VolumeState.Unspecified,
            balance = BalanceState.Unspecified,
            tempo = TempoState.Unspecified,
            replayGain = ReplayGainState.Unspecified,
            bitPerfect = false,
            audioOffload = false,
            audioFloatOutput = false,
            skipSilence = false,
        )
    }
}
