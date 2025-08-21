package com.mardous.booming.data.local.lyrics.lrc

import android.util.Log
import com.mardous.booming.data.LyricsParser
import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.data.model.lyrics.LyricsFile
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

    override fun parse(reader: Reader, trackLength: Long): Lyrics? {
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
                            val time = lineResult.groupValues[1].trim()
                                .takeUnless { it.isEmpty() } ?: continue
                            val rawText = lineResult.groupValues[2].trim()
                                .takeUnless { it.isEmpty() } ?: continue

                            val timeResult = LINE_TIME_PATTERN.find(time)
                            if (timeResult != null) {
                                val timeMs = parseTime(timeResult)
                                if (timeMs > LrcNode.INVALID_DURATION) {
                                    rawLines.add(LrcNode(timeMs, rawText))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return parse(attributes, rawLines, trackLength)
    }

    private fun parse(
        attributes: Map<String, String>,
        rawLines: List<LrcNode>,
        trackLength: Long
    ): Lyrics? {
        val lines = mutableListOf<Lyrics.Line>()
        val length = attributes["length"]
            ?.let { parseTime(it) }
            ?.takeIf { it > LrcNode.INVALID_DURATION }
            ?: trackLength

        try {
            for (i in 0 until rawLines.size) {
                val entry = rawLines[i]
                entry.setEnd(rawLines.getOrNull(i + 1)?.start ?: length)

                val matchResult = LINE_ACTOR_PATTERN.find(entry.text)
                val actor = matchResult?.groupValues?.get(1)
                entry.setActor(actor)

                val text = matchResult?.groupValues?.get(2) ?: entry.text
                LINE_WORD_PATTERN.findAll(text).forEach { match ->
                    entry.addChildren(
                        start = parseTime(match),
                        text = match.groupValues.getOrNull(3),
                    )
                }

                val line = entry.toLine()
                if (line != null) {
                    lines.add(line)
                }
            }
            return Lyrics(
                title = attributes["ti"],
                artist = attributes["ar"],
                album = attributes["al"],
                durationMillis = length,
                lines = lines.sortedBy { it.startAt }.distinctBy { it.id }
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
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
        private val LINE_PATTERN = Regex("((?:\\[.*?])+)(.*)")
        private val LINE_TIME_PATTERN = Regex("\\[${TIME_PATTERN.pattern}]")
        private val LINE_ACTOR_PATTERN = Regex("^(v\\d+):\\s*(.*)", RegexOption.IGNORE_CASE)
        private val LINE_WORD_PATTERN = Regex("<${TIME_PATTERN.pattern}>([^<]+)")
        private val ATTRIBUTE_PATTERN = Regex("\\[(offset|ti|ar|al|length|by):(.+)]", RegexOption.IGNORE_CASE)
    }
}