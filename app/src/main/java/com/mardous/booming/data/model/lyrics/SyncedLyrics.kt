package com.mardous.booming.data.model.lyrics

import androidx.compose.runtime.Immutable

@Immutable
data class SyncedLyrics(
    val lines: List<Line>,
    val offset: Long = 0,
    val provider: String? = null
) {
    val hasContent = lines.isNotEmpty()

    init {
        for (line in lines) {
            require(line.start >= 0) { "startAt in the LyricsLine must >= 0" }
            require(line.duration >= 0) { "durationMillis in the LyricsLine >= 0" }
        }
    }

    @Immutable
    data class Line(
        val start: Long,
        val end: Long,
        val duration: Long = (end - start),
        val content: TextContent,
        val transliteration: TextContent?,
        val translation: TextContent?,
        val actor: LyricsActor?
    ) {
        val id: Long = 31 * (31 * start + duration) + content.hashCode()

        val isEmpty = content.isEmpty

        val isWordSynced = content.isWordSynced

        val hasBackgroundVocals = content.hasBackgroundSyllables
    }

    @Immutable
    data class Word(
        val content: String,
        val start: Long,
        val startIndex: Int,
        val end: Long,
        val endIndex: Int,
        val duration: Long,
        val actor: LyricsActor?
    ) {
        val isBackground = actor?.isBackground == true
    }

    @Immutable
    data class TextContent(
        val content: String,
        val backgroundContent: String?,
        val rawContent: String?,
        val syllables: List<Word>
    ) {
        val isEmpty = content.isBlank()

        val isWordSynced = syllables.isNotEmpty()

        val mainSyllables = syllables.filterNot { it.isBackground }

        val backgroundSyllables = syllables.filter { it.isBackground }

        val hasBackgroundSyllables = backgroundSyllables.isNotEmpty() && !backgroundContent.isNullOrBlank()

        fun getSyllables(background: Boolean) = if (background) backgroundSyllables else mainSyllables

        fun getText(background: Boolean) = if (background) backgroundContent.orEmpty() else content
    }

    companion object {
        const val MIN_OFFSET_TIME = 3500

        val EmptyContent = TextContent("", null, null, emptyList())
    }
}