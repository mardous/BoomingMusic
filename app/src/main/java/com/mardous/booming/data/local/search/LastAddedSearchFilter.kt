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

import com.mardous.booming.data.SearchFilter
import com.mardous.booming.data.local.repository.RealAlbumRepository
import com.mardous.booming.data.local.repository.RealArtistRepository
import com.mardous.booming.data.local.repository.SmartRepository
import com.mardous.booming.data.model.ContentType
import com.mardous.booming.data.model.search.SearchQuery.FilterMode
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @author Christians M. A. (mardous)
 */
@Parcelize
class LastAddedSearchFilter(private val name: CharSequence) : SearchFilter, KoinComponent {

    @IgnoredOnParcel
    private val smartRepository by inject<SmartRepository>()

    @IgnoredOnParcel
    private val albumRepository by inject<RealAlbumRepository>()

    @IgnoredOnParcel
    private val artistRepository by inject<RealArtistRepository>()

    override fun getName(): CharSequence {
        return name
    }

    override fun getCompatibleModes(): List<FilterMode> {
        return listOf(FilterMode.Songs, FilterMode.Albums, FilterMode.Artists)
    }

    override suspend fun getResults(searchMode: FilterMode, query: String): List<Any> {
        val contentType = when (searchMode) {
            FilterMode.Albums -> ContentType.RecentAlbums
            FilterMode.Artists -> ContentType.RecentArtists
            else -> ContentType.RecentSongs
        }
        val songs = smartRepository.recentSongs(query, contentType)
        return when (contentType) {
            ContentType.RecentAlbums -> albumRepository.splitIntoAlbums(songs, false)
            ContentType.RecentArtists -> artistRepository.splitIntoAlbumArtists(albumRepository.splitIntoAlbums(songs))
            else -> songs
        }
    }
}