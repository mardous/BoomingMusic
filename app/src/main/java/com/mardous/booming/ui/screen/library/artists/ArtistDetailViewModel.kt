package com.mardous.booming.ui.screen.library.artists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.network.NetworkFeature
import com.mardous.booming.data.remote.lastfm.model.LastFmArtist
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
        emit(repository.similarAlbumArtists(artist).sortedBy { it.name })
    }

    fun getArtistBio(
        name: String,
        lang: String?,
        cache: String?
    ): LiveData<LastFmArtist?> = liveData(Dispatchers.IO) {
        if (NetworkFeature.Lastfm.Biographies.isAvailable(getApplication())) {
            emit(repository.artistInfo(name, lang, cache))
        }
    }
}