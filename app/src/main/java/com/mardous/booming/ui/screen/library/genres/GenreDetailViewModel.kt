package com.mardous.booming.ui.screen.library.genres

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.Genre
import com.mardous.booming.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GenreDetailViewModel(private val repository: Repository, private val genre: Genre) : ViewModel() {

    private val _genreSongs = MutableLiveData<List<Song>>()

    fun getSongs(): LiveData<List<Song>> = _genreSongs

    init {
        loadGenreSongs()
    }

    fun loadGenreSongs() = viewModelScope.launch(Dispatchers.IO) {
        val songs = repository.songsByGenre(genre.id)
        _genreSongs.postValue(songs)
    }
}