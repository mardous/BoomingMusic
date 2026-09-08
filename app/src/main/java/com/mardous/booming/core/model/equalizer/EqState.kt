package com.mardous.booming.core.model.equalizer

import androidx.compose.runtime.Immutable
import com.mardous.booming.R

@Immutable
data class EqState(
    val supported: Boolean,
    val enabled: Boolean,
    val disableReason: DisableReason?,
    val preferredBandCount: Int,
    val engineMode: EqEngineMode,
    val proMode: Boolean
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
            engineMode = EqEngineMode.Auto,
            proMode = false
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
) {
    companion object {
        val Unspecified = LimiterState(
            enabled = false,
            attackTimeMs = 1f,
            attackTimeRange = 0.1f..1000f,
            releaseTimeMs = 60f,
            releaseTimeRange = 0.1f..1000f,
            postGain = 0f,
            postGainRange = -10f..10f,
            ratio = 10f,
            ratioRange = 1f..50f,
            threshold = -2f,
            thresholdRange = -50f..0f
        )
    }
}

@Immutable
data class CompressorState(
    val enabled: Boolean,
    val attackTimeMs: Float,
    val attackTimeRange: ClosedFloatingPointRange<Float>,
    val releaseTimeMs: Float,
    val releaseTimeRange: ClosedFloatingPointRange<Float>,
    val kneeWidth: Float,
    val kneeWidthRange: ClosedFloatingPointRange<Float>,
    val noiseGateThreshold: Float,
    val noiseGateThresholdRange: ClosedFloatingPointRange<Float>,
    val preGain: Float,
    val preGainRange: ClosedFloatingPointRange<Float>,
    val postGain: Float,
    val postGainRange: ClosedFloatingPointRange<Float>,
    val ratio: Float,
    val ratioRange: ClosedFloatingPointRange<Float>,
    val expanderRatio: Float,
    val expanderRatioRange: ClosedFloatingPointRange<Float>,
    val threshold: Float,
    val thresholdRange: ClosedFloatingPointRange<Float>
) {
    companion object {
        val Unspecified = CompressorState(
            enabled = false,
            attackTimeMs = 3f,
            attackTimeRange = 0.1f..1000f,
            releaseTimeMs = 80f,
            releaseTimeRange = 0.1f..1000f,
            kneeWidth = 0f,
            kneeWidthRange = 0f..50f,
            noiseGateThreshold = -80f,
            noiseGateThresholdRange = -100f..0f,
            preGain = 0f,
            preGainRange = -10f..10f,
            postGain = 0f,
            postGainRange = -10f..10f,
            ratio = 2f,
            ratioRange = 1f..50f,
            expanderRatio = 1f,
            expanderRatioRange = 1f..50f,
            threshold = -12f,
            thresholdRange = -50f..0f
        )
    }
}