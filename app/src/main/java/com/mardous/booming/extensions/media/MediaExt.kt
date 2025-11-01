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

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore.Audio.AlbumColumns
import android.provider.MediaStore.MediaColumns
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import com.mardous.booming.R
import com.mardous.booming.extensions.hasQ
import com.mardous.booming.extensions.plurals
import com.mardous.booming.util.FileUtil
import java.io.File

fun createAlbumArtThumbFile(): File =
    File(
        FileUtil.thumbsDirectory(),
        "Thumb_%d".format(System.currentTimeMillis())
    )

fun ContentResolver.insertAlbumArt(albumId: Long, path: String) {
    val artworkUri = "content://media/external/audio/albumart".toUri()
    delete(ContentUris.withAppendedId(artworkUri, albumId), null, null)
    if (!hasQ()) {
        // On Android Q+, this throws an IllegalArgumentException
        // as modifying the _data column is not longer allowed
        val values = contentValuesOf(
            AlbumColumns.ALBUM_ID to albumId,
            MediaColumns.DATA to path
        )
        insert(artworkUri, values)
    }
    notifyChange(artworkUri, null)
}

fun ContentResolver.deleteAlbumArt(albumId: Long) {
    val localUri = "content://media/external/audio/albumart".toUri()
    delete(ContentUris.withAppendedId(localUri, albumId), null, null)
    notifyChange(localUri, null)
}

/**
 * iTunes uses for example 1002 for track 2 CD1 or 3011 for track 11 CD3.
 * this method converts those values to normal track numbers
 */
fun Int.asReadableTrackNumber(): Int = this % 1000

fun Int.asNumberOfSongs(context: Context): String = context.plurals(R.plurals.x_songs, this)

fun Int.asNumberOfTimes(context: Context): String {
    return if (this <= 0) {
        context.getString(R.string.label_never)
    } else context.plurals(R.plurals.x_times, this)
}

fun Long.asAlbumCoverUri(): Uri =
    ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), this)

fun Long.asReadableDuration(readableFormat: Boolean = false): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        if (readableFormat) {
            "%d h %d m %d s".format(hours, minutes, seconds)
        } else {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        }
    } else {
        if (readableFormat) {
            "%d m %d s".format(minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}

fun String?.asSectionName(): String {
    if (this.isNullOrEmpty())
        return ""

    val pronouns = arrayOf("the ", "an ", "a ")
    var title = this.trim().lowercase()
    for (pronoun in pronouns) {
        if (title.startsWith(pronoun)) {
            title = title.substring(pronoun.length)
            break
        }
    }
    return if (title.isEmpty()) "" else title[0].toString().lowercase()
}