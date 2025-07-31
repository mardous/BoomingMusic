package com.mardous.booming.fragments

import androidx.core.view.WindowInsetsCompat
import com.mardous.booming.extensions.getBottomInsets

data class LibraryMargin(
    val margin: Int,
    private val additionalSpace: Int = 0,
    private val insets: WindowInsetsCompat? = null
) {
    val totalMargin = getWithSpace(includeInsets = false)

    val bottomInsets get() = insets.getBottomInsets()

    fun getWithSpace(moreSpace: Int = 0, includeInsets: Boolean = true) =
        margin + additionalSpace + moreSpace + if (includeInsets) bottomInsets else 0
}