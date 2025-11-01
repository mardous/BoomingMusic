package com.mardous.booming.data.model.replaygain

enum class ReplayGainMode {
    Album, Track, Off;

    val isOn get() = this == Album || this == Track
}

data class ReplayGain(
    val albumGain: Float,
    val trackGain: Float,
    val albumPeak: Float,
    val trackPeak: Float
) {
    companion object {
        val Empty = ReplayGain(0f, 0f, 1f, 1f)
    }
}