package com.mardous.booming.ui.screen.library.playlists

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.mardous.booming.data.local.repository.PlaylistRepository
import com.mardous.booming.data.local.room.SongEntity

class PlaylistDetailViewModel(
    private val playlistRepository: PlaylistRepository,
    private var playlistId: Long
) : ViewModel() {
    fun getSongs(): LiveData<List<SongEntity>> =
        playlistRepository.getSongs(playlistId)

    fun playlistExists(): LiveData<Boolean> =
        playlistRepository.checkPlaylistExists(playlistId)

    fun getPlaylist() = playlistRepository.playlistWithSongsObservable(playlistId)
}