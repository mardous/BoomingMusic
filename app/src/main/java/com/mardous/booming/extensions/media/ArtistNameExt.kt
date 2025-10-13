package com.mardous.booming.extensions.media

import com.mardous.booming.R
import com.mardous.booming.appContext
import com.mardous.booming.data.model.Artist
import java.util.regex.Pattern

/**
 * A pattern that matches any artist name containing some sequences like "feat.", "ft.", "featuring"
 * and/or other special characters like "/" and ";".
 */
private val artistNamePattern = Pattern.compile("(.*)([(?\\s](feat(uring)?|ft)\\.? |\\s?[/;]\\s?)(.*)")

fun String.toAlbumArtistName(): String {
    val matcher = artistNamePattern.matcher(lowercase())
    if (matcher.matches()) {
        val goIndex = matcher.start(2)
        return this.substring(0, goIndex).trim()
    }
    return this
}

fun String.displayArtistName(): String =
    when {
        isArtistNameUnknown() -> appContext().getString(R.string.unknown_artist)
        isVariousArtists() -> appContext().getString(R.string.various_artists)
        else -> this
    }

internal fun String?.isVariousArtists(): Boolean {
    if (isNullOrBlank())
        return false
    if (this == Artist.VARIOUS_ARTISTS_DISPLAY_NAME)
        return true
    return false
}

internal fun String?.isArtistNameUnknown(): Boolean =
    if (isNullOrBlank()) false
    else trim().let { it.equals("unknown", true) || it.equals("<unknown>", true) }