package com.mardous.booming.core.model.equalizer

import androidx.compose.runtime.Immutable
import com.mardous.booming.R

@Immutable
data class EqState(
    val supported: Boolean,
    val enabled: Boolean,
    val disableReason: DisableReason?,
    val preferredBandCount: Int,
    val engineMode: EqEngineMode
) {
    val isDisabledByReason = disableReason != null
    val isUsable = supported && enabled && !isDisabledByReason

    enum class DisableReason(val titleRes: Int) {
        AudioOffload(R.string.audio_offload_is_enabled),
        BitPerfect(R.string.bit_perfect_is_active)
    }

    companion object {
        val Unspecified = EqState(
            supported = false,
            enabled = false,
            disableReason = null,
            preferredBandCount = 0,
            engineMode = EqEngineMode.Auto
        )
    }
}

@Immutable
data class BassBoostState(
    val supported: Boolean,
    val enabled: Boolean,
    val strength: Float,
    val strengthRange: ClosedFloatingPointRange<Float>
) {
    val isUsable = supported && enabled

    companion object {
        val Unspecified = BassBoostState(
            supported = false,
            enabled = false,
            strength = 0f,
            strengthRange = -1f..0f
        )
    }
}

@Immutable
data class VirtualizerState(
    val supported: Boolean,
    val enabled: Boolean,
    val strength: Float,
    val strengthRange: ClosedFloatingPointRange<Float>
) {
    val isUsable = supported && enabled

    companion object {
        val Unspecified = VirtualizerState(
            supported = false,
            enabled = false,
            strength = 0f,
            strengthRange = -1f..0f
        )
    }
}

@Immutable
data class LoudnessGainState(
    val supported: Boolean,
    val enabled: Boolean,
    val gainInDb: Float,
    val gainRange: ClosedFloatingPointRange<Float>
) {
    val isUsable = supported && enabled

    companion object {
        val Unspecified = LoudnessGainState(
            supported = false,
            enabled = false,
            gainInDb = 0f,
            gainRange = -1f..0f
        )
    }
}

@Immutable
data class LimiterState(
    val enabled: Boolean,
    val attackTimeMs: Float,
    val attackTimeRange: ClosedFloatingPointRange<Float>,
    val releaseTimeMs: Float,
    val releaseTimeRange: ClosedFloatingPointRange<Float>,
    val postGain: Float,
    val postGainRange: ClosedFloatingPointRange<Float>,
    val ratio: Float,
    val ratioRange: ClosedFloatingPointRange<Float>,
    val threshold: Float,
    val thresholdRange: ClosedFloatingPointRange<Float>
)

@Immutable
data class CompressorState(
    val enabled: Boolean,
    val attackTimeMs: Float,
    val releaseTimeMs: Float,
    val kneeWidth: Float,
    val noiseGateThreshold: Float,
    val preGain: Float,
    val postGain: Float,
    val ratio: Float,
    val expanderRatio: Float,
    val threshold: Float
)