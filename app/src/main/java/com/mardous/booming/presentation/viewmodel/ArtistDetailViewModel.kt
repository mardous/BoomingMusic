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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.mardous.booming.core.sort.sortedByMediaName
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.network.NetworkFeature
import com.mardous.booming.data.remote.lastfm.model.LastFmArtist
import com.mardous.booming.data.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArtistDetailViewModel(
    application: Application,
    private val repository: Repository,
    private val artistId: Long,
    private val artistName: String?
) : AndroidViewModel(application) {

    private val _artistDetail = MutableLiveData<Artist>()

    fun getArtist() = getArtistDetail().value ?: Artist.empty

    fun getArtistDetail(): LiveData<Artist> = _artistDetail

    fun loadArtistDetail() = viewModelScope.launch(Dispatchers.IO) {
        if (!artistName.isNullOrEmpty()) {
            _artistDetail.postValue(repository.albumArtistByName(artistName))
        } else if (artistId != -1L) {
            _artistDetail.postValue(repository.artistById(artistId))
        } else {
            _artistDetail.postValue(Artist.empty)
        }
    }

    fun getSimilarArtists(artist: Artist): LiveData<List<Artist>> = liveData(Dispatchers.IO) {
        emit(repository.similarAlbumArtists(artist).sortedByMediaName { it.name })
    }

    fun getArtistBio(
        name: String,
        lang: String?,
        cache: String?
    ): LiveData<LastFmArtist?> = liveData(Dispatchers.IO) {
        if (NetworkFeature.Lastfm.Biographies.isAvailable) {
            emit(repository.artistInfo(name, lang, cache))
        }
    }
}