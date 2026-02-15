package com.mardous.booming.data.local.lyrics

class InstrumentalDetector(
    private val identifiers: Set<String>,
    private val markByTitle: Boolean,
    private val maxLength: Int
) {

    fun byTitle(title: String): Boolean =
        markByTitle && identifiers.any { title.contains(it, ignoreCase = true) }

    fun byLyrics(text: String?): Boolean =
        !text.isNullOrBlank() && text.length <= maxLength && identifiers.contains(text.trim())
}
