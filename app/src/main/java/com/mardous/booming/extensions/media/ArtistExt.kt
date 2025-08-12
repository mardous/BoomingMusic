/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.mardous.booming.extensions.media

import android.content.Context
import com.mardous.booming.R
import com.mardous.booming.appContext
import com.mardous.booming.extensions.plurals
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.data.model.Artist
import java.util.regex.Pattern

fun String.displayArtistName(): String =
    when {
        isArtistNameUnknown() -> appContext().getString(R.string.unknown_artist)
        isVariousArtists() -> appContext().getString(R.string.various_artists)
        else -> this
    }

fun Artist.isNameUnknown() = name.isArtistNameUnknown()

fun Artist.artistInfo(context: Context): String {
    return if (albumCount > 0) {
        buildInfoString(albumCountStr(context), songCountStr(context))
    } else {
        buildInfoString(songCountStr(context))
    }
}

fun Artist.albumCountStr(context: Context) = context.plurals(R.plurals.x_albums, albumCount)

fun Artist.songCountStr(context: Context): String = songCount.songsStr(context)

fun Artist.displayName() = name.displayArtistName()

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