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
        checkNotNull(text)
        val wordText = if (trimEnd) text.trimEnd() else text
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
        return if (children.isNotEmpty()) {
            children.sortBy { it.start }
            for (i in 0 until children.lastIndex) {
                if (children[i].end == INVALID_DURATION) {
                    children[i].end = children[i + 1].start
                }
            }
            if (children[children.lastIndex].end == INVALID_DURATION) {
                children[children.lastIndex].end = end
            }

            var nextWordStartIndex = 0
            val lastWordIndex = children.lastIndex

            val words = mutableListOf<SyncedLyrics.Word>()
            for ((index, child) in children.withIndex()) {
                if (index == lastWordIndex && child.text.isNullOrBlank())
                    continue

                val trimEnd = if (index == (lastWordIndex - 1)) {
                    children[lastWordIndex].text.isNullOrBlank()
                } else index == children.lastIndex

                val word = child.toWord(nextWordStartIndex, trimEnd = trimEnd)
                if (words.add(word)) {
                    nextWordStartIndex += word.content.length
                }
            }

            SyncedLyrics.TextContent(
                content = words.filterNot { it.isBackground }
                    .joinToString(separator = "") { it.content }.trim(),
                backgroundContent = words.filter { it.isBackground }
                    .joinToString(separator = "") { it.content }.trim(),
                rawContent = rawLine.orEmpty(),
                syllables = words
            )
        } else {
            SyncedLyrics.TextContent(
                content = text.orEmpty(),
                backgroundContent = null,
                rawContent = rawLine.orEmpty(),
                syllables = emptyList()
            )
        }
    }

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