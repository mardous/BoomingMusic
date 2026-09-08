/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.data.local.lyrics.ttml

import com.mardous.booming.extensions.utilities.collapseSpaces
import com.mardous.booming.extensions.utilities.isWhitespace

class TtmlTranslation(override val type: Type.Translation, val isInLine: Boolean) :
    TtmlAccompaniment(type)

class TtmlTransliteration(override val type: Type.Transliteration) :
    TtmlAccompaniment(type)

open class TtmlAccompaniment(open val type: Type) {

    protected val accompanistTexts = mutableSetOf<MutableAccompanistText>()
    protected var pending: MutableAccompanistText? = null
        private set

    protected val hasPendingContent: Boolean
        get() = pending != null && pending!!.pending

    var closed = false
        private set

    fun prepare(key: String): Boolean {
        if (closed || hasPendingContent || accompanistTexts.any { it.key == key })
            return false

        pending = MutableAccompanistText(key)
        return true
    }

    fun set(content: String?): Boolean {
        if (closed || !hasPendingContent)
            return false

        return pending?.setContent(content) == true
    }

    fun addWord(node: TtmlNode): Boolean {
        if (closed || !hasPendingContent)
            return false

        return pending?.addWord(node) == true
    }

    fun background(background: Boolean): Boolean {
        if (closed || !hasPendingContent)
            return false

        if (background != pending!!.background) {
            pending!!.background = background
            return true
        }
        return false
    }

    fun finish(): Boolean {
        if (hasPendingContent) {
            accompanistTexts.add(MutableAccompanistText(pending!!))
            pending?.pending = false
            return true
        }
        return false
    }

    operator fun get(key: String?) =
        if (closed) accompanistTexts.firstOrNull { it.key == key }?.let {
            AccompanistText(
                content = it.content.orEmpty(),
                backgroundContent = it.backgroundContent,
                syllables = it.syllables.orEmpty()
            )
        } else null

    fun close(): Boolean {
        if (closed || hasPendingContent)
            return false

        closed = true
        pending = null
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TtmlAccompaniment

        if (closed != other.closed) return false
        if (accompanistTexts != other.accompanistTexts) return false
        if (pending != other.pending) return false
        if (hasPendingContent != other.hasPendingContent) return false
        return type == other.type
    }

    override fun hashCode(): Int {
        var result = closed.hashCode()
        result = 31 * result + accompanistTexts.hashCode()
        result = 31 * result + (pending?.hashCode() ?: 0)
        result = 31 * result + hasPendingContent.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String {
        return "TtmlAccompaniment{" +
                "type='$type', " +
                "accompanimentTexts=${accompanistTexts.size}}, " +
                "pending=$pending, " +
                "hasPendingContent=$hasPendingContent, " +
                "closed=$closed" +
                "}"
    }

    sealed interface Type {
        data class Translation(val lang: String) : Type
        data class Transliteration(val lang: String?) : Type
    }

    class AccompanistText(
        val content: String,
        val backgroundContent: String?,
        val syllables: List<TtmlNode>,
    )

    protected class MutableAccompanistText(
        val key: String,
        var content: String? = null,
        var backgroundContent: String? = null,
        var syllables: MutableList<TtmlNode>? = null,
        var pending: Boolean = true
    ) {

        var background: Boolean = false

        private var hasSetContent = false
        private var hasSetBackgroundContent = false

        constructor(pending: MutableAccompanistText) : this(
            pending.key,
            pending.content,
            pending.backgroundContent,
            pending.syllables,
            false
        )

        fun setContent(content: String?): Boolean {
            if (!pending) return false

            val lastAddedWord = syllables?.lastOrNull()
            if (lastAddedWord != null && !lastAddedWord.closed) {
                val trimmedContent = content?.replace(Regex("\\s+"), " ")
                if (!lastAddedWord.text.isNullOrBlank() && trimmedContent.isWhitespace()) {
                    val result = lastAddedWord.setText("${lastAddedWord.text} ")
                    // We close the node to prevent it from receiving further modifications
                    lastAddedWord.close()
                    return result
                } else {
                    return lastAddedWord.setText(trimmedContent)
                }
            }

            if (background) {
                if (!hasSetBackgroundContent) {
                    this.backgroundContent = content?.collapseSpaces()
                    hasSetBackgroundContent = true
                    return true
                }
            } else {
                if (!hasSetContent) {
                    this.content = content?.collapseSpaces()
                    hasSetContent = true
                    return true
                }
            }
            return false
        }

        fun addWord(node: TtmlNode): Boolean {
            if (!pending || node.type != TtmlNode.NODE_WORD) return false

            if (syllables == null) {
                syllables = mutableListOf()
            }
            if (node.background != background) {
                node.setBackground(background)
            }
            return syllables?.add(node) == true
        }

        override fun equals(other: Any?): Boolean {
            return other is MutableAccompanistText && other.key == key
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + content.hashCode()
            result = 31 * result + backgroundContent.hashCode()
            result = 31 * result + syllables.hashCode()
            result = 31 * result + pending.hashCode()
            return result
        }
    }
}