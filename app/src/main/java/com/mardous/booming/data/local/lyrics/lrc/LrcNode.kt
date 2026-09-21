package com.mardous.booming.data.local.lyrics.lrc

import com.mardous.booming.data.model.lyrics.LyricsActor
import com.mardous.booming.data.model.lyrics.SyncedLyrics

data class LrcNode(
    val start: Long,
    val text: String?,
    var bgText: String?,
    var rawLine: String?,
    var actor: LyricsActor? = null
) {

    private val children = mutableListOf<LrcNode>()
    var end: Long = INVALID_DURATION

    fun addChild(start: Long, end: Long = INVALID_DURATION, text: String?, actor: LyricsActor?): Boolean {
        if (start > INVALID_DURATION) {
            val node = LrcNode(
                start = start,
                text = text,
                bgText = null,
                rawLine = null,
                actor = actor
            )
            node.end = end
            return children.add(node)
        }
        return false
    }

    private fun toWord(startIndex: Int, trimEnd: Boolean = false): SyncedLyrics.Word {
        val currentText = checkNotNull(text) { "Word text cannot be null" }
        val wordText = if (trimEnd) currentText.trimEnd() else currentText
        return SyncedLyrics.Word(
            content = wordText,
            start = start,
            startIndex = startIndex,
            end = end,
            endIndex = startIndex + (wordText.length - 1),
            duration = (end - start),
            actor = actor
        )
    }

    fun getTextContent(): SyncedLyrics.TextContent {
        if (children.isEmpty()) {
            return getLineSyncedTextContent()
        }

        val validChildren = children
            .filterNot { it.text.isNullOrEmpty() }
            .sortedBy { it.start }

        if (validChildren.isEmpty()) {
            return getLineSyncedTextContent()
        }

        val lastWordIndex = validChildren.lastIndex
        for (i in 0 until lastWordIndex) {
            if (validChildren[i].end == INVALID_DURATION) {
                validChildren[i].end = validChildren[i + 1].start
            }
        }
        if (validChildren[lastWordIndex].end == INVALID_DURATION) {
            validChildren[lastWordIndex].end = end
        }

        var nextWordStartIndex = 0
        val words = mutableListOf<SyncedLyrics.Word>()
        for ((index, child) in validChildren.withIndex()) {
            if (index == lastWordIndex && child.text.isNullOrBlank())
                continue

            val trimEnd = if (index == lastWordIndex) true else {
                val nextText = validChildren[index + 1].text
                nextText.isNullOrBlank() || nextText.startsWith(" ")
            }

            val word = child.toWord(nextWordStartIndex, trimEnd = trimEnd)
            if (words.add(word)) {
                nextWordStartIndex += word.content.length
            }
        }

        return SyncedLyrics.TextContent(
            content = words.filterNot { it.isBackground }
                .joinToString(separator = "") { it.content }.trim(),
            backgroundContent = words.filter { it.isBackground }
                .joinToString(separator = "") { it.content }.trim(),
            rawContent = rawLine.orEmpty(),
            syllables = words
        )
    }

    private fun getLineSyncedTextContent() = SyncedLyrics.TextContent(
        content = text.orEmpty(),
        backgroundContent = null,
        rawContent = rawLine.orEmpty(),
        syllables = emptyList()
    )

    fun toLine(): SyncedLyrics.Line? {
        if (start <= INVALID_DURATION && end <= INVALID_DURATION) {
            return null
        }
        return SyncedLyrics.Line(
            start = start,
            end = end,
            duration = (end - start),
            content = getTextContent(),
            transliteration = null,
            translation = null,
            actor = actor
        )
    }

    companion object {
        const val INVALID_DURATION = -1L
    }
}