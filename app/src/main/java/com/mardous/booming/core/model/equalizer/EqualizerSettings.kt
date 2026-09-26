package com.mardous.booming.core.model.equalizer

import androidx.compose.runtime.Immutable
import com.mardous.booming.core.model.equalizer.autoeq.AutoEqProfile

@Immutable
data class EqualizerSettings(
    val eqState: EqState,
    val currentProfile: EqProfile,
    val customProfile: EqProfile,
    val profiles: List<EqProfile>,
    val autoEqProfiles: List<AutoEqProfile>,
    val bandCapabilities: EqBandCapabilities,
    val bassBoost: BassBoostState,
    val virtualizer: VirtualizerState,
    val loudnessGain: LoudnessGainState,
    val compressor: CompressorState,
    val limiter: LimiterState,
) {
    companion object {
        val Unspecified = EqualizerSettings(
            eqState = EqState.Unspecified,
            currentProfile = EqProfile(EqProfile.CUSTOM_PROFILE_NAME, FloatArray(0), isCustom = true),
            customProfile = EqProfile(EqProfile.CUSTOM_PROFILE_NAME, FloatArray(0), isCustom = true),
            profiles = emptyList(),
            autoEqProfiles = emptyList(),
            bandCapabilities = EqBandCapabilities.Empty,
            bassBoost = BassBoostState.Unspecified,
            virtualizer = VirtualizerState.Unspecified,
            loudnessGain = LoudnessGainState.Unspecified,
            compressor = CompressorState.Unspecified,
            limiter = LimiterState.Unspecified,
        )
    }
}
