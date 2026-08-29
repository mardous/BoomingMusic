/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.mardous.booming.playback.library

import android.app.SearchManager
import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log

object SearchQueryProvider {

    // https://developer.android.com/media/implement/assistant#declare_legacy_support_for_voice_actions
    // https://android-developers.googleblog.com/2010/09/supporting-new-music-voice-action.html
    // https://developer.android.com/guide/components/intents-common#PlaySearch
    @Suppress("DEPRECATION")
    fun handleSearchIntent(intent: Intent, onQuery: (String?, Bundle) -> Unit) {
        var focus = intent.getStringExtra(MediaStore.EXTRA_MEDIA_FOCUS)
            ?: ContentResolver.ANY_CURSOR_ITEM_TYPE

        if (focus != ContentResolver.ANY_CURSOR_ITEM_TYPE &&
            focus != MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE &&
            focus != MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE &&
            focus != MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE &&
            focus != MediaStore.Audio.Media.ENTRY_CONTENT_TYPE &&
            focus != MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE
        ) {
            Log.w("SearchProvider", "Unsupported media focus: $focus")
            focus = ContentResolver.ANY_CURSOR_ITEM_TYPE
        }

        val mainQuery: String?
        val subQueries = Bundle()
        subQueries.putString(MediaStore.EXTRA_MEDIA_FOCUS, focus)
        when (focus) {
            MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE -> {
                mainQuery = intent.getStringExtra(MediaStore.EXTRA_MEDIA_GENRE)
                    ?: intent.getStringExtra(SearchManager.QUERY)
            }

            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                mainQuery = intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST)
                    ?: intent.getStringExtra(SearchManager.QUERY)
                intent.getStringExtra(MediaStore.EXTRA_MEDIA_GENRE)?.let {
                    subQueries.putString(MediaStore.EXTRA_MEDIA_GENRE, it)
                }
            }

            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                mainQuery = intent.getStringExtra(MediaStore.EXTRA_MEDIA_ALBUM)
                    ?: intent.getStringExtra(SearchManager.QUERY)
                intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST)?.let {
                    subQueries.putString(MediaStore.EXTRA_MEDIA_ARTIST, it)
                }
                intent.getStringExtra(MediaStore.EXTRA_MEDIA_GENRE)?.let {
                    subQueries.putString(MediaStore.EXTRA_MEDIA_GENRE, it)
                }
            }

            MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> {
                mainQuery = intent.getStringExtra(MediaStore.EXTRA_MEDIA_TITLE)
                    ?: intent.getStringExtra(SearchManager.QUERY)
                intent.getStringExtra(MediaStore.EXTRA_MEDIA_ALBUM)?.let {
                    subQueries.putString(MediaStore.EXTRA_MEDIA_ALBUM, it)
                }
                intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST)?.let {
                    subQueries.putString(MediaStore.EXTRA_MEDIA_ARTIST, it)
                }
                intent.getStringExtra(MediaStore.EXTRA_MEDIA_GENRE)?.let {
                    subQueries.putString(MediaStore.EXTRA_MEDIA_GENRE, it)
                }
            }

            MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE -> {
                mainQuery = intent.getStringExtra(MediaStore.EXTRA_MEDIA_PLAYLIST)
                    ?: intent.getStringExtra(SearchManager.QUERY)
                intent.getStringExtra(MediaStore.EXTRA_MEDIA_TITLE)?.let {
                    subQueries.putString(MediaStore.EXTRA_MEDIA_TITLE, it)
                }
                intent.getStringExtra(MediaStore.EXTRA_MEDIA_ALBUM)?.let {
                    subQueries.putString(MediaStore.EXTRA_MEDIA_ALBUM, it)
                }
                intent.getStringExtra(MediaStore.EXTRA_MEDIA_ARTIST)?.let {
                    subQueries.putString(MediaStore.EXTRA_MEDIA_ARTIST, it)
                }
                intent.getStringExtra(MediaStore.EXTRA_MEDIA_GENRE)?.let {
                    subQueries.putString(MediaStore.EXTRA_MEDIA_GENRE, it)
                }
            }

            else -> mainQuery = intent.getStringExtra(SearchManager.QUERY)
        }
        onQuery(mainQuery, subQueries)
    }
}