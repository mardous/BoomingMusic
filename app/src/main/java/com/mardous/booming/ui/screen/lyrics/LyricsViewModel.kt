package com.mardous.booming.ui.screen.lyrics

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.mardous.booming.core.model.task.Result
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.local.repository.LyricsRepository
import com.mardous.booming.service.queue.QueueManager
import com.mardous.booming.service.queue.QueueObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @author Christians M. A. (mardous)
 */
class LyricsViewModel(
    private val queueManager: QueueManager,
    private val lyricsRepository: LyricsRepository
) : ViewModel(), QueueObserver {

    private var lyricsJob: Job? = null
    private val _lyricsResult = MutableStateFlow(LyricsResult.Empty)
    val lyricsResult = _lyricsResult.asStateFlow()

    private val silentHandler = CoroutineExceptionHandler { _, _ -> }

    init {
        queueManager.addObserver(this)
        if (lyricsResult.value == LyricsResult.Empty) {
            updateSong(queueManager.currentSong)
        }
    }

    override fun onCleared() {
        lyricsJob?.cancel()
        queueManager.removeObserver(this)
        super.onCleared()
    }

    override fun songChanged(currentSong: Song, nextSong: Song) {
        updateSong(currentSong)
    }

    fun getOnlineLyrics(song: Song, title: String, artist: String) = liveData(Dispatchers.IO) {
        emit(Result.Loading)
        emit(lyricsRepository.onlineLyrics(song, title, artist))
    }

    fun getAllLyrics(
        song: Song,
        allowDownload: Boolean = false,
        fromEditor: Boolean = false
    ) = liveData(Dispatchers.IO + silentHandler) {
        emit(LyricsResult(id = song.id, loading = true))
        emit(lyricsRepository.allLyrics(song, allowDownload, fromEditor))
    }

    fun getLyrics(song: Song) = liveData(Dispatchers.IO) {
        emit(lyricsRepository.embeddedLyrics(song, requirePlainText = true))
    }

    fun shareSyncedLyrics(song: Song) = liveData(Dispatchers.IO) {
        emit(lyricsRepository.shareSyncedLyrics(song))
    }

    fun getWritableUris(song: Song) = liveData(Dispatchers.IO) {
        val uris = lyricsRepository.writableUris(song)
        delay(500)
        emit(uris)
    }

    fun saveLyrics(song: Song, plainLyrics: EditableLyrics?, syncedLyrics: EditableLyrics?) =
        liveData(Dispatchers.IO) {
            val saveResult = lyricsRepository.saveLyrics(song, plainLyrics, syncedLyrics)
            if (saveResult.hasChanged && song.id == lyricsResult.value.id) {
                updateSong(song)
            }
            emit(saveResult)
        }

    fun importLyrics(song: Song, uri: Uri) = liveData(Dispatchers.IO) {
        emit(lyricsRepository.importLyrics(song, uri))
    }

    fun deleteLyrics() = viewModelScope.launch(Dispatchers.IO) {
        lyricsRepository.deleteAllLyrics()
    }

    private fun updateSong(song: Song) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            if (song == Song.emptySong) {
                _lyricsResult.value = LyricsResult.Empty
            } else {
                _lyricsResult.value = LyricsResult(id = song.id, loading = true)
                val result = withContext(Dispatchers.IO) {
                    lyricsRepository.allLyrics(song, allowDownload = true, fromEditor = false)
                }
                if (isActive) {
                    _lyricsResult.value = result
                }
            }
        }
    }
}