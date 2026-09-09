/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.mardous.booming.data.remote.lyrics

import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.remote.lyrics.api.LyricsResultConfidence
import com.mardous.booming.data.remote.lyrics.api.LyricsResultQuality
import com.mardous.booming.data.remote.lyrics.api.LyricsProvider

enum class LyricsProviderSearchStatus {
    Waiting,
    Searching,
    Found,
    NoMatch,
    Failed,
    TimedOut
}

data class LyricsProviderSearchResult(
    val provider: LyricsProvider,
    val status: LyricsProviderSearchStatus,
    val lyrics: RawLyrics.Remote? = null,
    val quality: LyricsResultQuality? = null,
    val confidence: LyricsResultConfidence? = null
) {
    val isTerminal: Boolean
        get() = when (status) {
            LyricsProviderSearchStatus.Waiting,
            LyricsProviderSearchStatus.Searching -> false
            else -> true
        }

    val isUsable: Boolean
        get() = status == LyricsProviderSearchStatus.Found &&
            lyrics?.let { it.hasPlain || it.hasSynced } == true

    val qualityScore: Int
        get() = quality?.rank ?: 0
}

data class LyricsSearchResult(
    val lyrics: RawLyrics.Remote,
    val providerResults: List<LyricsProviderSearchResult>
)
