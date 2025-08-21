package com.mardous.booming.data.local.lyrics.ttml

/**
 * Represents a single node in a TTML (Timed Text Markup Language) document.
 *
 * A node may correspond to structural elements (<body>, <div>, <p>, <span>, etc.)
 * or metadata elements (<head>, <metadata>, etc.). Each node can contain textual
 * content, timing attributes (begin, end, dur), style information, and nested child nodes.
 *
 * Key responsibilities:
 * - Store and expose timing information to determine when the node is active.
 * - Hold textual content and associated metadata such as agent and duration.
 * - Provide hierarchical structure via a list of child nodes.
 *
 * @author Christians Martínez A. (mardous)
 */
internal data class TtmlNode(
    val type: Int,
    val begin: Long,
    var end: Long,
    var dur: Long,
    val agent: TtmlAgent? = null
) {

    private val children = mutableListOf<TtmlNode>()

    var background: Boolean = false
        private set

    var text: String? = null
        private set

    var closed: Boolean = false
        private set

    fun getOpenChild(type: Int): TtmlNode? {
        if (closed) return null
        if (this.type == type) return this
        return children.firstNotNullOfOrNull { it.getOpenChild(type) }
    }

    fun getChildren(type: Int): List<TtmlNode> {
        if (closed) {
            return children.filter { it.type == type }
        }
        return emptyList()
    }

    fun setBackground(background: Boolean): Boolean {
        if (closed) return false
        if (this.type == NODE_WORD) {
            this.background = background
            return true
        }
        return false
    }

    fun setText(text: String?): Boolean {
        if (closed) return false
        if (this.type == NODE_LINE) {
            if (children.isEmpty()) {
                this.text = text
                return true
            } else {
                val trimmed = text?.trim()
                if (trimmed.isNullOrEmpty()) {
                    children[children.lastIndex].let { it.text = "${it.text} " }
                    return true
                }
            }
            return false
        }
        if (this.type == NODE_WORD) {
            this.text = text
            return true
        }
        return false
    }

    fun addChildNode(node: TtmlNode): Boolean {
        if (closed || node.closed) return false

        if (this.type == NODE_BODY && node.type == NODE_SECTION ||
            this.type == NODE_SECTION && node.type == NODE_LINE ||
            this.type == NODE_LINE && node.type == NODE_WORD
        ) {

            if (node.begin > -1) {
                return children.add(node)
            }

        }
        return false
    }

    fun close(): Boolean {
        if (!closed) {
            children.filterNot { it.closed }
                .forEach { it.close() }
            closed = true
            return true
        }
        return false
    }

    override fun toString(): String {
        val typeString = when (type) {
            NODE_BODY -> "NODE_BODY"
            NODE_SECTION -> "NODE_SECTION"
            NODE_LINE -> "NODE_LINE"
            NODE_WORD -> "NODE_WORLD"
            else -> "NODE_UNKNOWN"
        }
        return "TtmlNode{" +
                "type=$typeString, " +
                "begin=$begin, " +
                "end=$end, " +
                "dur=$dur, " +
                "background=$background, " +
                "agent=$agent, " +
                "text='$text', " +
                "closed=$closed" +
                "}"
    }

    companion object {
        const val NODE_UNKNOWN = -1
        const val NODE_BODY = 0
        const val NODE_SECTION = 1
        const val NODE_LINE = 2
        const val NODE_WORD = 3

        const val TAG_BODY = "body"
        const val TAG_DIV = "div"
        const val TAG_PARAGRAPH = "p"
        const val TAG_SPAN = "span"

        fun isSupportedTag(name: String?) =
            name == TAG_BODY || name == TAG_DIV || name == TAG_PARAGRAPH || name == TAG_SPAN

        fun buildBody(dur: Long): TtmlNode {
            return TtmlNode(type = NODE_BODY, begin = -1, end = -1, dur = dur)
        }

        fun buildSection(begin: Long, end: Long, dur: Long): TtmlNode {
            return TtmlNode(type = NODE_SECTION, begin = begin, end = end, dur = dur)
        }

        fun buildLine(begin: Long, end: Long, dur: Long, agent: TtmlAgent?): TtmlNode {
            return TtmlNode(type = NODE_LINE, begin = begin, end = end, dur = dur, agent = agent)
        }

        fun buildWord(begin: Long, end: Long, dur: Long): TtmlNode {
            return TtmlNode(type = NODE_WORD, begin = begin, end = end, dur = dur)
        }
    }
}