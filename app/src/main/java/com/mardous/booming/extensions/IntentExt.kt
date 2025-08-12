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

package com.mardous.booming.extensions

import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import com.mardous.booming.R
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.data.model.Song

const val MIME_TYPE_AUDIO = "audio/*"
const val MIME_TYPE_IMAGE = "image/*"
const val MIME_TYPE_PLAIN_TEXT = "text/plain"
const val MIME_TYPE_APPLICATION = "application/*"

const val EXTRA_SONG = "extra_song"
const val EXTRA_SONGS = "extra_songs"
const val EXTRA_PLAYLISTS = "extra_playlists"

fun String.openWeb(): Intent =
    Intent(Intent.ACTION_VIEW, this.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun Context.getShareNowPlayingIntent(song: Song): Intent {
    val currentlyListening =
        getString(R.string.currently_listening_to_x_by_x, song.title, song.displayArtistName())
    return ShareCompat.IntentBuilder(this)
        .setType(MIME_TYPE_PLAIN_TEXT)
        .setText(currentlyListening)
        .createChooserIntent()
}

fun Context.getShareSongIntent(song: Song): Intent {
    return try {
        ShareCompat.IntentBuilder(this)
            .setType(MIME_TYPE_AUDIO)
            .setStream(song.mediaStoreUri)
            .setChooserTitle(R.string.action_share)
            .createChooserIntent()
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        showToast("Could not share this file.")
        Intent()
    }
}

fun Context.getShareSongsIntent(songs: List<Song>): Intent {
    if (songs.size == 1) {
        return getShareSongIntent(songs.first())
    } else if (songs.isNotEmpty()) {
        val intent = ShareCompat.IntentBuilder(this)
            .setType(MIME_TYPE_AUDIO)
            .setChooserTitle(R.string.action_share)
        for (song in songs.filterNot { it == Song.emptySong }) {
            intent.addStream(song.mediaStoreUri)
        }
        return intent.createChooserIntent()
    }
    return Intent()
}

fun Intent.toChooser(chooserTitle: CharSequence? = null): Intent =
    Intent.createChooser(this, chooserTitle)