package com.mardous.booming.core.model

data class LibraryMargin(
    val margin: Int,
    private val additionalSpace: Int = 0,
    private val bottomInsets: Int = 0
) {
    val totalMargin = getWithSpace(includeInsets = false)

    fun getWithSpace(moreSpace: Int = 0, includeInsets: Boolean = true) =
        margin + additionalSpace + moreSpace + if (includeInsets) bottomInsets else 0
}