package com.mardous.booming.core.model.equalizer

import androidx.compose.runtime.Immutable

@Immutable
data class ViperBassState(
    val dynBassEnabled: Boolean = false,
    val dynBassPreset: Int = 0,
    val dynBassGainPct: Float = 33f,
    val viperBassEnabled: Boolean = false,
    val viperBassMode: Int = 0,
    val viperBassCutoffHz: Float = 60f,
    val viperBassGainDb: Float = 0.0f
) {
    companion object {
        val Unspecified = ViperBassState()
    }
}
