package com.mardous.booming.lyrics.parser

import com.mardous.booming.lyrics.Lyrics
import com.mardous.booming.lyrics.LyricsFile
import java.io.IOException
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

    override fun parse(reader: Reader): Lyrics? {
        val attributes = hashMapOf<String, String>()
        val lines = arrayListOf<Lyrics.Line>()
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
                            val time = lineResult.groupValues[1].trim()
                                .takeUnless { it.isEmpty() } ?: continue
                            val rawText = lineResult.groupValues[2].trim()
                                .takeUnless { it.isEmpty() } ?: continue

                            val actorMatch = LINE_ACTOR_PATTERN.find(rawText)
                            val actor = actorMatch?.groupValues?.get(1)
                            val text = actorMatch?.groupValues?.get(2) ?: rawText

                            val wordEntries = buildList {
                                LINE_WORD_PATTERN.findAll(text).forEach { match ->
                                    val ms = parseTime(match) ?: return@forEach
                                    val word = match.groupValues.getOrNull(3) ?: return@forEach

                                    add(Lyrics.Word(content = word, startAt = ms))
                                }
                            }.sortedBy {
                                it.startAt
                            }

                            LINE_TIME_PATTERN.findAll(time).forEach { result ->
                                val startAt = parseTime(result) ?: return@forEach
                                val syncedLine = if (wordEntries.isNotEmpty()) {
                                    val wordStartAt = wordEntries.minOf { it.startAt }
                                    val content = wordEntries.joinToString(separator = "") {
                                        it.content
                                    }
                                    Lyrics.Line(
                                        startAt = wordStartAt,
                                        durationMillis = 0,
                                        content = content.trim(),
                                        rawContent = line,
                                        words = wordEntries,
                                        actor = actor
                                    )
                                } else {
                                    Lyrics.Line(
                                        startAt = startAt,
                                        durationMillis = 0,
                                        content = text,
                                        rawContent = line,
                                        words = emptyList(),
                                        actor = actor
                                    )
                                }
                                lines.add(syncedLine)
                            }
                        }
                    }
                }
                val length = attributes["length"]?.let { parseTime(it) } ?: -1
                lines.adjustLines(length)
                return Lyrics(
                    title = attributes["ti"],
                    artist = attributes["ar"],
                    album = attributes["al"],
                    durationMillis = length,
                    lines = lines
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseTime(str: String): Long? {
        val result = TIME_PATTERN.find(str)
        if (result != null) {
            return parseTime(result)
        }
        return null
    }

    private fun parseTime(result: MatchResult): Long? {
        val m = result.groupValues.getOrNull(1)?.toInt()
        val s = result.groupValues.getOrNull(2)?.toFloat()
        return if (m != null && s != null) {
            (s * LRC_SECONDS_TO_MS_MULTIPLIER).toLong() + m * LRC_MINUTES_TO_MS_MULTIPLIER
        } else null
    }

    companion object {
        private const val LRC_SECONDS_TO_MS_MULTIPLIER = 1000f
        private const val LRC_MINUTES_TO_MS_MULTIPLIER = 60 * 1000

        private val TIME_PATTERN = Regex("(\\d+):(\\d{2}(?:\\.\\d+)?)")
        private val LINE_PATTERN = Regex("((?:\\[.*?])+)(.*)")
        private val LINE_TIME_PATTERN = Regex("\\[${TIME_PATTERN.pattern}]")
        private val LINE_ACTOR_PATTERN = Regex("^(v\\d+):\\s*(.*)", RegexOption.IGNORE_CASE)
        private val LINE_WORD_PATTERN = Regex("<${TIME_PATTERN.pattern}>([^<]+)")
        private val ATTRIBUTE_PATTERN = Regex("\\[(offset|ti|ar|al|length|by):(.+)]", RegexOption.IGNORE_CASE)
    }
}