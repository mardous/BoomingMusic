package com.mardous.booming.data.local.lyrics.lrc

import android.util.Log
import com.mardous.booming.data.LyricsParser
import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.data.model.lyrics.LyricsActor
import com.mardous.booming.data.model.lyrics.LyricsFile
import java.io.Reader
import java.util.Locale

class LrcLyricsParser : LyricsParser {

    override fun handles(file: LyricsFile): Boolean {
        return file.format == LyricsFile.Format.LRC
    }

    override fun handles(reader: Reader): Boolean {
        val content = reader.buffered().use { it.readText() }
        return content
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .any { line ->
                if (ATTRIBUTE_PATTERN.matches(line)) {
                    false
                } else {
                    val hasTime = LINE_TIME_PATTERN.containsMatchIn(line)
                    val hasContent = LINE_PATTERN.matchEntire(line)?.groupValues
                        ?.getOrNull(2)
                        ?.isNotBlank() == true

                    hasTime && hasContent
                }
            }
    }

    override fun parse(reader: Reader, trackLength: Long, ignoreBlankLines: Boolean): Lyrics? {
        val attributes = hashMapOf<String, String>()
        val rawLines = mutableListOf<LrcNode>()
        try {
            reader.buffered().use { br ->
                while (true) {
                    val line = br.readLine() ?: break
                    if (line.isBlank()) continue

                    val attrMatcher = ATTRIBUTE_PATTERN.find(line)
                    if (attrMatcher != null) {
                        val attr = attrMatcher.groupValues[1].lowercase(Locale.getDefault()).trim()
                        val value = attrMatcher.groupValues[2].lowercase(Locale.getDefault())
                            .trim()
                            .takeUnless { it.isEmpty() } ?: continue

                        attributes[attr] = value
                    } else {
                        val lineResult = LINE_PATTERN.find(line)
                        if (lineResult != null) {
                            val base = lineResult.groupValues[1].trim()
                                .takeUnless { it.isEmpty() } ?: continue
                            val text = lineResult.groupValues[2].trim()
                            val bgText = lineResult.groupValues[3]
                                .takeIf { it.isNotEmpty() }

                            val timeResult = LINE_TIME_PATTERN.find(base)
                            if (timeResult != null) {
                                val timeMs = parseTime(timeResult)
                                if (timeMs > LrcNode.INVALID_DURATION) {
                                    rawLines.add(LrcNode(timeMs, text, bgText, line))
                                }
                            } else {
                                val backgroundMatcher = BACKGROUND_ONLY_PATTERN.find(line)
                                if (rawLines.isNotEmpty() && backgroundMatcher != null) {
                                    val bgText = backgroundMatcher.groupValues.getOrNull(1)?.trim()
                                    if (!bgText.isNullOrEmpty()) {
                                        val lastNode = rawLines.last()
                                        if (lastNode.bgText.isNullOrEmpty()) {
                                            lastNode.rawLine = "${lastNode.rawLine}[bg:$bgText]"
                                            lastNode.bgText = bgText
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return parse(attributes, rawLines, trackLength, ignoreBlankLines)
    }

    private fun parse(
        attributes: Map<String, String>,
        rawLines: List<LrcNode>,
        trackLength: Long,
        ignoreBlankLines: Boolean
    ): Lyrics? {
        val lines = mutableMapOf<Long, Lyrics.Line?>()
        val length = attributes["length"]
            ?.let { parseTime(it) }
            ?.takeIf { it > LrcNode.INVALID_DURATION }
            ?: trackLength

        try {
            for (i in 0 until rawLines.size) {
                val entry = rawLines[i]

                if (entry.start > length) {
                    // This is likely due to a metadata error or a corrupted audio file,
                    // resulting in a total duration shorter than the actual duration of the lyrics.
                    // In either case, this leads to a failure. However, if it's the latter, it's
                    // still fine to continue with the current lines; this way, the user will still
                    // be able to see the lyrics for the incomplete song.
                    break
                }

                var nextStep = 1
                var nextEntry = rawLines.getOrNull(i + nextStep)
                while (nextEntry != null && entry.start == nextEntry.start) {
                    nextEntry = rawLines.getOrNull(i + (nextStep++))
                }

                val end = nextEntry?.let { nextEntryNonNull ->
                    if (nextEntryNonNull.start >= entry.start) {
                        nextEntryNonNull.start
                    } else {
                        val firstLine = lines.values.firstOrNull()
                        if (firstLine != null && firstLine.startAt == nextEntryNonNull.start) {
                            length
                        } else {
                            error("Malformed LRC file")
                        }
                    }
                }

                entry.end = end ?: length

                if (entry.text.isNullOrBlank()) {
                    if (!ignoreBlankLines && !lines.containsKey(entry.start)) {
                        lines[entry.start] = entry.toLine()
                    }
                } else {
                    // If a line already exists at the same timestamp, this entry could be a translation.
                    // We must check that the new entry is not exactly the same as the previous one
                    // and that the previous line does not already contain a translation; for now,
                    // we only handle one translation per line.
                    val existing = lines[entry.start]
                    if (existing != null && !existing.content.isEmpty &&
                        existing.content.rawContent != entry.rawLine && existing.translation == null
                    ) {
                        // If the new entry is word-synced, we process it.
                        addChildren(entry, existing.actor)

                        // Once words have been processed, we can check if the content is
                        // exactly the same; if so, we discard the new entry since it does not
                        // add any real value as a translation.
                        val translationContent = entry.getTextContent()
                        if (translationContent.content != existing.content.content) {
                            var newDuration = existing.durationMillis
                            val newEnd = if (existing.end == 0L) entry.end else existing.end
                            if (newEnd != existing.end) {
                                newDuration = (newEnd - existing.startAt)
                            }
                            if (translationContent.isWordSynced && !existing.isWordSynced) {
                                // It appears we are dealing with an edge case in which the second
                                // line actually represents the main content and the first line
                                // is the translation.
                                lines[entry.start] = existing.copy(
                                    end = newEnd,
                                    durationMillis = newDuration,
                                    content = translationContent,
                                    translation = existing.content,
                                    actor = entry.actor ?: existing.actor
                                )
                            } else {
                                lines[entry.start] = existing.copy(
                                    end = newEnd,
                                    durationMillis = newDuration,
                                    translation = translationContent
                                )
                            }
                        }
                    } else {
                        // It's a new line, we just add it to the list.
                        addChildren(entry, null)
                        lines[entry.start] = entry.toLine()
                    }
                }
            }

            val linesWithOffset = lines.values
                .filterNotNull()
                .distinctBy { it.id }
                .toMutableList().apply {
                    sortBy { it.startAt }
                }

            if (linesWithOffset.isNotEmpty()) {
                val firstLine = linesWithOffset.first()
                if (firstLine.startAt > Lyrics.MIN_OFFSET_TIME) {
                    linesWithOffset.add(0,
                        Lyrics.Line(
                            startAt = 0,
                            end = firstLine.startAt,
                            content = Lyrics.EmptyContent,
                            translation = null,
                            actor = firstLine.actor
                        )
                    )
                }
            }

            return Lyrics(
                title = attributes["ti"],
                artist = attributes["ar"],
                album = attributes["al"],
                durationMillis = length,
                lines = linesWithOffset,
                offset = attributes["offset"]?.toLongOrNull() ?: 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun addChildren(entry: LrcNode, actor: LyricsActor?) {
        check(!entry.text.isNullOrBlank())

        val matchResult = LINE_ACTOR_PATTERN.find(entry.text)
        entry.actor = actor ?: LyricsActor.getActorFromValue(matchResult?.groupValues?.get(1))

        val text = matchResult?.groupValues?.get(2) ?: entry.text
        LINE_WORD_PATTERN.findAll(text).forEach { match ->
            entry.addChild(
                start = parseTime(match),
                text = match.groupValues.getOrNull(3),
                actor = entry.actor
            )
        }

        entry.bgText?.let {
            LINE_WORD_PATTERN.findAll(it).forEach { match ->
                entry.addChild(
                    start = parseTime(match),
                    text = match.groupValues.getOrNull(3),
                    actor = entry.actor?.asBackground(true)
                )
            }
        }
    }

    private fun parseTime(str: String): Long {
        val result = TIME_PATTERN.find(str)
        if (result != null) {
            return parseTime(result)
        }
        return LrcNode.INVALID_DURATION
    }

    private fun parseTime(result: MatchResult): Long {
        try {
            val m = result.groupValues.getOrNull(1)?.toInt()
            val s = result.groupValues.getOrNull(2)?.toFloat()
            return if (m != null && s != null) {
                (s * LRC_SECONDS_TO_MS_MULTIPLIER).toLong() + m * LRC_MINUTES_TO_MS_MULTIPLIER
            } else LrcNode.INVALID_DURATION
        } catch (e: Exception) {
            Log.d("LrcLyricsParser", "LRC timestamp format is incorrect: ${result.value}", e)
        }
        return LrcNode.INVALID_DURATION
    }

    companion object {
        private const val LRC_SECONDS_TO_MS_MULTIPLIER = 1000f
        private const val LRC_MINUTES_TO_MS_MULTIPLIER = 60 * 1000

        private val TIME_PATTERN = Regex("(\\d+):(\\d{2}(?:\\.\\d+)?)")
        private val LINE_PATTERN = Regex("((?:\\[.*?])+)(.*?)(?:\\[bg:(.*?)])?$")
        private val LINE_TIME_PATTERN = Regex("\\[${TIME_PATTERN.pattern}]")
        private val LINE_ACTOR_PATTERN = Regex("^([vV]\\d+|D|M|F)\\s*:\\s*(.*)")
        private val LINE_WORD_PATTERN = Regex("<${TIME_PATTERN.pattern}>([^<]*)")
        private val BACKGROUND_ONLY_PATTERN = Regex("^\\[bg:(.*?)]\\s*$")
        private val ATTRIBUTE_PATTERN = Regex("\\[(offset|ti|ar|al|length|by):(.+)]", RegexOption.IGNORE_CASE)
    }
}