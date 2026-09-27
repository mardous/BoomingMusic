package com.mardous.booming.data.local.lyrics.ttml

import android.util.Log
import com.mardous.booming.data.local.lyrics.LyricsInfo
import com.mardous.booming.data.local.lyrics.LyricsParser
import com.mardous.booming.data.model.lyrics.SyncedLyrics
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.util.regex.Pattern

/**
 * Parser for TTML (Timed Text Markup Language) format.
 *
 * This parser is designed to handle Apple Music-style TTML lyrics, including:
 * - Hierarchical structure: `<body>` -> `<div>` (sections) -> `<p>` (lines) -> `<span>` (words/background)
 * - Word-level synchronization using `<span>` tags with `begin`, `end`, or `dur`.
 * - Multiple agents (actors) defined in the `<head>` and referenced by `ttm:agent`.
 * - Background vocals identified by `ttm:role="x-bg"`.
 * - Translations and transliterations.
 * - Various time expressions (clock time and offset time).
 */
class TtmlLyricsParser : LyricsParser {

    override fun getInfo(reader: Reader): LyricsInfo {
        return try {
            val parser = XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = false
            }.newPullParser().apply {
                setInput(reader)
            }

            var foundTt = false
            var insideBody = false
            var hasTime = false
            var hasParagraphs = false

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase().substringAfterLast(":")) {
                            "tt" -> foundTt = true
                            TtmlNode.TAG_BODY -> insideBody = true
                            TtmlNode.TAG_PARAGRAPH -> {
                                if (insideBody) {
                                    hasParagraphs = true
                                    if (hasTimeAttribute(parser)) {
                                        hasTime = true
                                    }
                                }
                            }
                            TtmlNode.TAG_SPAN,
                            TtmlNode.TAG_DIV -> {
                                if (insideBody && !hasTime) {
                                    if (hasTimeAttribute(parser)) {
                                        hasTime = true
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name.lowercase().substringAfterLast(":")
                        if (tagName == TtmlNode.TAG_BODY) {
                            insideBody = false
                        }
                    }
                }
                event = parser.next()
            }

            if (foundTt && hasParagraphs) {
                LyricsInfo.Valid(actuallySynced = hasTime)
            } else {
                LyricsInfo.Invalid
            }
        } catch (_: Exception) {
            LyricsInfo.Invalid
        }
    }

    /**
     * Performs a simple parsing of the TTML file to obtain lyrics in plain text format.
     *
     * This parsing method is faster because it doesn't require instantiating a [TtmlNodeTree];
     * however, it can also be lenient and produce unpredictable results under certain circumstances.
     * For this reason, it should only be used if [getInfo] reports that the TTML lyrics do not contain timestamps.
     */
    override fun parseAsPlain(reader: Reader): String? {
        return try {
            val parser = XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = false
            }.newPullParser().apply {
                setInput(reader)
            }

            val builder = StringBuilder()
            var insideBody = false
            var insideParagraph = false

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name.lowercase().substringAfterLast(":")
                        when (tagName) {
                            TtmlNode.TAG_BODY -> insideBody = true
                            TtmlNode.TAG_PARAGRAPH -> {
                                if (insideBody) {
                                    insideParagraph = true
                                }
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (insideBody && insideParagraph) {
                            val text = parser.text.trim()
                            if (text.isNotEmpty()) {
                                builder.append(text)
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name.lowercase().substringAfterLast(":")
                        when (tagName) {
                            TtmlNode.TAG_PARAGRAPH -> {
                                if (insideBody) {
                                    insideParagraph = false
                                    builder.appendLine()
                                }
                            }

                            TtmlNode.TAG_DIV -> builder.appendLine()
                            TtmlNode.TAG_BODY -> insideBody = false
                        }
                    }
                }
                event = parser.next()
            }

            parser.setInput(null)

            val result = builder.toString().trim()
            result.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Main entry point for parsing TTML content using [XmlPullParser].
     * It builds a [TtmlNodeTree] by processing start and end tags.
     * The tree is then converted into [SyncedLyrics].
     */
    override fun parse(reader: Reader, trackLength: Long, ignoreBlankLines: Boolean): SyncedLyrics? {
        try {
            val parser = XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = false
            }.newPullParser().apply {
                setInput(reader)
            }

            val nodeTree = TtmlNodeTree()
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (!isSupportedTag(name)) {
                            eventType = parser.next()
                            continue
                        }
                        when (name) {
                            TtmlNode.TAG_AGENT -> {
                                // Agents define the performers of the lyrics
                                nodeTree.addAgent(
                                    id = parser.getAttributeValue(null, "xml:id"),
                                    type = parser.getAttributeValue(null, "type")
                                )
                            }

                            TtmlNode.TAG_TRANSLITERATION -> {
                                val lang = parser.getAttributeValue(null, "xml:lang")
                                if (nodeTree.createTransliteration(lang) == null) {
                                    throw XmlPullParserException("transliteration format isn't valid")
                                }
                            }

                            TtmlNode.TAG_TRANSLATION -> {
                                val type = parser.getAttributeValue(null, "type")
                                if (type != "subtitle") {
                                    throw XmlPullParserException("unknown translation type: $type")
                                }
                                val lang = parser.getAttributeValue(null, "xml:lang")
                                if (nodeTree.createTranslation(lang) == null) {
                                    throw XmlPullParserException("translation format isn't valid")
                                }
                            }

                            TtmlNode.TAG_TEXT -> {
                                // Associated with accompaniment (translation/transliteration)
                                val key = parser.getAttributeValue(null, "for")
                                if (!nodeTree.prepareAccompanimentText(key)) break
                            }

                            TtmlNode.TAG_BODY -> {
                                val hasRoot = nodeTree.addRoot(
                                    TtmlNode.buildBody(parser.getTimeAttribute("dur"))
                                )
                                if (!hasRoot) break
                            }

                            TtmlNode.TAG_DIV -> {
                                // Sections (div) group multiple lines together
                                val openSection = nodeTree.openSection(
                                    TtmlNode.buildSection(
                                        begin = parser.getTimeAttribute("begin"),
                                        end = parser.getTimeAttribute("end"),
                                        dur = parser.getTimeAttribute("dur")
                                    )
                                )
                                if (!openSection && nodeTree.hasRoot) break
                            }

                            TtmlNode.TAG_PARAGRAPH -> {
                                // Paragraphs (p) represent a single line of lyrics
                                val agentAttribute = parser.getAttributeValue(null, "ttm:agent")
                                val openLine = nodeTree.openLine(
                                    TtmlNode.buildLine(
                                        begin = parser.getTimeAttribute("begin"),
                                        end = parser.getTimeAttribute("end"),
                                        dur = parser.getTimeAttribute("dur"),
                                        key = parser.getAttributeValue(null, "itunes:key"),
                                        agent = agentAttribute?.let { nodeTree.getAgent(it) }
                                    )
                                )
                                if (!openLine && nodeTree.hasRoot) break
                            }

                            TtmlNode.TAG_SPAN -> {
                                // Spans can be words (timed), background vocals, or inline translations
                                val role = parser.getAttributeValue(null, "ttm:role")
                                val lang = parser.getAttributeValue(null, "xml:lang")
                                if (role == null) {
                                    // Default span is treated as a word
                                    val openWord = nodeTree.openWord(
                                        TtmlNode.buildWord(
                                            begin = parser.getTimeAttribute("begin"),
                                            end = parser.getTimeAttribute("end"),
                                            dur = parser.getTimeAttribute("dur")
                                        )
                                    )
                                    if (!openWord && nodeTree.hasRoot) break
                                } else {
                                    when (role) {
                                        "x-bg" -> {
                                            nodeTree.enterBackground()
                                        }
                                        "x-translation" -> {
                                            nodeTree.prepareTranslationForCurrentLine(lang)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val name = parser.name
                        if (!isSupportedTag(name)) {
                            eventType = parser.next()
                            continue
                        }
                        when (name) {
                            TtmlNode.TAG_TRANSLITERATION,
                            TtmlNode.TAG_TRANSLATION -> if (!nodeTree.closeAccompaniment()) break
                            TtmlNode.TAG_TEXT -> if (!nodeTree.finishAccompanimentText()) break
                            TtmlNode.TAG_BODY -> if (!nodeTree.closeNode(TtmlNode.NODE_BODY)) break
                            TtmlNode.TAG_DIV -> if (!nodeTree.closeNode(TtmlNode.NODE_SECTION)) break
                            TtmlNode.TAG_PARAGRAPH -> if (!nodeTree.closeNode(TtmlNode.NODE_LINE)) break
                            TtmlNode.TAG_SPAN -> {
                                if (!nodeTree.finishTranslationForCurrentLine()) {
                                    val closeWord = nodeTree.closeNode(TtmlNode.NODE_WORD)
                                    if (!closeWord) {
                                        if (!nodeTree.closeBackground()) break
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        nodeTree.setText(parser.text)
                    }
                }
                eventType = parser.next()
            }
            nodeTree.close()
            parser.setInput(null)
            return nodeTree.toLyrics(trackLength)
        } catch (e: Exception) {
            Log.e("TtmlLyricsParser", "Couldn't parse TTML lyrics", e)
        }
        return null
    }

    private fun isSupportedTag(name: String?) = TtmlNode.isSupportedTag(name)

    private fun hasTimeAttribute(parser: XmlPullParser): Boolean {
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i).lowercase().substringAfterLast(":")
            if (attrName in setOf("begin", "dur", "end")) {
                return true
            }
        }
        return false
    }

    private fun XmlPullParser.getTimeAttribute(name: String): Long {
        try {
            val attribute = getAttributeValue(null, name)
            if (attribute != null) {
                return parseTimeExpression(attribute)
            }
        } catch (e: XmlPullParserException) {
            Log.e("TtmlLyricsParser", "Failed to parse time attribute: $name", e)
        }
        return -1
    }

    /**
     * Parses time expressions from TTML attributes.
     * Supports:
     * - Simple seconds: `12.34`
     * - Complex clock time: `00:12:34.56`
     * - Offset time with units: `100ms`, `2.5s`, `1m`
     */
    @Throws(XmlPullParserException::class)
    private fun parseTimeExpression(time: String?): Long {
        if (time == null) return -1

        var matcher = CLOCK_TIME_COMPLEX.matcher(time)
        if (matcher.matches()) {
            val hours = matcher.group(1)?.toLong() ?: 0
            val minutes = matcher.group(2)?.toLong() ?: 0
            val seconds = matcher.group(3)?.toLong() ?: 0
            val fraction = matcher.group(4)
            var durationSeconds = (hours * 3600).toDouble()
            durationSeconds += (minutes * 60).toDouble()
            durationSeconds += seconds.toDouble()
            durationSeconds += (fraction?.toDouble() ?: 0.0) / 1000
            return (durationSeconds * 1000).toLong()
        }
        matcher = CLOCK_TIME_SIMPLE.matcher(time)
        if (matcher.matches()) {
            val seconds = matcher.group(1)?.toLongOrNull() ?: 0L
            val millis = matcher.group(2)?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L
            return (seconds * 1000) + millis
        }
        matcher = OFFSET_TIME.matcher(time)
        if (matcher.matches()) {
            val timeValue = matcher.group(1)?.toDouble() ?: 0.0
            val unit = matcher.group(2)
            val offsetMillis = when(unit) {
                "h" -> (timeValue * 3600_000).toLong()
                "m" -> (timeValue * 60_000).toLong()
                "s" -> (timeValue * 1_000).toLong()
                "ms" -> timeValue.toLong()
                else -> 0L
            }
            return offsetMillis
        }
        throw XmlPullParserException("Malformed time expression: $time")
    }

    companion object {
        private val CLOCK_TIME_SIMPLE = Pattern.compile("^(\\d+)(?:\\.(\\d{1,3}))?$")
        private val CLOCK_TIME_COMPLEX = Pattern.compile("^(?:(\\d+):)?([0-5]?\\d):([0-5]?\\d)(?:\\.(\\d{1,3}))?$")
        private val OFFSET_TIME = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)(h|m|s|ms)$")
    }
}