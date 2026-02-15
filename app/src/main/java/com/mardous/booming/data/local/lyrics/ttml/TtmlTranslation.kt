package com.mardous.booming.data.local.lyrics.ttml

import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.extensions.utilities.collapseSpaces
import java.util.Locale

class TtmlTranslation(val lang: String, val isInLine: Boolean) {

    private val translatedTexts = mutableSetOf<TranslationText>()
    private var pending: TranslationText? = null

    private val isPendingTranslation: Boolean
        get() = pending != null && pending!!.pending

    var closed = false
        private set

    fun matchesLocale(suggestedLocale: Locale): Boolean {
        val translationLocale = Locale.forLanguageTag(lang)
        return translationLocale == suggestedLocale ||
                translationLocale.language == suggestedLocale.language
    }

    fun prepare(key: String): Boolean {
        if (closed || isPendingTranslation || translatedTexts.any { it.key == key })
            return false

        pending = TranslationText(key)
        return true
    }

    fun translate(content: String?): Boolean {
        if (closed || !isPendingTranslation)
            return false

        return pending?.setContent(content) == true
    }

    fun background(background: Boolean): Boolean {
        if (closed || !isPendingTranslation)
            return false

        if (background != pending!!.background) {
            pending!!.background = background
            return true
        }
        return false
    }

    fun finish(): Boolean {
        if (isPendingTranslation) {
            translatedTexts.add(TranslationText(pending!!))
            pending?.pending = false
            return true
        }
        return false
    }

    operator fun get(key: String?) =
        if (closed) translatedTexts.firstOrNull { it.key == key }?.getTextContent() else null

    fun close(): Boolean {
        if (closed || isPendingTranslation)
            return false

        closed = true
        pending = null
        return true
    }

    override fun equals(other: Any?): Boolean {
        return other is TtmlTranslation && other.lang == lang
    }

    override fun hashCode(): Int {
        var result = closed.hashCode()
        result = 31 * result + lang.hashCode()
        result = 31 * result + translatedTexts.hashCode()
        result = 31 * result + (pending?.hashCode() ?: 0)
        result = 31 * result + isPendingTranslation.hashCode()
        return result
    }

    override fun toString(): String {
        return "TtmlTranslation{lang='$lang', translatedTexts=${translatedTexts.size}}"
    }

    private class TranslationText(
        val key: String,
        var content: String?,
        var backgroundContent: String?,
        var pending: Boolean
    ) {

        var background: Boolean = false

        private var hasSetContent = false
        private var hasSetBackgroundContent = false

        constructor(key: String) : this(key, null, null, true)

        constructor(pending: TranslationText) : this(
            pending.key,
            pending.content,
            pending.backgroundContent,
            false
        )

        fun setContent(content: String?): Boolean {
            if (!pending) return false
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

        fun getTextContent(): Lyrics.TextContent {
            return Lyrics.TextContent(
                content = content.orEmpty(),
                backgroundContent = backgroundContent,
                rawContent = null,
                words = emptyList()
            )
        }

        override fun equals(other: Any?): Boolean {
            return other is TranslationText && other.key == key
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + content.hashCode()
            result = 31 * result + backgroundContent.hashCode()
            return result
        }

        override fun toString(): String {
            return "TranslatedText{" +
                    "key='$key', " +
                    "content='$content', " +
                    "backgroundContent='$backgroundContent', " +
                    "pending=$pending" +
                    "}"
        }
    }
}