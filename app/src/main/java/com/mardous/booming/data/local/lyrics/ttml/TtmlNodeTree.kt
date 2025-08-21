package com.mardous.booming.data.local.lyrics.ttml

import com.mardous.booming.data.model.lyrics.Lyrics

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
    private var openNodes = mutableMapOf<Int, TtmlNode?>()

    private var background = false
    private var closed = false

    val hasRoot: Boolean
        get() = rootNode?.type == TtmlNode.NODE_BODY && rootNode?.closed == false

    private fun getOpenNode(type: Int): TtmlNode? {
        if (!hasRoot || closed) return null

        return openNodes.getOrPut(type) {
            rootNode?.getOpenChild(type)
        }
    }

    fun addRoot(node: TtmlNode): Boolean {
        if (closed || node.closed) return false

        if (!hasRoot && node.type == TtmlNode.NODE_BODY) {
            rootNode = node
        }
        return hasRoot
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
        if (!hasRoot) return false

        val wordNode = getOpenNode(TtmlNode.NODE_WORD)
        if (wordNode == null) {
            this.background = true
        }
        return background
    }

    fun closeBackground(): Boolean {
        if (!hasRoot) return false

        val wordNode = getOpenNode(TtmlNode.NODE_WORD)
        if (wordNode == null) {
            this.background = false
        }
        return !background
    }

    fun closeNode(type: Int): Boolean {
        if (!hasRoot) return false

        val openNode = getOpenNode(type)
        if (openNode != null) {
            val closed = openNode.close()
            openNodes.remove(type)
            return closed
        }
        return false
    }

    fun close(): Boolean {
        val rootNode = this.rootNode
        if (rootNode == null || closed) return false

        this.closed = true
        openNodes.clear()
        return rootNode.close()
    }

    fun toLyrics(trackLength: Long): Lyrics? {
        checkNotNull(rootNode) { "The node tree does not have a root" }
        check(closed) { "The node tree must be closed to obtain nested data" }

        val duration = rootNode!!.dur.takeIf { it > -1 } ?: trackLength
        val sectionNodes = rootNode!!.getChildren(TtmlNode.NODE_SECTION)
        val lineNodes = sectionNodes.flatMap { it.getChildren(TtmlNode.NODE_LINE) }.sortedBy { it.begin }
        if (lineNodes.isNotEmpty()) {
            val lines = mutableListOf<Lyrics.Line>()
            val lastLineIndex = lineNodes.lastIndex
            for (i in lineNodes.indices) {
                val line = lineNodes[i]
                if (line.end == -1L) {
                    line.end = (if (i < lastLineIndex) lineNodes[i + 1].begin else duration)
                }
                if (line.dur == -1L) {
                    line.dur = (line.end - line.begin)
                }
                if (line.text.isNullOrBlank()) {
                    val words = mutableListOf<Lyrics.Word>()
                    val wordNodes = line.getChildren(TtmlNode.NODE_WORD).sortedBy { it.begin }
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
                                Lyrics.Word(
                                    content = text,
                                    startMillis = word.begin,
                                    startIndex = startIndex,
                                    endMillis = word.end,
                                    endIndex = endIndex,
                                    durationMillis = word.dur,
                                    isBackground = word.background
                                )
                            )
                        }
                    }
                    val content = words.filterNot { it.isBackground }.joinToString("") { it.content }
                    val backgroundContent = words.filter { it.isBackground }.joinToString("") { it.content }
                    lines.add(
                        Lyrics.Line(
                            startAt = line.begin,
                            end = line.end,
                            durationMillis = line.dur,
                            content = content,
                            backgroundContent = backgroundContent,
                            rawContent = content + "\n" + backgroundContent,
                            words = words,
                            actor = line.agent?.agent
                        )
                    )
                } else {
                    lines.add(
                        Lyrics.Line(
                            startAt = line.begin,
                            end = line.end,
                            durationMillis = line.dur,
                            content = line.text.orEmpty(),
                            backgroundContent = null,
                            rawContent = line.text.orEmpty(),
                            words = emptyList(),
                            actor = line.agent?.agent
                        )
                    )
                }
            }
            return Lyrics(
                title = null,
                artist = null,
                album = null,
                durationMillis = duration,
                lines = lines
            )
        }
        return null
    }
}