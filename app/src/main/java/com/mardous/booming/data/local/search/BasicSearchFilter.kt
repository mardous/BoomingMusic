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

package com.mardous.booming.data.local.search

import android.os.Parcelable
import com.mardous.booming.data.SearchFilter
import com.mardous.booming.data.local.repository.RealAlbumRepository
import com.mardous.booming.data.local.repository.SearchRepository
import com.mardous.booming.data.model.search.SearchQuery.FilterMode
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Serializable

/**
 * @author Christians M. A. (mardous)
 */
@Parcelize
class BasicSearchFilter<T : Serializable>(private val name: String, private val argument: Argument<T>) : SearchFilter,
    KoinComponent {

    @IgnoredOnParcel
    private val searchRepository: SearchRepository by inject()
    @IgnoredOnParcel
    private val albumRepository: RealAlbumRepository by inject()

    override fun getName(): CharSequence {
        return name
    }

    override fun getCompatibleModes(): List<FilterMode> {
        val modes = mutableListOf(FilterMode.Songs)
        if (argument.type == Argument.YEAR) {
            modes.add(FilterMode.Albums)
        }
        return modes
    }

    override suspend fun getResults(searchMode: FilterMode, query: String): List<Any> {
        return when (argument.type) {
            Argument.FOLDER -> {
                val folderPath = argument.value as? String ?: return emptyList()
                val songs = searchRepository.searchFolderSongs(folderPath, query)
                if (searchMode == FilterMode.Albums) {
                    albumRepository.splitIntoAlbums(songs, sorted = false)
                } else {
                    songs
                }
            }
            Argument.GENRE -> {
                val genreId = argument.value as? Long ?: return emptyList()
                searchRepository.searchGenreSongs(genreId, query)
            }
            Argument.YEAR -> {
                val year = argument.value as? Int ?: return emptyList()
                searchRepository.searchYearSongs(year, query)
            }
            Argument.PLAYLIST -> {
                val playlistId = argument.value as? Long ?: return emptyList()
                searchRepository.searchPlaylistSongs(playlistId, query)
            }
            else -> arrayListOf()
        }
    }

    @Parcelize
    class Argument<T : Serializable>(val value: T, val type: Int) : Parcelable {
        companion object {
            const val YEAR = 1
            const val GENRE = 2
            const val PLAYLIST = 3
            const val FOLDER = 4
        }
    }
}