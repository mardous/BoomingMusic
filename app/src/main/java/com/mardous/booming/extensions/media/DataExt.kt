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
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.Settings
import android.text.SpannableString
import android.util.Log
import androidx.core.text.buildSpannedString
import com.mardous.booming.R
import com.mardous.booming.core.model.WebSearchEngine
import com.mardous.booming.data.local.ReplayGainTagExtractor
import com.mardous.booming.data.local.room.PlaylistEntity
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.plurals
import com.mardous.booming.extensions.resources.textColorPrimary
import com.mardous.booming.extensions.resources.textColorSecondary
import com.mardous.booming.extensions.resources.toForegroundColorSpan
import com.mardous.booming.extensions.showToast
import com.mardous.booming.extensions.utilities.DEFAULT_INFO_DELIMITER
import com.mardous.booming.extensions.utilities.buildInfoString
import java.util.Locale

fun Album.isArtistNameUnknown() = albumArtistName().isArtistNameUnknown()

fun Album.albumArtistName() = if (albumArtistName.isNullOrBlank()) artistName else albumArtistName!!

fun Album.displayArtistName() = albumArtistName().displayArtistName()

fun Album.albumInfo(): String = when {
    year > 0 -> buildInfoString(displayArtistName(), year.toString())
    else -> displayArtistName()
}

fun Album.songCountStr(context: Context) = songCount.asNumberOfSongs(context)

fun Artist.artistInfo(context: Context): String {
    return if (albumCount > 0) {
        buildInfoString(albumCountStr(context), songCountStr(context))
    } else {
        buildInfoString(songCountStr(context))
    }
}

fun Artist.albumCountStr(context: Context) = context.plurals(R.plurals.x_albums, albumCount)

fun Artist.songCountStr(context: Context): String = songCount.asNumberOfSongs(context)

fun Artist.displayName() = name.displayArtistName()

fun List<Song>.getSpannedTitles(context: Context): List<CharSequence> {
    val primaryColorSpan = context.textColorPrimary().toForegroundColorSpan()
    val secondaryColorSpan = context.textColorSecondary().toForegroundColorSpan()

    return map {
        buildSpannedString {
            append(SpannableString(it.title).also { title ->
                title.setSpan(primaryColorSpan, 0, title.length, 0)
            })
            append(DEFAULT_INFO_DELIMITER)
            append(SpannableString(it.displayArtistName()).also { artistName ->
                artistName.setSpan(secondaryColorSpan, 0, artistName.length, 0)
            })
        }
    }
}

fun List<Song>.playlistInfo(context: Context) = buildInfoString(songCountStr(context), songsDurationStr())

fun List<Song>.songsDurationStr() = sumOf { it.duration }.asReadableDuration()

fun List<Song>.songCountStr(context: Context) = size.asNumberOfSongs(context)

fun List<Song>.indexOfSong(songId: Long): Int = indexOfFirst { song -> song.id == songId }

fun Song.isArtistNameUnknown() = artistName.isArtistNameUnknown()

fun Song.displayArtistName() = artistName.displayArtistName()

fun Song.albumArtistName() = if (albumArtistName.isNullOrBlank()) artistName else albumArtistName!!

fun Song.songDurationStr() = duration.asReadableDuration()

fun Song.searchQuery(engine: WebSearchEngine): String {
    val searchQuery = when (engine) {
        WebSearchEngine.Google, WebSearchEngine.YouTube ->
            if (isArtistNameUnknown()) title else "$artistName $title"

        WebSearchEngine.LastFm, WebSearchEngine.Wikipedia ->
            if (isArtistNameUnknown()) title else if (albumArtistName.isNullOrEmpty()) artistName.toAlbumArtistName() else albumArtistName!!
    }
    return engine.getURLForQuery(searchQuery)
}

fun Song.songInfo(): String {
    return buildInfoString(songDurationStr(), displayArtistName())
}

fun Song.replayGainStr(context: Context): String? {
    val rg = ReplayGainTagExtractor.getReplayGain(uri)
    val builder = StringBuilder()
    if (rg.trackGain != 0f) {
        builder.append(String.format(Locale.ROOT, "%s: %.2f dB", context.getString(R.string.track), rg.trackGain))
    }
    if (rg.albumGain != 0f) {
        if (builder.isNotEmpty())
            builder.append(" - ")

        builder.append(String.format(Locale.ROOT, "%s: %.2f dB", context.getString(R.string.album), rg.albumGain))
    }
    val replayGainValues = builder.toString()
    return replayGainValues.ifEmpty { null }
}

fun Song.configureRingtone(context: Context, isAlarm: Boolean): Boolean {
    val resolver = context.contentResolver
    val uri = uri

    try {
        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns.TITLE),
            "${BaseColumns._ID}=?",
            arrayOf(id.toString()),
            null
        ).use { cursor ->
            if (cursor != null && cursor.count == 1) {
                cursor.moveToFirst()

                Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString())
                if (isAlarm) {
                    Settings.System.putString(resolver, Settings.System.ALARM_ALERT, uri.toString())
                }

                context.showToast(
                    if (isAlarm) {
                        context.getString(R.string.x_has_been_set_as_ringtone_and_as_alarm, cursor.getString(0))
                    } else context.getString(R.string.x_has_been_set_as_ringtone, cursor.getString(0))
                )
            }
        }
    } catch (e: SecurityException) {
        Log.e("SongExt", "Couldn't set the ringtone...", e)
        return false
    }
    return true
}

fun PlaylistEntity.isFavorites(context: Context) = playlistName == context.getString(R.string.favorites_label)