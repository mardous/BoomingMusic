package com.mardous.booming.data.model.lyrics

import androidx.compose.runtime.Immutable

@Immutable
data class Lyrics(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMillis: Long?,
    val lines: List<Line>,
    val offset: Long = 0
) {
    val hasContent = lines.isNotEmpty()

    val plainText: String
        get() = lines.joinToString("\n") { it.content.content }

    val rawText: String
        get() = lines.joinToString("\n") { it.content.rawContent.orEmpty() }.trim()

    init {
        for (line in lines) {
            require(line.startAt >= 0) { "startAt in the LyricsLine must >= 0" }
            require(line.durationMillis >= 0) { "durationMillis in the LyricsLine >= 0" }
        }
    }

    @Immutable
    data class Line(
        val startAt: Long,
        val end: Long,
        val durationMillis: Long = (end - startAt),
        val content: TextContent,
        val translation: TextContent?,
        val actor: LyricsActor?
    ) {
        val id: Long = 31 * (31 * startAt + durationMillis) + content.hashCode()

        val isEmpty = content.isEmpty

        val hasBackgroundVocals = content.hasBackgroundVocals
    }

    @Immutable
    data class Word(
        val content: String,
        val startMillis: Long,
        val startIndex: Int,
        val endMillis: Long,
        val endIndex: Int,
        val durationMillis: Long,
        val actor: LyricsActor?
    ) {
        val isBackground = actor?.isBackground == true
    }

    @Immutable
    data class TextContent(
        val content: String,
        val backgroundContent: String?,
        val rawContent: String?,
        val words: List<Word>
    ) {
        val isEmpty = content.isBlank()

        val mainVocals = words.filterNot { it.isBackground }

        val backgroundVocals = words.filter { it.isBackground }

        val hasBackgroundVocals = backgroundVocals.isNotEmpty() && !backgroundContent.isNullOrBlank()

        fun getVocals(background: Boolean) = if (background) backgroundVocals else mainVocals

        fun getText(background: Boolean) = if (background) backgroundContent.orEmpty() else content
    }

    companion object {
        const val MIN_OFFSET_TIME = 3500

        val EmptyContent = TextContent("", null, null, emptyList())
    }
}