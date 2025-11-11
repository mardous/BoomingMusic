package com.mardous.booming.data.local.lyrics.ttml

import android.util.Log
import com.mardous.booming.data.LyricsParser
import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.data.model.lyrics.LyricsActor
import com.mardous.booming.data.model.lyrics.LyricsFile
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.util.regex.Pattern

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
                            "tt" -> foundTt = true
                            "body" -> insideBody = true
                            "div" -> if (insideBody) {
                                foundDivInBody = true
                                break
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "body") {
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

    override fun parse(reader: Reader, trackLength: Long): Lyrics? {
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(reader)

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
                            TtmlNode.TAG_TRANSLATION -> {
                                val openTranslation = nodeTree.createNewTranslation(
                                    type = parser.getAttributeValue(null, "type"),
                                    language = parser.getAttributeValue(null, "xml:lang")
                                )
                                if (!openTranslation) break
                            }

                            TtmlNode.TAG_TEXT -> {
                                val preparedTranslation = nodeTree.prepareTranslation(
                                    key = parser.getAttributeValue(null, "for")
                                )
                                if (!preparedTranslation) break
                            }

                            TtmlNode.TAG_BODY -> {
                                val hasRoot = nodeTree.addRoot(
                                    TtmlNode.buildBody(
                                        dur = parser.getTimeAttribute("dur")
                                    )
                                )
                                if (!hasRoot) break
                            }

                            TtmlNode.TAG_DIV -> {
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
                                val openLine = nodeTree.openLine(
                                    TtmlNode.buildLine(
                                        begin = parser.getTimeAttribute("begin"),
                                        end = parser.getTimeAttribute("end"),
                                        dur = parser.getTimeAttribute("dur"),
                                        key = parser.getAttributeValue(null, "itunes:key"),
                                        actor = parser.getAgentAttribute("ttm:agent")
                                    )
                                )
                                if (!openLine && nodeTree.hasRoot) break
                            }

                            TtmlNode.TAG_SPAN -> {
                                val role = parser.getAttributeValue(null, "ttm:role")
                                if (role == null) {
                                    val openWord = nodeTree.openWord(
                                        TtmlNode.buildWord(
                                            begin = parser.getTimeAttribute("begin"),
                                            end = parser.getTimeAttribute("end"),
                                            dur = parser.getTimeAttribute("dur")
                                        )
                                    )
                                    if (!openWord && nodeTree.hasRoot) break
                                } else {
                                    if (role == "x-bg") {
                                        nodeTree.enterBackground()
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
                            TtmlNode.TAG_TRANSLATION -> if (!nodeTree.closeCurrentTranslation()) break
                            TtmlNode.TAG_TEXT -> if (!nodeTree.finishTranslation()) break
                            TtmlNode.TAG_BODY -> if (!nodeTree.closeNode(TtmlNode.NODE_BODY)) break
                            TtmlNode.TAG_DIV -> if (!nodeTree.closeNode(TtmlNode.NODE_SECTION)) break
                            TtmlNode.TAG_PARAGRAPH -> if (!nodeTree.closeNode(TtmlNode.NODE_LINE)) break
                            TtmlNode.TAG_SPAN -> {
                                val closeWord = nodeTree.closeNode(TtmlNode.NODE_WORD)
                                if (!closeWord) {
                                    if (!nodeTree.closeBackground()) break
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
            return nodeTree.toLyrics(trackLength)
        } catch (e: Exception) {
            Log.e("TtmlLyricsParser", "Couldn't parse TTML lyrics", e)
        }
        return null
    }

    private fun isSupportedTag(name: String?) = TtmlNode.isSupportedTag(name)

    private fun XmlPullParser.getAgentAttribute(name: String): LyricsActor? {
        val attribute = getAttributeValue(null, name)
        if (attribute != null) {
            return LyricsActor.getActorFromValue(attribute)
        }
        return null
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