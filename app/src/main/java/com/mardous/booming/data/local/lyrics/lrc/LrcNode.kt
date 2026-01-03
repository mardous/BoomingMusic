package com.mardous.booming.data.local.lyrics.lrc

import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.data.model.lyrics.LyricsActor

internal class LrcNode(
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
                start = start,
                text = text,
                bgText = null,
                rawLine = null,
                actor = actor
            ))
        }
        return false
    }

    private fun toWord(startIndex: Int): Lyrics.Word {
        check(!text.isNullOrBlank())
        return Lyrics.Word(
            content = text,
            startMillis = start,
            startIndex = startIndex,
            endMillis = end,
            endIndex = startIndex + (text.length - 1),
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

            val words = mutableListOf<Lyrics.Word>()
            for (child in children) {
                if (child.text.isNullOrBlank()) continue

                val startIndex = words.sumOf { it.content.length }
                words.add(child.toWord(startIndex))
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
            actor = actor
        )
    }

    companion object {
        const val INVALID_DURATION = -1L
    }
}