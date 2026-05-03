package com.mardous.booming.data.local.lyrics.lrc

import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.data.model.lyrics.LyricsActor

internal class LrcNode(
    val rawIndex: Int,
    val start: Long,
    val text: String?,
    var bgText: String?,
    var rawLine: String?,
    var actor: LyricsActor? = null
) {
    private val children = mutableListOf<LrcNode>()

    var end: Long = INVALID_DURATION

    fun addChild(start: Long, text: String?, actor: LyricsActor?): Boolean {
        if (start > INVALID_DURATION) {
            return children.add(LrcNode(
                rawIndex = -1,
                start = start,
                text = text,
                bgText = null,
                rawLine = null,
                actor = actor
            ))
        }
        return false
    }

    private fun toWord(startIndex: Int, trimEnd: Boolean = false): Lyrics.Word {
        checkNotNull(text)
        val wordText = if (trimEnd) text.trimEnd() else text
        return Lyrics.Word(
            content = wordText,
            startMillis = start,
            startIndex = startIndex,
            endMillis = end,
            endIndex = startIndex + (wordText.length - 1),
            durationMillis = (end - start),
            actor = actor
        )
    }

    fun getTextContent(): Lyrics.TextContent {
        return if (children.isNotEmpty()) {
            children.sortBy { it.start }
            for (i in 0 until children.lastIndex) {
                children[i].end = children[i + 1].start
            }
            children[children.lastIndex].end = end

            var nextWordStartIndex = 0
            val lastWordIndex = children.lastIndex

            val words = mutableListOf<Lyrics.Word>()
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

            Lyrics.TextContent(
                content = words.filterNot { it.isBackground }
                    .joinToString(separator = "") { it.content }.trim(),
                backgroundContent = words.filter { it.isBackground }
                    .joinToString(separator = "") { it.content }.trim(),
                rawContent = rawLine.orEmpty(),
                words = words
            )
        } else {
            Lyrics.TextContent(
                content = text.orEmpty(),
                backgroundContent = null,
                rawContent = rawLine.orEmpty(),
                words = emptyList()
            )
        }
    }

    fun toLine(): Lyrics.Line? {
        if (start <= INVALID_DURATION && end <= INVALID_DURATION) {
            return null
        }
        return Lyrics.Line(
            startAt = start,
            end = end,
            durationMillis = (end - start),
            content = getTextContent(),
            translation = null,
            actor = actor,
            rawIndex = rawIndex
        )
    }

    companion object {
        const val INVALID_DURATION = -1L
    }
}