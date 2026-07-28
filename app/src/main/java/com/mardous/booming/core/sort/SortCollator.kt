package com.mardous.booming.core.sort

import android.icu.text.Collator
import android.icu.text.RuleBasedCollator
import com.mardous.booming.extensions.media.normalizeForSorting
import com.mardous.booming.util.Preferences
import java.util.Locale

fun sortingCollator(): Collator =
    Collator.getInstance(Locale.getDefault()).apply {
        strength = Collator.PRIMARY
        (this as? RuleBasedCollator)?.numericCollation = true
    }

fun <T> Iterable<T>.sortedByName(selector: (T) -> String): List<T> =
    sortedWith(compareBy(sortingCollator()) { selector(it) })

fun <T> Iterable<T>.sortedByMediaName(selector: (T) -> String): List<T> {
    val ignoreArticles = Preferences.ignoreArticlesWhenSorting
    val language = Locale.getDefault().language
    return sortedByName { selector(it).normalizeForSorting(ignoreArticles, language) }
}
