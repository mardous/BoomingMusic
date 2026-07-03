package com.mardous.booming.data.local.lyrics.lrc

import android.util.Log
import com.mardous.booming.data.LyricsParser
import com.mardous.booming.data.model.lyrics.LyricsActor
import com.mardous.booming.data.model.lyrics.LyricsFile
import com.mardous.booming.data.model.lyrics.SyncedLyrics
import java.io.Reader
import java.util.Locale

/**
 * Parser for LRC format.
 *
 * This parser supports:
 * - Standard LRC format: `[mm:ss.xx] line content`
 * - Enhanced LRC format (Word-sync): `[mm:ss.xx] <mm:ss.xx> word <mm:ss.xx> word...`
 * - Metadata attributes: `[ar:Artist]`, `[ti:Title]`, `[offset:milliseconds]`, etc.
 * - Multiple timestamps for the same line: `[mm:ss.xx][mm:ss.yy] line content`
 * - Background vocals: `[bg:Background content]` or inline `[00:12.34] Line [bg:Background]`
 * - Translations: Identified by matching timestamps across different lines.
 */
class LrcLyricsParser : LyricsParser {

    override fun handles(file: LyricsFile): Boolean {
        return file.format == LyricsFile.Format.LRC
    }

    /**
     * Quickly checks if the reader content looks like LRC.
     * It scans for lines that have both a timestamp and actual text content,
     * while ignoring metadata attribute lines.
     */
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

    /**
     * Main entry point for parsing LRC content.
     * First pass: Extracts all raw lines and attributes, handling multiple timestamps per line.
     * Second pass: Sorts and converts raw nodes into [SyncedLyrics], resolving overlaps and durations.
     */
    override fun parse(reader: Reader, trackLength: Long, ignoreBlankLines: Boolean): SyncedLyrics? {
        val attributes = hashMapOf<String, String>()
        val rawLines = mutableListOf<LrcNode>()
        try {
            reader.buffered().use { br ->
                while (true) {
                    val line = br.readLine() ?: break
                    if (line.isBlank()) continue

                    // Check for metadata attributes like [ti:Title]
                    val attrResult = ATTRIBUTE_PATTERN.find(line)
                    // Special case: Karaoke word-sync line or Background-only line
                    val karaokeMatcher = KARAOKE_LINE_PATTERN.find(line)
                    if (attrResult != null) {
                        val attr = attrResult.groupValues[1].lowercase(Locale.getDefault()).trim()
                        val value = attrResult.groupValues[2].lowercase(Locale.getDefault())
                            .trim()
                            .takeUnless { it.isEmpty() } ?: continue

                        attributes[attr] = value
                    } else if (karaokeMatcher != null && rawLines.isNotEmpty()) {
                        val lastNode = rawLines.last()
                        val matches = KARAOKE_WORD_PATTERN.findAll(karaokeMatcher.groupValues[1]).toList()
                        matches.forEachIndexed { index, match ->
                            var wordText = match.groupValues[1]
                            if (index < matches.lastIndex && !wordText.endsWith(" ")) {
                                wordText += " "
                            }
                            val startMs = (match.groupValues[2].toFloat() * 1000).toLong()
                            val endMs = (match.groupValues[3].toFloat() * 1000).toLong()

                            lastNode.addChild(startMs, endMs, wordText, lastNode.actor)
                        }
                    } else {
                        // Check for lyric lines with timestamps
                        val lineResult = LINE_PATTERN.find(line)
                        if (lineResult != null) {
                            val base = lineResult.groupValues[1].trim()
                                .takeUnless { it.isEmpty() } ?: continue
                            val text = lineResult.groupValues[2].trim()
                            val bgText = lineResult.groupValues[3]
                                .takeIf { it.isNotEmpty() }

                            var foundAny = false
                            // Extract all timestamps from the line (LRC allows multiple timestamps for one line)
                            LINE_TIME_PATTERN.findAll(base).forEach { match ->
                                val timeMs = parseTime(match)
                                if (timeMs > LrcNode.INVALID_DURATION) {
                                    rawLines.add(LrcNode(timeMs, text, bgText, line))
                                    foundAny = true
                                }
                            }

                            // Special case: Background-only line
                            if (!foundAny) {
                                val backgroundMatcher = BACKGROUND_ONLY_PATTERN.find(line)
                                if (backgroundMatcher != null && rawLines.isNotEmpty()) {
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
        // LRC nodes can appear out of order in the file, especially with multi-timestamp lines
        rawLines.sortBy { it.start }
        return parse(attributes, rawLines, trackLength, ignoreBlankLines)
    }

    /**
     * Converts a list of raw [LrcNode]s into the final [SyncedLyrics] structure.
     * This method calculates durations by looking at the next line's timestamp
     * and handles translation merging for lines sharing the same start time.
     */
    private fun parse(
        attributes: Map<String, String>,
        rawLines: List<LrcNode>,
        trackLength: Long,
        ignoreBlankLines: Boolean
    ): SyncedLyrics? {
        val lines = mutableMapOf<Long, SyncedLyrics.Line?>()
        val length = attributes["length"]
            ?.let { parseTime(it) }
            ?.takeIf { it > LrcNode.INVALID_DURATION }
            ?: trackLength

        try {
            for ((i, entry) in rawLines.withIndex()) {
                if (entry.start > length) {
                    // This is likely due to a metadata error or a corrupted audio file,
                    // resulting in a total duration shorter than the actual duration of the lyrics.
                    // In either case, this leads to a failure. However, if it's the latter, it's
                    // still fine to continue with the current lines; this way, the user will still
                    // be able to see the lyrics for the incomplete song.
                    break
                }

                // Calculate the end time based on the next line's start time.
                // If there are multiple entries at the same start time, find the next one with a different start.
                var nextStep = 1
                var nextEntry = rawLines.getOrNull(i + nextStep)
                while (nextEntry != null && entry.start == nextEntry.start) {
                    nextEntry = rawLines.getOrNull(i + (nextStep++))
                }

                val end = nextEntry?.let { nextEntryNonNull ->
                    if (nextEntryNonNull.start >= entry.start) {
                        nextEntryNonNull.start
                    } else {
                        // Safety check for malformed files where timestamps might go backwards
                        val firstLine = lines.values.firstOrNull()
                        if (firstLine != null && firstLine.start == nextEntryNonNull.start) {
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
                            var newDuration = existing.duration
                            val newEnd = if (existing.end == 0L) entry.end else existing.end
                            if (newEnd != existing.end) {
                                newDuration = (newEnd - existing.start)
                            }

                            // Heuristic: if the new entry has word-sync tags and the existing one doesn't,
                            // swap them so the word-synced one is the main content.
                            if (translationContent.isWordSynced && !existing.isWordSynced) {
                                lines[entry.start] = existing.copy(
                                    end = newEnd,
                                    duration = newDuration,
                                    content = translationContent,
                                    translation = existing.content,
                                    actor = entry.actor ?: existing.actor
                                )
                            } else {
                                lines[entry.start] = existing.copy(
                                    end = newEnd,
                                    duration = newDuration,
                                    translation = translationContent
                                )
                            }
                        }
                    } else {
                        // It's a brand new line at this timestamp.
                        addChildren(entry, null)
                        lines[entry.start] = entry.toLine()
                    }
                }
            }

            val linesWithOffset = lines.values
                .filterNotNull()
                .distinctBy { it.id }
                .toMutableList().apply {
                    sortBy { it.start }
                }

            if (linesWithOffset.isNotEmpty()) {
                val firstLine = linesWithOffset.first()
                if (firstLine.start > SyncedLyrics.MIN_OFFSET_TIME) {
                    linesWithOffset.add(0,
                        SyncedLyrics.Line(
                            start = 0,
                            end = firstLine.start,
                            content = SyncedLyrics.EmptyContent,
                            transliteration = null,
                            translation = null,
                            actor = firstLine.actor
                        )
                    )
                }
            }

            return SyncedLyrics(
                lines = linesWithOffset,
                offset = attributes["offset"]?.toLongOrNull() ?: 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Parses word-sync tags and actor prefixes within a line.
     * Example: `V1: <00:12.00>Hello <00:12.50>world`
     * Also handles background tags: `[bg:<00:13.00>ooh]`
     */
    private fun addChildren(entry: LrcNode, actor: LyricsActor?) {
        check(!entry.text.isNullOrBlank())

        // Extract actor prefix if present (e.g., "M:", "F:", "V1:")
        val matchResult = LINE_ACTOR_PATTERN.find(entry.text)
        entry.actor = actor ?: LyricsActor.getActorFromValue(matchResult?.groupValues?.get(1))

        val text = matchResult?.groupValues?.get(2) ?: entry.text
        // Extract words with their relative timestamps
        LINE_WORD_PATTERN.findAll(text).forEach { match ->
            entry.addChild(
                start = parseTime(match),
                text = match.groupValues.getOrNull(3),
                actor = entry.actor
            )
        }

        // Handle word-sync within background text
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
        private val KARAOKE_LINE_PATTERN = Regex("^<(.*)>$")
        private val KARAOKE_WORD_PATTERN = Regex("([^:|]+):(\\d+(?:\\.\\d+)?):(\\d+(?:\\.\\d+)?)")
    }
}