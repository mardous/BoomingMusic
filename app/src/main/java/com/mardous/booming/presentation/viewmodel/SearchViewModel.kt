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

package com.mardous.booming.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.core.model.action.SongClickBehavior
import com.mardous.booming.data.SearchFilter
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.search.SearchQuery
import com.mardous.booming.data.repository.Repository
import com.mardous.booming.extensions.media.indexOfSong
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: Repository) : ViewModel() {

    private val searchQuery = MutableStateFlow(SearchQuery())

    private val _searchFilter = MutableStateFlow<SearchFilter?>(null)
    val searchFilter = _searchFilter.asStateFlow()

    private val _searchResult = MutableStateFlow<List<Any>>(emptyList())
    val searchResult = _searchResult.asStateFlow()

    private val _queueFlow = MutableSharedFlow<Triple<List<Song>, Int, SongClickBehavior>>()
    val queueFlow = _queueFlow.asSharedFlow()

    init {
        @OptIn(FlowPreview::class)
        combine(searchQuery, searchFilter) { query, filter -> query to filter }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { (query, filter) ->
                val result = repository.search(query, filter)
                _searchResult.value = result
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    fun updateFilter(filter: SearchFilter?) {
        _searchFilter.value = filter
    }

    fun updateQuery(
        mode: SearchQuery.FilterMode? = searchQuery.value.filterMode,
        query: String? = searchQuery.value.searched
    ) {
        searchQuery.value = searchQuery.value.copy(filterMode = mode, searched = query)
    }

    fun songClick(song: Song, results: List<Any>) = viewModelScope.launch(Dispatchers.IO) {
        val songs = if (Preferences.playAllSongsWhenSearching) {
            repository.allSongs()
        } else {
            results.filterIsInstance<Song>()
        }
        val startPos = if (songs.size == 1) {
            if (songs.singleOrNull { it.id == song.id } != null) 0 else -1
        } else {
            songs.indexOfSong(song.id).coerceAtLeast(0)
        }
        if (startPos != -1) {
            _queueFlow.emit(Triple(songs, startPos, Preferences.songClickAction))
        }
    }

    fun refresh() {
        searchQuery.value = searchQuery.value.copy(timestamp = System.currentTimeMillis())
    }
}