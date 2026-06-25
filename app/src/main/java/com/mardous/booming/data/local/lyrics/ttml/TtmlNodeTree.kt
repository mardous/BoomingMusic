package com.mardous.booming.data.local.lyrics.ttml

import com.mardous.booming.data.model.lyrics.LyricsActor
import com.mardous.booming.data.model.lyrics.SyncedLyrics
import java.util.Locale

/**
 * Represents the hierarchical structure of a TTML document as a tree of [TtmlNode]s.
 *
 * This class provides higher-level operations for managing and traversing the TTML node tree,
 * including building the tree from a parsed document and resolving timing overlaps.
 *
 * Key responsibilities:
 * - Maintain the root node (usually <tt> or <body>) and its children.
 * - Traverse the node hierarchy to collect active nodes.
 * - Flatten or resolve nodes into textual output with proper timing and styles.
 *
 * @author Christians Martínez A. (mardous)
 */
internal class TtmlNodeTree {

    private var rootNode: TtmlNode? = null

    private val accompaniments = linkedMapOf<TtmlAccompaniment.Type, TtmlAccompaniment>()

    private val translations = mutableSetOf<TtmlTranslation>()
    private val agents = mutableSetOf<TtmlAgent>()

    private var lastTransliterationType: TtmlAccompaniment.Type.Transliteration? = null
    private val openTransliteration: TtmlTransliteration?
        get() = getOpenAccompaniment(lastTransliterationType)

    private var lastTranslationType: TtmlAccompaniment.Type.Translation? = null
    private val openTranslation: TtmlTranslation?
        get() = getOpenAccompaniment(lastTranslationType)

    private var openNodes = mutableMapOf<Int, TtmlNode?>()

    private var background = false
    private var closed = false

    var isInTransliteration: Boolean = false
    val hasRoot: Boolean
        get() = rootNode?.type == TtmlNode.NODE_BODY && rootNode?.closed == false

    private fun getOpenNode(type: Int): TtmlNode? {
        if (!hasRoot || closed) return null

        return openNodes.getOrPut(type) {
            rootNode?.getOpenChild(type)
        }
    }

    private inline fun <reified T : TtmlAccompaniment> getOpenAccompaniment(
        type: TtmlAccompaniment.Type?
    ): T? {
        if (type == null) return null
        val openAccompaniment = accompaniments[type]?.takeIf { !it.closed }
        if (openAccompaniment is T) return openAccompaniment
        return null
    }

    private fun getClosedTransliteration(): TtmlTransliteration? {
        return accompaniments.values
            .filterIsInstance<TtmlTransliteration>()
            .singleOrNull { it.closed }
    }

    private fun getClosedTranslation(): TtmlTranslation? {
        val systemLocale = Locale.getDefault()
        val closedTranslations = accompaniments.values
            .filterIsInstance<TtmlTranslation>()
            .filter { it.closed }

        if (closedTranslations.size == 1) {
            return closedTranslations.single()
        }

        val matchingLocale = closedTranslations.firstOrNull {
            Locale.forLanguageTag(it.type.lang).let { locale ->
                locale == systemLocale || locale.language == systemLocale.language
            }
        }

        if (matchingLocale != null) {
            return matchingLocale
        }

        return closedTranslations.firstOrNull()
    }

    fun addRoot(node: TtmlNode): Boolean {
        if (closed || node.closed) return false

        if (!hasRoot && node.type == TtmlNode.NODE_BODY) {
            rootNode = node
        }
        return hasRoot
    }

    fun addAgent(id: String, type: String): Boolean {
        val agent = TtmlAgent(id, type)
        return agents.add(agent)
    }

    fun getAgent(id: String): TtmlAgent? {
        return agents.firstOrNull { it.id == id }
    }

    fun openSection(node: TtmlNode): Boolean {
        if (!hasRoot) return false

        val sectionNode = getOpenNode(TtmlNode.NODE_SECTION)
        if (sectionNode == null && node.type == TtmlNode.NODE_SECTION) {
            return rootNode?.addChildNode(node) == true
        }
        return false
    }

    fun openLine(node: TtmlNode): Boolean {
        if (!hasRoot) return false

        val lineNode = getOpenNode(TtmlNode.NODE_LINE)
        if (lineNode == null && node.type == TtmlNode.NODE_LINE) {
            val currentSection = getOpenNode(TtmlNode.NODE_SECTION)
            return currentSection?.addChildNode(node) == true
        }
        return false
    }

    fun openWord(node: TtmlNode): Boolean {
        if (openTransliteration?.addWord(node) == true) {
            isInTransliteration = true
            return true
        }

        if (!hasRoot) return false

        val lineNode = getOpenNode(TtmlNode.NODE_LINE)
        if (lineNode != null && node.type == TtmlNode.NODE_WORD) {
            if (background) {
                node.setBackground(true)
            }
            return lineNode.addChildNode(node)
        }
        return false
    }

    fun setText(text: String?): Boolean {
        if (openTranslation?.set(text) == true ||
            openTransliteration?.set(text) == true)
            return true

        if (!hasRoot) return false

        var textNode = getOpenNode(TtmlNode.NODE_WORD)
        if (textNode == null) {
            textNode = getOpenNode(TtmlNode.NODE_LINE)
        }
        if (textNode != null) {
            return textNode.setText(text)
        }
        return false
    }

    fun enterBackground(): Boolean {
        if (openTranslation?.background(true) == true ||
            openTransliteration?.background(true) == true)
            return true

        if (!hasRoot) return false

        val wordNode = getOpenNode(TtmlNode.NODE_WORD)
        if (wordNode == null) {
            this.background = true
        }
        return background
    }

    fun closeBackground(): Boolean {
        if (openTranslation?.background(false) == true ||
            openTransliteration?.background(false) == true)
            return true

        if (!hasRoot) return false

        val wordNode = getOpenNode(TtmlNode.NODE_WORD)
        if (wordNode == null) {
            this.background = false
        }
        return !background
    }

    fun createTransliteration(language: String?): TtmlTransliteration? {
        val transliterationType = TtmlAccompaniment.Type.Transliteration(language)
        val openTransliteration = getOpenAccompaniment<TtmlTransliteration>(transliterationType)
        if (openTransliteration == null) {
            val transliteration = TtmlTransliteration(transliterationType)
            accompaniments[transliterationType] = transliteration
            lastTransliterationType = transliterationType
            return transliteration
        }
        return null
    }

    fun createTranslation(language: String, inLine: Boolean = false): TtmlTranslation? {
        val translationType = TtmlAccompaniment.Type.Translation(language)
        val openTranslation = getOpenAccompaniment<TtmlTranslation>(translationType)
        if (openTranslation == null && language.isNotEmpty()) {
            val translation = TtmlTranslation(translationType, inLine)
            accompaniments[translationType] = translation
            lastTranslationType = translationType
            return translation
        }
        return null
    }

    fun prepareTranslationForCurrentLine(lang: String?): Boolean {
        if (lang == null)
            return false

        val openLine = getOpenNode(TtmlNode.NODE_LINE)
        if (openLine != null && openLine.key != null) {
            var openTranslation = this.openTranslation
            if (openTranslation == null || openTranslation.type.lang != lang) {
                openTranslation = getOpenAccompaniment(TtmlAccompaniment.Type.Translation(lang))
            }
            if (openTranslation == null) {
                openTranslation = createTranslation(lang, inLine = true)
            }
            return openTranslation?.prepare(openLine.key) == true
        }
        return false
    }

    fun finishTranslationForCurrentLine(): Boolean {
        val openLine = getOpenNode(TtmlNode.NODE_LINE)
        if (openLine != null && openLine.key != null) {
            return openTranslation?.finish() == true
        }
        return false
    }

    fun prepareAccompanimentText(key: String) =
        accompaniments.values.lastOrNull()?.prepare(key) == true

    fun finishAccompanimentText() =
        accompaniments.values.lastOrNull()?.finish() == true

    fun closeAccompaniment() =
        accompaniments.values.lastOrNull()?.close() == true

    fun closeNode(type: Int): Boolean {
        if (type == TtmlNode.NODE_WORD && isInTransliteration) {
            isInTransliteration = false
            return true
        }

        if (!hasRoot) return false

        val openNode = getOpenNode(type)
        if (openNode != null) {
            val closed = openNode.close()
            openNodes.remove(type)
            if (openNode.type == TtmlNode.NODE_BODY) {
                translations.filter { it.isInLine }
                    .forEach { it.close() }
            }
            return closed
        }
        return false
    }

    fun close(): Boolean {
        val rootNode = this.rootNode
        if (rootNode == null || closed) return false

        this.closed = true
        openNodes.clear()
        lastTranslationType = null
        lastTransliterationType = null
        return rootNode.close()
    }

    fun toLyrics(trackLength: Long): SyncedLyrics? {
        checkNotNull(rootNode) { "The node tree does not have a root" }
        check(closed) { "The node tree must be closed to obtain nested data" }

        val duration = rootNode!!.dur.takeIf { it > -1 } ?: trackLength
        val sectionNodes = rootNode!!.getChildren(TtmlNode.NODE_SECTION)
        val lineNodes = sectionNodes.flatMap { it.getChildren(TtmlNode.NODE_LINE) }.sortedBy { it.begin }
        val translation = getClosedTranslation()
        val transliteration = getClosedTransliteration()

        if (lineNodes.isNotEmpty()) {
            val lines = mutableListOf<SyncedLyrics.Line>()
            val lastLineIndex = lineNodes.lastIndex

            var lastLateralActor: LyricsActor = LyricsActor.Voice1
            var lastAgentId: String? = null
            var lateralDefined = false

            for (i in lineNodes.indices) {
                val line = lineNodes[i]

                val actor: LyricsActor? = line.agent?.let { agent ->
                    when (agent.type) {
                        TtmlAgent.Type.Person -> {
                            if (lastAgentId == null) {
                                lastLateralActor = LyricsActor.Voice1
                                lateralDefined = true
                            } else if (agent.id != lastAgentId) {
                                lastLateralActor = if (lastLateralActor == LyricsActor.Voice1)
                                    LyricsActor.Voice2
                                else LyricsActor.Voice1
                            }
                            lastAgentId = agent.id
                            lastLateralActor
                        }

                        TtmlAgent.Type.Other -> {
                            if (!lateralDefined) {
                                lastLateralActor = LyricsActor.Voice2
                                lateralDefined = true
                            } else {
                                lastLateralActor = if (lastLateralActor == LyricsActor.Voice1)
                                    LyricsActor.Voice2
                                else LyricsActor.Voice1
                            }
                            lastAgentId = agent.id
                            lastLateralActor
                        }

                        TtmlAgent.Type.Group -> LyricsActor.Group
                    }
                }

                if (line.end == -1L) {
                    line.end = (if (i < lastLineIndex) lineNodes[i + 1].begin else duration)
                }
                if (line.dur == -1L) {
                    line.dur = (line.end - line.begin)
                }
                if (line.text.isNullOrBlank()) {
                    val wordNodes = line.getChildren(TtmlNode.NODE_WORD).sortedBy { it.begin }
                    val words = nodesToWords(line, wordNodes, actor)

                    lines.add(
                        createSyncedLine(
                            line = line,
                            transliteration = transliteration,
                            translation = translation,
                            mainContent = wordsToTextContent(words),
                            actor = actor
                        )
                    )
                } else {
                    lines.add(
                        createSyncedLine(
                            line = line,
                            transliteration = transliteration,
                            translation = translation,
                            mainContent = SyncedLyrics.TextContent(
                                content = line.text.orEmpty(),
                                backgroundContent = null,
                                rawContent = null,
                                syllables = emptyList()
                            ),
                            actor = actor
                        )
                    )
                }
            }

            val linesWithOffset = lines
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

            return SyncedLyrics(linesWithOffset)
        }
        return null
    }

    private fun createSyncedLine(
        line: TtmlNode,
        transliteration: TtmlTransliteration?,
        translation: TtmlTranslation?,
        mainContent: SyncedLyrics.TextContent,
        actor: LyricsActor?
    ): SyncedLyrics.Line {
        fun resolveAccompaniment(acc: TtmlAccompaniment?): SyncedLyrics.TextContent? =
            accompanimentToTextContent(line, acc, actor)?.let { content ->
                val resolved = if (content.backgroundContent == mainContent.backgroundContent) {
                    content.copy(
                        backgroundContent = null,
                        syllables = content.syllables.filterNot { it.isBackground }
                    )
                } else content

                resolved.takeUnless { it.content == mainContent.content && it.backgroundContent == null }
            }

        return SyncedLyrics.Line(
            start = line.begin,
            end = line.end,
            duration = line.dur,
            content = mainContent,
            transliteration = resolveAccompaniment(transliteration),
            translation = resolveAccompaniment(translation),
            actor = actor
        )
    }

    private fun nodesToWords(
        line: TtmlNode,
        wordNodes: List<TtmlNode>,
        actor: LyricsActor?
    ): List<SyncedLyrics.Word> {
        val words = mutableListOf<SyncedLyrics.Word>()
        if (wordNodes.isNotEmpty()) {
            val lastWordIndex = wordNodes.lastIndex
            for (j in wordNodes.indices) {
                val word = wordNodes[j]
                if (word.end == -1L) {
                    word.end = (if (j < lastWordIndex) wordNodes[j + 1].begin else line.end)
                }
                if (word.dur == -1L) {
                    word.dur = (word.end - word.begin)
                }
                val text = word.text.orEmpty()
                val startIndex = words.filter { it.isBackground == word.background }
                    .sumOf { it.content.length }
                val endIndex = startIndex + (text.length - 1)
                words.add(
                    SyncedLyrics.Word(
                        content = text,
                        start = word.begin,
                        startIndex = startIndex,
                        end = word.end,
                        endIndex = endIndex,
                        duration = word.dur,
                        actor = actor?.asBackground(word.background)
                    )
                )
            }
        }
        return words
    }

    private fun wordsToTextContent(
        syllables: List<SyncedLyrics.Word>
    ): SyncedLyrics.TextContent {
        val blankSpace = "\\s{2,}".toRegex()
        val content = syllables.filterNot { it.isBackground }
            .joinToString("") { it.content.replace(blankSpace, " ") }
            .trim()

        val backgroundContent = syllables.filter { it.isBackground }
            .joinToString("") { it.content.replace(blankSpace, " ") }
            .trim()

        return SyncedLyrics.TextContent(
            content = content,
            backgroundContent = backgroundContent,
            rawContent = null,
            syllables = syllables
        )
    }

    private fun accompanimentToTextContent(
        line: TtmlNode,
        accompaniment: TtmlAccompaniment?,
        actor: LyricsActor?
    ): SyncedLyrics.TextContent? {
        if (accompaniment == null) return null

        val text = accompaniment[line.key]
        if (text != null) {
            if (text.syllables.isNotEmpty()) {
                val words = nodesToWords(line, text.syllables, actor)
                return wordsToTextContent(words)
            } else {
                return SyncedLyrics.TextContent(
                    content = text.content,
                    backgroundContent = text.backgroundContent,
                    rawContent = null,
                    syllables = emptyList()
                )
            }
        }
        return null
    }
}