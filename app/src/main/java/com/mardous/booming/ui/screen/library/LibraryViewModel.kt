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

package com.mardous.booming.ui.screen.library

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import androidx.core.animation.doOnEnd
import androidx.lifecycle.*
import com.mardous.booming.core.model.LibraryMargin
import com.mardous.booming.core.model.filesystem.FileSystemItem
import com.mardous.booming.core.model.filesystem.FileSystemQuery
import com.mardous.booming.data.SongProvider
import com.mardous.booming.data.local.repository.RealSmartRepository
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.local.room.*
import com.mardous.booming.data.mapper.toSong
import com.mardous.booming.data.mapper.toSongEntity
import com.mardous.booming.data.mapper.toSongsEntity
import com.mardous.booming.data.model.*
import com.mardous.booming.extensions.files.getCanonicalPathSafe
import com.mardous.booming.extensions.media.indexOfSong
import com.mardous.booming.ui.screen.library.home.SuggestedResult
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.StorageUtil
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

class LibraryViewModel(
    private val repository: Repository,
    private val inclExclDao: InclExclDao
) : ViewModel() {

    init {
        viewModelScope.launch(IO) {
            initializeBlacklist()
            deleteMissingContent()
        }
    }

    private val suggestions = MutableLiveData(SuggestedResult.Idle)
    private val songs = MutableLiveData<List<Song>>()
    private val albums = MutableLiveData<List<Album>>()
    private val artists = MutableLiveData<List<Artist>>()
    private val playlists = MutableLiveData<List<PlaylistWithSongs>>()
    private val genres = MutableLiveData<List<Genre>>()
    private val years = MutableLiveData<List<ReleaseYear>>()
    private val fileSystem = MutableLiveData<FileSystemQuery>()
    private val fabMargin = MutableLiveData(LibraryMargin(0))
    private val miniPlayerMargin = MutableLiveData(LibraryMargin(0))
    private val songHistory = MutableLiveData<List<Song>>()

    fun getSuggestions(): LiveData<SuggestedResult> = suggestions
    fun getSongs(): LiveData<List<Song>> = songs
    fun getAlbums(): LiveData<List<Album>> = albums
    fun getArtists(): LiveData<List<Artist>> = artists
    fun getPlaylists(): LiveData<List<PlaylistWithSongs>> = playlists
    fun getGenres(): LiveData<List<Genre>> = genres
    fun getYears(): LiveData<List<ReleaseYear>> = years
    fun getFileSystem(): LiveData<FileSystemQuery> = fileSystem
    fun getFabMargin(): LiveData<LibraryMargin> = fabMargin
    fun getMiniPlayerMargin(): LiveData<LibraryMargin> = miniPlayerMargin

    private fun createValueAnimator(oldValue: Int, newValue: Int, setter: (Int) -> Unit): Animator {
        return ValueAnimator.ofInt(oldValue, newValue).apply {
            addUpdateListener { setter(it.animatedValue as Int) }
            doOnEnd { setter(newValue) }
            start()
        }
    }

    fun setLibraryMargins(fabBottomMargin: LibraryMargin, bottomSheetMargin: LibraryMargin) {
        val fabAnimator = createValueAnimator(
            oldValue = fabMargin.value!!.margin,
            newValue = fabBottomMargin.margin
        ) {
            fabMargin.postValue(fabBottomMargin.copy(margin = it))
        }
        val miniPlayerAnimator = createValueAnimator(
            oldValue = miniPlayerMargin.value!!.margin,
            newValue = bottomSheetMargin.margin
        ) {
            miniPlayerMargin.postValue(bottomSheetMargin.copy(margin = it))
        }
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fabAnimator, miniPlayerAnimator)
        animatorSet.start()
    }

    suspend fun albumById(id: Long) = repository.albumById(id)
    fun artistById(id: Long) = repository.artistById(id)
    suspend fun devicePlaylistById(id: Long) = repository.devicePlaylist(id)
    fun genreBySong(song: Song): LiveData<Genre> = liveData(IO) {
        emit(repository.genreBySong(song))
    }

    fun allSongs() = liveData(IO) {
        emit(repository.allSongs())
    }

    fun forceReload(reloadType: ReloadType) = viewModelScope.launch(IO) {
        when (reloadType) {
            ReloadType.Songs -> fetchSongs()
            ReloadType.Albums -> fetchAlbums()
            ReloadType.Artists -> fetchArtists()
            ReloadType.Playlists -> fetchPlaylists()
            ReloadType.Genres -> fetchGenres()
            ReloadType.Folders -> fetchFolders()
            ReloadType.Years -> fetchYears()
            ReloadType.Suggestions -> fetchSuggestions()
        }
    }

    private suspend fun fetchSuggestions() {
        val currentValue = suggestions.value?.copy(state = SuggestedResult.State.Loading)
            ?: SuggestedResult(SuggestedResult.State.Loading)
        suggestions.postValue(currentValue)

        val data = repository.homeSuggestions()
        suggestions.postValue(SuggestedResult(SuggestedResult.State.Ready, data))
    }

    private suspend fun fetchSongs() {
        songs.postValue(repository.allSongs())
    }

    private suspend fun fetchAlbums() {
        albums.postValue(repository.allAlbums())
    }

    private suspend fun fetchArtists() {
        if (Preferences.onlyAlbumArtists) {
            artists.postValue(repository.allAlbumArtists())
        } else {
            artists.postValue(repository.allArtists())
        }
    }

    private suspend fun fetchPlaylists() {
        playlists.postValue(repository.playlistsWithSongs(true))
    }

    private suspend fun fetchGenres() {
        genres.postValue(repository.allGenres())
    }

    private suspend fun fetchYears() {
        years.postValue(repository.allYears())
    }

    private fun fetchFolders() {
        navigateToPath()
    }

    private suspend fun filesToSongs(
        files: List<FileSystemItem>,
        includeFolders: Boolean,
        deepListing: Boolean
    ): List<Song> {
        return buildList {
            if (includeFolders) {
                val songs = files.filterIsInstance<Folder>().flatMap {
                    if (deepListing) {
                        repository.songsByFolder(it.filePath, true)
                    } else {
                        it.songs
                    }
                }
                addAll(songs)
            }
            addAll(files.filterIsInstance<Song>())
        }
    }

    private suspend fun songsFromCurrentFolder(): List<Song> {
        val currentFolder = fileSystem.value
        return if (currentFolder != null) {
            filesToSongs(currentFolder.children, includeFolders = false, deepListing = false)
        } else {
            emptyList()
        }
    }

    fun navigateToPath(
        navigateToPath: String? = null,
        hierarchyView: Boolean = Preferences.hierarchyFolderView
    ) = viewModelScope.launch(IO) {
        if (hierarchyView) {
            val path = if (navigateToPath.isNullOrEmpty()) {
                fileSystem.value?.path ?: Preferences.startDirectory.getCanonicalPathSafe()
            } else {
                navigateToPath
            }
            fileSystem.postValue(repository.filesInPath(path))
        } else {
            fileSystem.postValue(repository.allFolders())
        }
    }

    fun scanPaths(context: Context, paths: Array<String>): LiveData<Int> = liveData(IO) {
        val scanResult = runCatching {
            suspendCancellableCoroutine { continuation ->
                var progress = 0
                val total = paths.size

                MediaScannerConnection.scanFile(context, paths, null) { _, _ ->
                    progress++
                    if (progress == total && continuation.isActive) {
                        continuation.resume(total)
                    }
                }
            }
        }
        emit(scanResult.getOrElse { 0 })
    }

    fun scanAllPaths(context: Context): LiveData<Int> {
        // We attempt to retrieve all storage roots using our StorageManager-based utility.
        // If that fails for some reason, we fall back to Environment.getExternalStorageDirectory()
        // to scan at least the device's primary storage root.
        val storageRoots = StorageUtil.refreshStorageVolumes()
                .map { it.filePath }
                .plus(Environment.getExternalStorageDirectory().path)
                .distinct()
                .toTypedArray()

        return scanPaths(context, storageRoots)
    }

    fun blacklistPath(file: File) = viewModelScope.launch(IO) {
        inclExclDao.insertPath(InclExclEntity(file.getCanonicalPathSafe(), InclExclDao.BLACKLIST))
        forceReload(ReloadType.Folders)
    }

    fun listSongsFromFiles(
        song: Song,
        files: List<FileSystemItem>? = fileSystem.value?.children
    ) = liveData(IO) {
        if (!files.isNullOrEmpty()) {
            val songs = songsFromCurrentFolder()
            val startPos = songs.indexOfSong(song.id).coerceAtLeast(0)
            emit(songs to startPos)
        }
    }

    fun songs(providers: List<Any>): LiveData<List<Song>> = liveData(IO) {
        val songs = providers.filterIsInstance<SongProvider>()
            .flatMap { it.songs }
        emit(songs)
    }

    fun songs(
        files: List<FileSystemItem>,
        includeFolders: Boolean,
        deepListing: Boolean
    ): LiveData<List<Song>> = liveData(IO) {
        val songs = filesToSongs(files, includeFolders, deepListing)
        emit(songs)
    }

    fun artists(type: ContentType): LiveData<List<Artist>> = liveData(IO) {
        when (type) {
            ContentType.TopArtists -> emit(repository.topArtists())
            ContentType.RecentArtists -> emit(repository.recentArtists())
            else -> emit(arrayListOf())
        }
    }

    fun albums(type: ContentType): LiveData<List<Album>> = liveData(IO) {
        when (type) {
            ContentType.TopAlbums -> emit(repository.topAlbums())
            ContentType.RecentAlbums -> emit(repository.recentAlbums())
            else -> emit(arrayListOf())
        }
    }

    fun clearHistory() {
        viewModelScope.launch(IO) {
            repository.clearSongHistory()
        }
        songHistory.value = emptyList()
    }

    fun favorites() = repository.favoriteSongsObservable()

    fun lastAddedSongs(): LiveData<List<Song>> = liveData(IO) {
        emit(repository.recentSongs())
    }

    fun topTracks(): LiveData<List<Song>> = liveData(IO) {
        val songs = repository.playCountSongs().filter { song ->
            if (!File(song.data).exists() || song.id == -1L) {
                repository.deleteSongInPlayCount(song)
                false
            } else true
        }.take(RealSmartRepository.NUMBER_OF_TOP_TRACKS).map {
            it.toSong()
        }
        emit(songs)
    }

    fun observableHistorySongs(): LiveData<List<Song>> {
        viewModelScope.launch(IO) {
            val historySongs = repository.historySongs().filter { song ->
                if (!File(song.data).exists() || song.id == -1L) {
                    repository.deleteSongInHistory(song.id)
                    false
                } else true
            }.map {
                it.toSong()
            }
            songHistory.postValue(historySongs)
        }
        return songHistory
    }

    fun notRecentlyPlayedSongs(): LiveData<List<Song>> = liveData(IO) {
        emit(repository.notRecentlyPlayedSongs())
    }

    fun renamePlaylist(playListId: Long, name: String) = viewModelScope.launch(IO) {
        repository.renamePlaylist(playListId, name)
    }

    fun updatePlaylist(
        playlistId: Long,
        newName: String,
        customCoverUri: String? = null,
        description: String? = null
    ) = viewModelScope.launch(IO) {
        repository.updatePlaylist(playlistId, newName, customCoverUri, description)
    }

    fun deleteSongsInPlaylist(songs: List<SongEntity>) {
        viewModelScope.launch(IO) {
            repository.deleteSongsInPlaylist(songs)
            forceReload(ReloadType.Playlists)
        }
    }

    fun deleteSongsFromPlaylist(playlists: List<PlaylistEntity>) = viewModelScope.launch(IO) {
        repository.deletePlaylistSongs(playlists)
    }

    fun deletePlaylists(playlists: List<PlaylistEntity>) = viewModelScope.launch(IO) {
        repository.deletePlaylists(playlists)
    }

    fun addToPlaylist(playlistName: String, songs: List<Song>): LiveData<AddToPlaylistResult> =
        liveData(IO) {
            emit(AddToPlaylistResult(playlistName, isWorking = true))

            val playlists = checkPlaylistExists(playlistName)
            if (playlists.isEmpty()) {
                val playlistId: Long = createPlaylist(PlaylistEntity(playlistName = playlistName))
                insertSongs(songs.map { it.toSongEntity(playlistId) })
                val playlistCreated = (playlistId != -1L)
                val isFavoritePlaylist = repository.checkFavoritePlaylist()?.playListId == playlistId
                emit(
                    AddToPlaylistResult(
                        playlistName,
                        playlistCreated = playlistCreated,
                        isFavoritePlaylist = isFavoritePlaylist,
                        insertedSongs = songs.size
                    )
                )
            } else {
                val playlist = playlists.firstOrNull()
                if (playlist != null) {
                    val checkedSongs = songs.filterNot { checkSongExistInPlaylist(playlist, it) }
                    val favoritePlaylist = repository.checkFavoritePlaylist()
                    val isFavoritePlaylist = playlist.playListId == favoritePlaylist?.playListId
                    insertSongs(checkedSongs.map {
                        it.toSongEntity(playListId = playlist.playListId)
                    })
                    emit(
                        AddToPlaylistResult(
                            playlistName,
                            isFavoritePlaylist = isFavoritePlaylist,
                            insertedSongs = checkedSongs.size
                        )
                    )
                } else {
                    emit(AddToPlaylistResult(playlistName))
                }
            }
            forceReload(ReloadType.Playlists)
        }

    fun createCustomPlaylist(
        playlistName: String,
        customCoverUri: String? = null,
        description: String? = null,
        songs: List<Song> = emptyList()
    ): LiveData<AddToPlaylistResult> = liveData(IO) {
        emit(AddToPlaylistResult(playlistName, isWorking = true))

        val playlists = checkPlaylistExists(playlistName)
        if (playlists.isEmpty()) {
            val playlistEntity = PlaylistEntity(
                playlistName = playlistName,
                customCoverUri = customCoverUri,
                description = description
            )
            val playlistId: Long = createPlaylist(playlistEntity)
            if (songs.isNotEmpty()) {
                insertSongs(songs.map { it.toSongEntity(playlistId) })
            }
            val playlistCreated = (playlistId != -1L)
            val isFavoritePlaylist = repository.checkFavoritePlaylist()?.playListId == playlistId
            emit(
                AddToPlaylistResult(
                    playlistName,
                    playlistCreated = playlistCreated,
                    isFavoritePlaylist = isFavoritePlaylist,
                    insertedSongs = songs.size
                )
            )
        } else {
            // Playlist already exists
            emit(AddToPlaylistResult(playlistName, playlistCreated = false))
        }
        forceReload(ReloadType.Playlists)
    }

    fun playlistsAsync(): LiveData<List<PlaylistWithSongs>> = liveData(IO) {
        emit(repository.playlistsWithSongs())
    }

    fun favoritePlaylist(): LiveData<PlaylistEntity> = liveData(IO) {
        emit(repository.favoritePlaylist())
    }

    fun isSongFavorite(song: Song): LiveData<Boolean> = liveData(IO) {
        emit(repository.isSongFavorite(song.id))
    }

    private suspend fun checkSongExistInPlaylist(playlistEntity: PlaylistEntity, song: Song) =
        repository.checkSongExistInPlaylist(playlistEntity, song)

    suspend fun insertSongs(songs: List<SongEntity>) = repository.insertSongsInPlaylist(songs)
    private suspend fun removeSongFromPlaylist(songEntity: SongEntity) =
        repository.removeSongFromPlaylist(songEntity)

    private suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity> =
        repository.checkPlaylistExists(playlistName)

    private suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long =
        repository.createPlaylist(playlistEntity)

    private suspend fun deleteMissingContent() {
        repository.deleteMissingContent()
    }

    fun getDevicePlaylists(): LiveData<List<ImportablePlaylistResult>> = liveData(IO) {
        val devicePlaylists = repository.devicePlaylists()
        val importablePlaylists = devicePlaylists.map {
            ImportablePlaylistResult(it.name, it.getSongs())
        }.filter {
            it.songs.isNotEmpty()
        }
        emit(importablePlaylists)
    }

    fun importPlaylist(context: Context, playlist: ImportablePlaylistResult): LiveData<ImportResult> = liveData(IO) {
        var count = 1
        var playlistName = playlist.playlistName
        while (repository.checkPlaylistExists(playlistName).isNotEmpty() && count <= 100) {
            playlistName = "${playlist.playlistName} $count"
            count++
        }
        if (repository.checkPlaylistExists(playlistName).isEmpty()) {
            val id = repository.createPlaylist(PlaylistEntity(playlistName = playlistName))
            if (id != -1L) {
                repository.insertSongsInPlaylist(playlist.songs.toSongsEntity(id))
                emit(ImportResult.success(context, playlist))
                forceReload(ReloadType.Playlists)
            } else {
                emit(ImportResult.error(context, playlist))
            }
        } else {
            emit(ImportResult.error(context, playlist))
        }
    }

    fun deleteSongs(songs: List<Song>) = viewModelScope.launch(IO) {
        repository.deleteSongs(songs)
    }

    private suspend fun initializeBlacklist() {
        if (!Preferences.initializedBlacklist) {
            repository.initializeBlacklist()
            Preferences.initializedBlacklist = true
        }
    }

    @Suppress("DEPRECATION")
    fun handleIntent(intent: Intent): LiveData<HandleIntentResult> = liveData(IO) {
        val result = HandleIntentResult(handled = true)
        val uri = intent.data
        if (uri == null) {
            emit(result.copy(handled = false))
        } else {
            if (uri.toString().isNotEmpty()) {
                val songs = repository.songsByUri(uri)
                emit(result.copy(songs = songs, failed = songs.isEmpty()))
            } else {
                when (intent.type) {
                    MediaStore.Audio.Playlists.CONTENT_TYPE -> {
                        val id = parseIdFromIntent(intent, "playlistId", "playlist")
                        if (id >= 0) {
                            val position = intent.getIntExtra("position", 0)
                            val playlist = devicePlaylistById(id)
                            if (playlist != Playlist.EmptyPlaylist) {
                                emit(result.copy(songs = playlist.getSongs(), position = position))
                            } else {
                                emit(result)
                            }
                        }
                    }

                    MediaStore.Audio.Albums.CONTENT_TYPE -> {
                        val id = parseIdFromIntent(intent, "albumId", "album")
                        if (id >= 0) {
                            val position = intent.getIntExtra("position", 0)
                            emit(result.copy(songs = albumById(id).songs, position = position))
                        }
                    }

                    MediaStore.Audio.Artists.CONTENT_TYPE -> {
                        val id = parseIdFromIntent(intent, "artistId", "artist")
                        if (id >= 0) {
                            val position = intent.getIntExtra("position", 0)
                            emit(result.copy(songs = artistById(id).songs, position = position))
                        }
                    }

                    else -> emit(result.copy(handled = false))
                }
            }
        }
    }

    private fun parseIdFromIntent(intent: Intent, longKey: String, stringKey: String): Long {
        var id = intent.getLongExtra(longKey, -1)
        if (id < 0) {
            id = intent.getStringExtra(stringKey)?.toLongOrNull() ?: -1
        }
        return id
    }
}

enum class ReloadType {
    Songs,
    Albums,
    Artists,
    Playlists,
    Genres,
    Folders,
    Years,
    Suggestions
}
