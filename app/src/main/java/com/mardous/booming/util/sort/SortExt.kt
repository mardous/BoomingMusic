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

package com.mardous.booming.util.sort

import android.view.Menu
import android.view.MenuItem
import com.mardous.booming.R
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.model.*
import com.mardous.booming.extensions.media.albumArtistName
import java.text.Collator
import java.util.Locale

private val collator: Collator by lazy {
    Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
}

private val articlesByLanguage = mapOf(
    "en" to listOf("the", "a", "an"),
    "es" to listOf("el", "la", "los", "las", "un", "una"),
    "fr" to listOf("le", "la", "les", "un", "une"),
    "de" to listOf("der", "die", "das", "ein", "eine"),
    "it" to listOf("il", "lo", "la", "l’", "i", "gli", "un", "una"),
    "pt" to listOf("o", "a", "os", "as", "um", "uma"),
    "nl" to listOf("de", "het", "een")
)

private fun String.stripLeadingArticles(language: String): String {
    val articles = articlesByLanguage[language] ?: return this
    val regex = Regex("^(${articles.joinToString("|")})\\s+", RegexOption.IGNORE_CASE)
    return trim().replace(regex, "")
}

private fun String.normalizeForSorting(language: String, ignoreArticles: Boolean): String {
    return if (ignoreArticles) stripLeadingArticles(language) else this
}

private fun <T> List<T>.applyOrder(descending: Boolean): List<T> =
    if (descending) this.reversed() else this

fun List<Song>.sortedSongs(sortOrder: SortOrder): List<Song> {
    return sortedSongs(sortOrder.value, sortOrder.isDescending, sortOrder.ignoreArticles)
}

fun List<Song>.sortedSongs(
    sortOrder: String,
    descending: Boolean,
    ignoreArticles: Boolean
): List<Song> {
    val langCode = Locale.getDefault().language
    val songs = when (sortOrder) {
        SortKeys.AZ -> sortedWith(compareBy(collator) {
            it.title.normalizeForSorting(langCode, ignoreArticles)
        })

        SortKeys.ARTIST -> sortedWith(compareBy(collator) {
            it.artistName.normalizeForSorting(langCode, ignoreArticles)
        })

        SortKeys.ALBUM -> sortedWith(
            Comparator.comparing<Song, String>(
                { it.albumName.normalizeForSorting(langCode, ignoreArticles) },
                collator
            ).thenComparingInt { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
        )

        SortKeys.TRACK_NUMBER -> sortedWith(compareBy { it.trackNumber })
        SortKeys.DURATION -> sortedWith(compareBy { it.duration })
        SortKeys.YEAR -> sortedWith(compareBy { it.year })
        SortKeys.DATE_ADDED -> sortedWith(compareBy { it.dateAdded })
        SortKeys.DATE_MODIFIED -> sortedWith(compareBy { it.dateModified })
        else -> this
    }
    return songs.applyOrder(descending)
}

fun List<Artist>.sortedArtists(sortOrder: SortOrder): List<Artist> {
    val langCode = Locale.getDefault().language
    val artists = when (sortOrder.value) {
        SortKeys.AZ -> sortedWith(compareBy(collator) {
            it.name.normalizeForSorting(langCode, sortOrder.ignoreArticles)
        })

        SortKeys.NUMBER_OF_SONGS -> sortedWith(compareBy { it.songCount })
        SortKeys.NUMBER_OF_ALBUMS -> sortedWith(compareBy { it.albumCount })
        else -> this
    }
    return artists.applyOrder(sortOrder.isDescending)
}

fun List<Album>.sortedAlbums(sortOrder: SortOrder): List<Album> {
    val langCode = Locale.getDefault().language
    val albums = when (sortOrder.value) {
        SortKeys.AZ -> sortedWith(compareBy(collator) {
            it.name.normalizeForSorting(langCode, sortOrder.ignoreArticles)
        })

        SortKeys.ARTIST -> sortedWith(compareBy(collator) {
            it.albumArtistName().normalizeForSorting(langCode, sortOrder.ignoreArticles)
        })

        SortKeys.YEAR -> sortedWith(compareBy { it.year })
        SortKeys.NUMBER_OF_SONGS -> sortedWith(compareBy { it.songCount })
        else -> this
    }
    return albums.applyOrder(sortOrder.isDescending)
}

fun List<Genre>.sortedGenres(sortOrder: SortOrder): List<Genre> {
    val langCode = Locale.getDefault().language
    val genres = when (sortOrder.value) {
        SortKeys.AZ -> sortedWith(compareBy(collator) {
            it.name.normalizeForSorting(langCode, sortOrder.ignoreArticles)
        })

        SortKeys.NUMBER_OF_SONGS -> sortedWith(compareBy { it.songCount })
        else -> this
    }
    return genres.applyOrder(sortOrder.isDescending)
}

fun List<ReleaseYear>.sortedYears(sortOrder: SortOrder): List<ReleaseYear> {
    val years = when (sortOrder.value) {
        SortKeys.YEAR -> sortedWith(compareBy { it.year })
        SortKeys.NUMBER_OF_SONGS -> sortedWith(compareBy { it.songCount })
        else -> this
    }
    return years.applyOrder(sortOrder.isDescending)
}

fun List<Folder>.sortedFolders(sortOrder: SortOrder): List<Folder> {
    val folders = when (sortOrder.value) {
        SortKeys.AZ -> sortedWith(compareBy { it.fileName })
        SortKeys.NUMBER_OF_SONGS -> sortedWith(compareBy { it.songCount })
        SortKeys.DATE_ADDED -> sortedWith(compareBy { it.fileDateAdded })
        SortKeys.DATE_MODIFIED -> sortedWith(compareBy { it.fileDateModified })
        else -> this
    }
    return folders.applyOrder(sortOrder.isDescending)
}

fun List<PlaylistWithSongs>.sortedPlaylists(sortOrder: SortOrder): List<PlaylistWithSongs> {
    val playlists = when (sortOrder.value) {
        SortKeys.AZ -> sortedWith(compareBy { it.playlistEntity.playlistName })
        SortKeys.NUMBER_OF_SONGS -> sortedWith(compareBy { it.songCount })
        // TODO SortKeys.DATE_MODIFIED
        else -> this
    }
    return playlists.applyOrder(sortOrder.isDescending)
}

fun Menu.prepareSortOrder(sortOrder: SortOrder) {
    val menu = findItem(R.id.action_sort_order)?.subMenu ?: this
    menu.findItem(R.id.action_sort_order_descending)?.apply {
        isCheckable = true
        isChecked = sortOrder.isDescending
    }
    menu.findItem(R.id.action_sort_order_ignore_articles)?.apply {
        isCheckable = true
        isChecked = sortOrder.ignoreArticles
    }
    when (sortOrder.value) {
        SortKeys.AZ -> menu.findItem(R.id.action_sort_order_az)?.isChecked = true
        SortKeys.ARTIST -> menu.findItem(R.id.action_sort_order_artist)?.isChecked = true
        SortKeys.ALBUM -> menu.findItem(R.id.action_sort_order_album)?.isChecked = true
        SortKeys.TRACK_NUMBER -> menu.findItem(R.id.action_sort_order_track_list)?.isChecked = true
        SortKeys.DURATION -> menu.findItem(R.id.action_sort_order_duration)?.isChecked = true
        SortKeys.YEAR -> menu.findItem(R.id.action_sort_order_year)?.isChecked = true
        SortKeys.DATE_ADDED -> menu.findItem(R.id.action_sort_order_date_added)?.isChecked = true
        SortKeys.DATE_MODIFIED -> menu.findItem(R.id.action_sort_order_date_modified)?.isChecked = true
        SortKeys.NUMBER_OF_SONGS -> menu.findItem(R.id.action_sort_order_number_of_songs)?.isChecked = true
        SortKeys.NUMBER_OF_ALBUMS -> menu.findItem(R.id.action_sort_order_number_of_albums)?.isChecked = true
    }
}

fun MenuItem.selectedSortOrder(sortOrder: SortOrder): Boolean {
    val handled = when (this.itemId) {
        R.id.action_sort_order_az -> {
            sortOrder.value = SortKeys.AZ
            isChecked = true
            true
        }

        R.id.action_sort_order_artist -> {
            sortOrder.value = SortKeys.ARTIST
            isChecked = true
            true
        }

        R.id.action_sort_order_album -> {
            sortOrder.value = SortKeys.ALBUM
            isChecked = true
            true
        }

        R.id.action_sort_order_track_list -> {
            sortOrder.value = SortKeys.TRACK_NUMBER
            isChecked = true
            true
        }

        R.id.action_sort_order_duration -> {
            sortOrder.value = SortKeys.DURATION
            isChecked = true
            true
        }

        R.id.action_sort_order_year -> {
            sortOrder.value = SortKeys.YEAR
            isChecked = true
            true
        }

        R.id.action_sort_order_date_added -> {
            sortOrder.value = SortKeys.DATE_ADDED
            isChecked = true
            true
        }

        R.id.action_sort_order_date_modified -> {
            sortOrder.value = SortKeys.DATE_MODIFIED
            isChecked = true
            true
        }

        R.id.action_sort_order_number_of_songs -> {
            sortOrder.value = SortKeys.NUMBER_OF_SONGS
            isChecked = true
            true
        }

        R.id.action_sort_order_number_of_albums -> {
            sortOrder.value = SortKeys.NUMBER_OF_ALBUMS
            isChecked = true
            true
        }

        R.id.action_sort_order_descending -> {
            sortOrder.isDescending = !isChecked
            isChecked = !isChecked
            true
        }

        R.id.action_sort_order_ignore_articles -> {
            sortOrder.ignoreArticles = !isChecked
            isChecked = !isChecked
            true
        }

        else -> false
    }
    return handled
}