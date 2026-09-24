/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.mardous.booming.ui.screen.lyrics

import androidx.compose.runtime.Immutable
import com.mardous.booming.data.remote.lyrics.LyricsProviderSearchResult
import com.mardous.booming.data.remote.lyrics.api.LyricsProvider

enum class LyricsSearchSheetStage {
    Hidden,
    Docked,
    Providers,
    Results,
    SourceChip
}

@Immutable
data class LyricsSearchUiState(
    val songId: Long = -1,
    val isRunning: Boolean = false,
    val providerResults: List<LyricsProviderSearchResult> = emptyList(),
    val sheetStage: LyricsSearchSheetStage = LyricsSearchSheetStage.Hidden,
    val isUserInteracting: Boolean = false,
    val selectedProvider: LyricsProvider? = null
) {
    val completedProviderCount: Int
        get() = providerResults.count { it.isTerminal }

    val usableResults: List<LyricsProviderSearchResult>
        get() = providerResults.filter { it.isUsable }

    val recommendedResult: LyricsProviderSearchResult?
        get() = usableResults.maxByOrNull { it.qualityScore }

    val selectedResult: LyricsProviderSearchResult?
        get() = selectedProvider?.let { provider ->
            usableResults.firstOrNull { it.provider == provider }
        }

    val activeResult: LyricsProviderSearchResult?
        get() = selectedResult ?: recommendedResult

    val hasUsableResult: Boolean
        get() = activeResult != null

    companion object {
        val Idle = LyricsSearchUiState()
    }
}
