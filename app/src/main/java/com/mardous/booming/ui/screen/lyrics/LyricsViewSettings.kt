package com.mardous.booming.ui.screen.lyrics

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Immutable
class LyricsViewSettings(
    val mode: Mode,
    val isCenterCurrentLine: Boolean,
    val enableSyllableLyrics: Boolean,
    val progressiveColoring: Boolean,
    val backgroundEffect: BackgroundEffect,
    val blurEffect: Boolean,
    val shadowEffect: Boolean,
    val syncedStyle: TextStyle,
    val unsyncedStyle: TextStyle
) {

    val contentPadding: PaddingValues = when (mode) {
        Mode.Full -> PaddingValues(vertical = 96.dp, horizontal = 16.dp)
        Mode.Player -> PaddingValues(vertical = 72.dp, horizontal = 8.dp)
    }

    enum class Mode {
        Player, Full;

        val isFull get() = this == Full
    }

    enum class BackgroundEffect(val isGradient: Boolean = false, val isCanvas: Boolean = false) {
        Gradient(true, false),
        Canvas(false, true),
        None;

        val isNone get() = this == None
    }

    interface Key {
        companion object {
            const val ENABLE_SYLLABLE_LYRICS = "enable_syllable_lyrics"
            const val USE_CUSTOM_FONT = "lyrics_use_custom_font"
            const val SELECTED_CUSTOM_FONT = "lyrics_custom_font"
            const val CENTER_CURRENT_LINE = "lyrics_center_current_line"
            const val LINE_SPACING = "lyrics_line_spacing"
            const val PROGRESSIVE_COLORING = "lyrics_progressive_coloring"
            const val BACKGROUND_EFFECT = "lyrics_background_effect"
            const val BLUR_EFFECT = "lyrics_text_blur"
            const val SHADOW_EFFECT = "lyrics_text_shadow"
            const val UNSYNCED_BOLD_FONT = "unsynced_lyrics_bold_font"
            const val SYNCED_FONT_SIZE_PLAYER = "synced_lyrics_font_size_player"
            const val UNSYNCED_FONT_SIZE_PLAYER = "unsynced_lyrics_font_size_player"
            const val SYNCED_FONT_SIZE_FULL = "synced_lyrics_font_size_full"
            const val UNSYNCED_FONT_SIZE_FULL = "unsynced_lyrics_font_size_full"
        }
    }
}