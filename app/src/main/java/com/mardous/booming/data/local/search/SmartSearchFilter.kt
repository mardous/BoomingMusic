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
import com.mardous.booming.data.local.MediaQueryDispatcher
import com.mardous.booming.data.local.repository.RealAlbumRepository
import com.mardous.booming.data.local.repository.RealSongRepository
import com.mardous.booming.data.model.search.FilterSelection
import com.mardous.booming.data.SearchFilter
import com.mardous.booming.data.model.search.SearchQuery
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Parcelize
class SmartSearchFilter internal constructor(
    private val name: String,
    private val projection: Array<String>?,
    private vararg val selections: FilterSelection,
) : SearchFilter, Parcelable, KoinComponent {

    override fun getName(): CharSequence {
        return name
    }

    override fun getCompatibleModes(): List<SearchQuery.FilterMode> {
        return selections.map { it.mode }
    }

    /**
     * Parse the input query using the appropriate operator for the given mode and return a set
     * of results according to the filter preferences.
     *
     * @param searchMode Specifies the mode that the user has selected to search using this filter.
     * This must be one of the explicitly declared as "operable" by this search filter through its
     * operators list. In other case, an exception will be thrown.
     * @param query What the user is searching.
     */
    override suspend fun getResults(searchMode: SearchQuery.FilterMode, query: String): List<Any> {
        // Check if we can use the specified SearchMode.
        val selectingWith = selections.firstOrNull { it.mode == searchMode }
        checkNotNull(selectingWith) { "Unsupported search mode: $searchMode" }

        val queryDispatcher = MediaQueryDispatcher()
        if (projection == null) {
            queryDispatcher.setProjection(RealSongRepository.getBaseProjection())
        } else {
            queryDispatcher.setProjection(projection)
        }

        // First, add filter selection and arguments
        queryDispatcher.addSelection(selectingWith.selection)
        queryDispatcher.addArguments(*selectingWith.arguments)

        // Now, we add our selection based on the filter type.
        // In other words, this is the magic thing that will filter results.
        queryDispatcher.addSelection("${selectingWith.column} LIKE ?")
        queryDispatcher.addArguments("%$query%")

        val songRepository: RealSongRepository = get()
        val songs = songRepository.makeSongCursor(queryDispatcher).let {
            songRepository.songs(it)
        }
        return when (selectingWith.mode) {
            SearchQuery.FilterMode.Albums -> get<RealAlbumRepository>().splitIntoAlbums(songs, false)
            else -> songs
        }
    }
}