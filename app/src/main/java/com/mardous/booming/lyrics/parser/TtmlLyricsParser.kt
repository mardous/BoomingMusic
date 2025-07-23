package com.mardous.booming.lyrics.parser

import android.util.Log
import com.mardous.booming.lyrics.Lyrics
import com.mardous.booming.lyrics.LyricsFile
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader

class TtmlLyricsParser : LyricsParser {

    override fun handles(file: LyricsFile): Boolean =
        file.format == LyricsFile.Format.TTML

    override fun handles(reader: Reader): Boolean {
        return try {
            val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
                setInput(reader)
            }

            var foundTt = false
            var foundDivInBody = false
            var insideBody = false

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            TAG_TT -> foundTt = true
                            TAG_BODY -> insideBody = true
                            TAG_DIV -> if (insideBody) {
                                foundDivInBody = true
                                break
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == TAG_BODY) {
                            insideBody = false
                        }
                    }
                }
                event = parser.next()
            }
            foundTt && foundDivInBody
        } catch (_: Exception) {
            false
        }
    }

    override fun parse(reader: Reader): Lyrics? {
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
                setInput(reader)
            }
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == TAG_BODY) {
                    return parseBody(parser)
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("TtmlLyricsParser", "Couldn't parse TTML lyrics", e)
        }
        return null
    }

    private fun parseBody(parser: XmlPullParser): Lyrics {
        val lines = mutableListOf<Lyrics.Line>()
        val duration = parser.getAttributeValue(null, ATTR_DUR).parseTime()
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == TAG_BODY)) {
            if (event == XmlPullParser.START_TAG && parser.name == TAG_DIV) {
                lines += parseDiv(parser)
            }
            event = parser.next()
        }
        lines.adjustLines(duration)
        return Lyrics(null, null, null, duration, lines)
    }

    private fun parseDiv(parser: XmlPullParser): List<Lyrics.Line> {
        val lines = mutableListOf<Lyrics.Line>()
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == TAG_DIV)) {
            if (event == XmlPullParser.START_TAG && parser.name == TAG_PARAGRAPH) {
                parseParagraph(parser)?.let { lines.add(it) }
            }
            event = parser.next()
        }
        return lines
    }

    private fun parseParagraph(parser: XmlPullParser): Lyrics.Line? {
        val lineBegin = parser.getAttributeValue(null, ATTR_BEGIN).parseTime()
        if (lineBegin == -1L) return null

        val lineEnd = parser.getAttributeValue(null, ATTR_END).parseTime()
        var lineDuration = parser.getAttributeValue(null, ATTR_DUR).parseTime()
        if (lineDuration == -1L && lineEnd != -1L) {
            lineDuration = lineEnd - lineBegin
        }

        val lineAgent = parser.getAttributeValue(null, ATTR_AGENT)
        val words = mutableListOf<Lyrics.Word>()

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == TAG_PARAGRAPH)) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == TAG_SPAN) {
                        parseSpan(words, parser, null)
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text
                    if (parser.isWhitespace && words.isNotEmpty()) {
                        val last = words.last()
                        words[words.lastIndex] = last.copy(content = "${last.content}$text")
                    }
                }
            }
            event = parser.next()
        }

        val content = words.joinToString("") { it.content }
        return Lyrics.Line(
            startAt = lineBegin,
            durationMillis = lineDuration,
            content = content,
            rawContent = content,
            words = words,
            actor = lineAgent
        )
    }

    private fun parseSpan(
        words: MutableList<Lyrics.Word>,
        parser: XmlPullParser,
        role: String?,
        inheritedBegin: Long? = null
    ) {
        val localBegin = parser.getAttributeValue(null, ATTR_BEGIN)?.parseTime()
        val wordBegin = localBegin?.takeIf { it != -1L } ?: inheritedBegin
        val wordRole = parser.getAttributeValue(null, ATTR_ROLE)
        var event = parser.next()

        while (!(event == XmlPullParser.END_TAG && parser.name == TAG_SPAN)) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == TAG_SPAN) {
                        parseSpan(words, parser, wordRole ?: role, wordBegin)
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text
                    val isBackground = (role ?: wordRole)?.contains("bg") == true

                    if (parser.isWhitespace && words.isNotEmpty()) {
                        val last = words.last()
                        words[words.lastIndex] = last.copy(content = last.content + text)
                    } else if (!text.isNullOrBlank() && wordBegin != null && wordBegin != -1L) {
                        words.add(Lyrics.Word(text, wordBegin, isBackground))
                    }
                }
            }
            event = parser.next()
        }
    }

    private fun String?.parseTime(): Long {
        if (this == null) return -1

        val normalized = this.trim()
        val (timePart, millisPart) = if ('.' in normalized) {
            val parts = normalized.split('.')
            parts[0] to parts.getOrElse(1) { "000" }.padEnd(3, '0').take(3)
        } else {
            normalized to "000"
        }

        val timeUnits = timePart.split(":").mapNotNull { it.toLongOrNull() }
        val millis = millisPart.toLongOrNull() ?: 0

        return when (timeUnits.size) {
            3 -> (timeUnits[0] * 3600 + timeUnits[1] * 60 + timeUnits[2]) * 1000 + millis
            2 -> (timeUnits[0] * 60 + timeUnits[1]) * 1000 + millis
            1 -> timeUnits[0] * 1000 + millis
            else -> -1
        }
    }

    companion object {
        private const val TAG_TT = "tt"
        private const val TAG_BODY = "body"
        private const val TAG_DIV = "div"
        private const val TAG_PARAGRAPH = "p"
        private const val TAG_SPAN = "span"

        private const val ATTR_DUR = "dur"
        private const val ATTR_BEGIN = "begin"
        private const val ATTR_END = "end"
        private const val ATTR_ROLE = "ttm:role"
        private const val ATTR_AGENT = "ttm:agent"
    }
}