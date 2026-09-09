/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.mardous.booming.data.remote.lyrics.api

import com.mardous.booming.data.model.lyrics.RawLyrics

enum class LyricsResultQuality(val rank: Int) {
    Plain(1),
    LineSynced(2),
    WordSynced(3)
}

enum class LyricsResultConfidence {
    Low,
    Medium,
    High
}

data class LyricsApiResult(
    val lyrics: RawLyrics.Remote,
    val quality: LyricsResultQuality,
    val confidence: LyricsResultConfidence? = null
)
