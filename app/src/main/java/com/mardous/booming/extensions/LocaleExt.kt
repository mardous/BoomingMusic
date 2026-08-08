package com.mardous.booming.extensions

import android.content.Context
import android.icu.util.ULocale
import androidx.core.app.LocaleManagerCompat
import com.mardous.booming.util.AUTO_LANGUAGE
import java.util.Locale

// The tag we apply is not always the one that names a language best
private val displayTagOverrides = mapOf(
    "es-US" to "es-419",
    "zh-CN" to "zh-Hans",
    "zh-TW" to "zh-Hant"
)

private const val REGIONLESS_FLAG = "🌎"
private const val SYSTEM_LANGUAGE_FLAG = "🌐"
private const val REGIONAL_INDICATOR_A = 0x1F1E6

class LanguageLabel(val flag: String, val name: String)

fun Context.languageLabelOf(tag: String): LanguageLabel {
    if (tag == AUTO_LANGUAGE) {
        val language = LocaleManagerCompat.getSystemLocales(this)[0]?.language.orEmpty()
        return LanguageLabel(SYSTEM_LANGUAGE_FLAG, language.languageEndonym())
    }
    return LanguageLabel(tag.languageFlagEmoji(), tag.languageNameInEnglish())
}

private val String.displayLocale: Locale
    get() = Locale.forLanguageTag(displayTagOverrides[this] ?: this)

fun String.languageEndonym(): String {
    val locale = displayLocale
    val endonym = locale.getDisplayName(locale)
    if (endonym.equals(locale.toLanguageTag(), ignoreCase = true)) {
        return languageNameInEnglish()
    }
    return endonym.replaceFirstChar { it.titlecase(locale) }
}

fun String.languageNameInEnglish(): String = displayLocale.displayName(Locale.ENGLISH)

fun String.languageFlagEmoji(): String {
    val uLocale = ULocale.forLocale(displayLocale)
    val country = uLocale.country.ifEmpty { ULocale.addLikelySubtags(uLocale).country }
    if (country.length != 2 || !country.all { it in 'A'..'Z' }) {
        return REGIONLESS_FLAG
    }
    return buildString {
        for (char in country) {
            appendCodePoint(REGIONAL_INDICATOR_A + (char - 'A'))
        }
    }
}

private fun Locale.displayName(inLocale: Locale = this): String =
    getDisplayName(inLocale).replaceFirstChar { it.titlecase(inLocale) }
