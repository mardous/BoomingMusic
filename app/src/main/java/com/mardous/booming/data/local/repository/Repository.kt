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

package com.mardous.booming.data.local.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.media3.common.MediaItem
import com.mardous.booming.core.model.about.Contribution
import com.mardous.booming.core.model.filesystem.FileSystemQuery
import com.mardous.booming.core.model.task.Result
import com.mardous.booming.core.model.task.Result.Error
import com.mardous.booming.core.model.task.Result.Success
import com.mardous.booming.data.SearchFilter
import com.mardous.booming.data.local.room.PlayCountEntity
import com.mardous.booming.data.local.room.PlaylistEntity
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.local.room.SongEntity
import com.mardous.booming.data.mapper.toSong
import com.mardous.booming.data.model.*
import com.mardous.booming.data.model.search.SearchQuery
import com.mardous.booming.data.remote.deezer.DeezerService
import com.mardous.booming.data.remote.deezer.model.DeezerAlbum
import com.mardous.booming.data.remote.deezer.model.DeezerTrack
import com.mardous.booming.data.remote.lastfm.LastFmService
import com.mardous.booming.data.remote.lastfm.model.LastFmAlbum
import com.mardous.booming.data.remote.lastfm.model.LastFmArtist
import kotlinx.coroutines.flow.Flow
import java.io.File

const val MAX_ITEMS_PER_CHUNK = 900

interface Repository {

    suspend fun allSongs(): List<Song>
    suspend fun allAlbums(): List<Album>
    suspend fun allArtists(): List<Artist>
    suspend fun allAlbumArtists(): List<Artist>
    suspend fun allGenres(): List<Genre>
    suspend fun allYears(): List<ReleaseYear>
    suspend fun allFolders(): FileSystemQuery
    suspend fun filesInPath(path: String): FileSystemQuery
    suspend fun playlists(): List<PlaylistEntity>
    fun playlistSongs(playListId: Long): LiveData<List<SongEntity>>
    suspend fun playlistSongs(playlistWithSongs: PlaylistWithSongs): List<Song>
    suspend fun devicePlaylists(): List<Playlist>
    suspend fun devicePlaylistSongs(playlist: Playlist): List<Song>
    suspend fun devicePlaylist(playlistId: Long): Playlist
    suspend fun playlistsWithSongs(sorted: Boolean = false): List<PlaylistWithSongs>
    suspend fun playlistWithSongs(playlistId: Long): PlaylistWithSongs
    fun playlistWithSongsObservable(playlistId: Long): LiveData<PlaylistWithSongs>
    suspend fun isSongFavorite(songEntity: SongEntity): List<SongEntity>
    suspend fun isSongFavorite(songId: Long): Boolean
    suspend fun favoriteSongs(): List<Song>
    fun favoriteSongsFlow(): Flow<List<Song>>
    suspend fun favoritePlaylist(): PlaylistEntity
    suspend fun checkFavoritePlaylist(): PlaylistEntity?
    suspend fun toggleFavorite(song: Song): Boolean
    fun checkPlaylistExists(playListId: Long): LiveData<Boolean>
    suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity>
    suspend fun checkSongExistInPlaylist(playlistEntity: PlaylistEntity, song: Song): Boolean
    suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long
    suspend fun deletePlaylists(playlists: List<PlaylistEntity>)
    suspend fun renamePlaylist(playlistId: Long, name: String)
    suspend fun updatePlaylist(playlist: PlaylistEntity)
    suspend fun insertSongsInPlaylist(songs: List<SongEntity>)
    suspend fun removeSongFromPlaylist(songEntity: SongEntity)
    suspend fun deleteSongsInPlaylist(songs: List<SongEntity>)
    suspend fun deletePlaylistSongs(playlists: List<PlaylistEntity>)
    suspend fun deleteSong(songId: Long): Song
    suspend fun deleteSongs(songs: List<Song>)
    suspend fun deleteMissingContent()
    suspend fun albumById(albumId: Long): Album
    suspend fun albumByIdAsync(albumId: Long): Album
    suspend fun similarAlbums(album: Album): List<Album>
    fun artistById(artistId: Long): Artist
    fun albumArtistByName(name: String): Artist
    suspend fun similarAlbumArtists(artist: Artist): List<Artist>
    fun songById(songId: Long): Song
    suspend fun songsByGenre(genreId: Long): List<Song>
    fun songByGenre(genreId: Long): Song
    suspend fun genreBySong(song: Song): Genre
    suspend fun yearById(year: Int): ReleaseYear
    suspend fun songsByYear(year: Int): List<Song>
    suspend fun folderByPath(path: String): Folder
    suspend fun songsByUri(uri: Uri): List<Song>
    suspend fun songsByMediaItems(mediaItems: List<MediaItem>): Pair<List<Song>, List<MediaItem>>
    suspend fun songByMediaItem(mediaItem: MediaItem?): Song
    suspend fun songsByFolder(folderPath: String, includeSubfolders: Boolean): List<Song>
    suspend fun songByFilePath(path: String, ignoreBlacklist: Boolean): Song
    suspend fun homeSuggestions(): List<Suggestion>
    suspend fun topArtistsSuggestion(): Suggestion
    suspend fun topAlbumsSuggestion(): Suggestion
    suspend fun recentArtistsSuggestion(): Suggestion
    suspend fun recentAlbumsSuggestion(): Suggestion
    suspend fun favoritesSuggestion(): Suggestion
    suspend fun recommendedSongSuggestion(): Suggestion
    suspend fun recentSongs(): List<Song>
    suspend fun topArtists(): List<Artist>
    suspend fun recentArtists(): List<Artist>
    suspend fun topAlbums(): List<Album>
    suspend fun recentAlbums(): List<Album>
    suspend fun playCountSongs(): List<Song>
    fun playCountSongsFlow(): Flow<List<Song>>
    suspend fun findSongsInPlayCount(songs: List<Song>): List<PlayCountEntity>
    suspend fun findSongInPlayCount(songId: Long): PlayCountEntity?
    suspend fun insertOrIncrementPlayCount(song: Song, timePlayed: Long)
    suspend fun insertOrIncrementSkipCount(song: Song)
    suspend fun clearPlayCount()
    suspend fun upsertSongInHistory(currentSong: Song)
    suspend fun deleteSongInHistory(songId: Long)
    suspend fun clearSongHistory()
    suspend fun historySongs(): List<Song>
    fun historySongsFlow(): Flow<List<Song>>
    suspend fun notRecentlyPlayedSongs(): List<Song>
    suspend fun initializeBlacklist()
    suspend fun search(query: SearchQuery, filter: SearchFilter?): List<Any>
    suspend fun searchSongs(query: String): List<Song>
    suspend fun searchArtists(query: String): List<Artist>
    suspend fun searchAlbums(query: String): List<Album>
    suspend fun searchPlaylists(query: String): List<PlaylistWithSongs>
    suspend fun searchGenres(query: String): List<Genre>
    suspend fun deezerTrack(artist: String, title: String): Result<DeezerTrack>
    suspend fun deezerAlbum(artist: String, name: String): Result<DeezerAlbum>
    suspend fun artistInfo(name: String, lang: String?, cache: String?): Result<LastFmArtist>
    suspend fun albumInfo(artist: String, album: String, lang: String?): Result<LastFmAlbum>
    suspend fun contributors(): List<Contribution>
    suspend fun translators(): List<Contribution>
}

class RealRepository(
    private val context: Context,
    private val deezerService: DeezerService,
    private val lastFmService: LastFmService,
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val genreRepository: GenreRepository,
    private val smartRepository: SmartRepository,
    private val specialRepository: SpecialRepository,
    private val playlistRepository: PlaylistRepository,
    private val searchRepository: SearchRepository
) : Repository {

    override suspend fun allSongs(): List<Song> = songRepository.songs()

    override suspend fun allAlbums(): List<Album> = albumRepository.albums()

    override suspend fun allArtists(): List<Artist> = artistRepository.artists()

    override suspend fun allAlbumArtists(): List<Artist> = artistRepository.albumArtists()

    override suspend fun allGenres(): List<Genre> = genreRepository.genres()

    override suspend fun allYears(): List<ReleaseYear> = specialRepository.releaseYears()

    override suspend fun allFolders(): FileSystemQuery = specialRepository.musicFolders()

    override suspend fun filesInPath(path: String): FileSystemQuery = specialRepository.musicFilesInPath(path)

    override suspend fun playlists(): List<PlaylistEntity> = playlistRepository.playlists()

    override fun playlistSongs(playListId: Long): LiveData<List<SongEntity>> =
        playlistRepository.getSongs(playListId)

    override suspend fun playlistSongs(playlistWithSongs: PlaylistWithSongs): List<Song> =
        playlistWithSongs.songs.map {
            it.toSong()
        }

    override suspend fun devicePlaylists(): List<Playlist> =
        playlistRepository.devicePlaylists()

    override suspend fun devicePlaylistSongs(playlist: Playlist): List<Song> =
        playlistRepository.devicePlaylistSongs(playlist.id)

    override suspend fun devicePlaylist(playlistId: Long): Playlist =
        playlistRepository.devicePlaylist(playlistId)

    override suspend fun playlistsWithSongs(sorted: Boolean): List<PlaylistWithSongs> =
        playlistRepository.playlistsWithSongs(sorted)

    override suspend fun playlistWithSongs(playlistId: Long): PlaylistWithSongs =
        playlistRepository.playlistWithSongs(playlistId)

    override fun playlistWithSongsObservable(playlistId: Long): LiveData<PlaylistWithSongs> =
        playlistRepository.playlistWithSongsObservable(playlistId)

    override suspend fun isSongFavorite(songEntity: SongEntity): List<SongEntity> =
        playlistRepository.isSongFavorite(songEntity)

    override suspend fun isSongFavorite(songId: Long): Boolean =
        playlistRepository.isSongFavorite(songId)

    override suspend fun favoriteSongs(): List<Song> =
        playlistRepository.favoriteSongs()

    override fun favoriteSongsFlow(): Flow<List<Song>> =
        playlistRepository.favoriteSongsFlow()

    override suspend fun favoritePlaylist(): PlaylistEntity =
        playlistRepository.favoritePlaylist()

    override suspend fun checkFavoritePlaylist(): PlaylistEntity? =
        playlistRepository.checkFavoritePlaylist()

    override suspend fun toggleFavorite(song: Song): Boolean =
        playlistRepository.toggleFavorite(song)

    override fun checkPlaylistExists(playListId: Long): LiveData<Boolean> =
        playlistRepository.checkPlaylistExists(playListId)

    override suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity> =
        playlistRepository.checkPlaylistExists(playlistName)

    override suspend fun checkSongExistInPlaylist(
        playlistEntity: PlaylistEntity,
        song: Song
    ): Boolean =
        playlistRepository.checkSongExistInPlaylist(playlistEntity, song)

    override suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long =
        playlistRepository.createPlaylist(playlistEntity)

    override suspend fun deletePlaylists(playlists: List<PlaylistEntity>) =
        playlistRepository.deletePlaylistEntities(playlists)

    override suspend fun renamePlaylist(playlistId: Long, name: String) =
        playlistRepository.renamePlaylistEntity(playlistId, name)

    override suspend fun updatePlaylist(playlist: PlaylistEntity) =
        playlistRepository.updatePlaylist(playlist)

    override suspend fun insertSongsInPlaylist(songs: List<SongEntity>) =
        playlistRepository.insertSongs(songs)

    override suspend fun removeSongFromPlaylist(songEntity: SongEntity) =
        playlistRepository.removeSongFromPlaylist(songEntity)

    override suspend fun deleteSongsInPlaylist(songs: List<SongEntity>) =
        playlistRepository.deleteSongsInPlaylist(songs)

    override suspend fun deletePlaylistSongs(playlists: List<PlaylistEntity>) =
        playlistRepository.deletePlaylistSongs(playlists)

    override suspend fun deleteSong(songId: Long): Song {
        val song = songRepository.song(songId)
        if (song != Song.emptySong) {
            playlistRepository.deleteSongFromAllPlaylists(songId)
            smartRepository.deleteSongInHistory(songId)
            smartRepository.deleteSongInPlayCount(songId)
        }
        return song
    }

    override suspend fun deleteSongs(songs: List<Song>) {
        val deletableIds = songs.filterNot { it == Song.emptySong }.map { it.id }
        if (deletableIds.isEmpty()) return

        playlistRepository.deleteSongsFromAllPlaylists(deletableIds)
        smartRepository.deleteSongsInHistory(deletableIds)
        smartRepository.deleteSongsInPlayCount(deletableIds)
    }

    override suspend fun deleteMissingContent() {
        // Clean up playlists
        val playlists = playlistRepository.playlistsWithSongs()
        playlists.forEach { playlistWithSongs ->
            val missingSongs = playlistWithSongs.songs.filterNot {
                File(it.data).exists()
            }
            playlistRepository.deleteSongsInPlaylist(missingSongs)
        }
    }

    override suspend fun albumById(albumId: Long): Album = albumRepository.album(albumId)

    override suspend fun albumByIdAsync(albumId: Long): Album = albumRepository.album(albumId)

    override suspend fun similarAlbums(album: Album): List<Album> =
        albumRepository.similarAlbums(album)

    override fun artistById(artistId: Long): Artist = artistRepository.artist(artistId)

    override fun albumArtistByName(name: String): Artist = artistRepository.albumArtist(name)

    override suspend fun similarAlbumArtists(artist: Artist): List<Artist> =
        artistRepository.similarAlbumArtists(artist)

    override fun songById(songId: Long): Song = songRepository.song(songId)

    override suspend fun songsByGenre(genreId: Long): List<Song> = genreRepository.songs(genreId)

    override fun songByGenre(genreId: Long): Song = genreRepository.song(genreId)

    override suspend fun genreBySong(song: Song): Genre = genreRepository.genre(song)

    override suspend fun yearById(year: Int): ReleaseYear = specialRepository.releaseYear(year)

    override suspend fun songsByYear(year: Int): List<Song> = specialRepository.songsByYear(year, null)

    override suspend fun folderByPath(path: String): Folder = specialRepository.folderByPath(path)

    override suspend fun songsByUri(uri: Uri): List<Song> = songRepository.songsByUri(uri)

    override suspend fun songsByMediaItems(mediaItems: List<MediaItem>): Pair<List<Song>, List<MediaItem>> =
        songRepository.songsByMediaItems(mediaItems)

    override suspend fun songByMediaItem(mediaItem: MediaItem?): Song =
        songRepository.songByMediaItem(mediaItem)

    override suspend fun songsByFolder(folderPath: String, includeSubfolders: Boolean) =
        specialRepository.songsByFolder(folderPath, includeSubfolders)

    override suspend fun songByFilePath(path: String, ignoreBlacklist: Boolean) =
        songRepository.songByFilePath(path, ignoreBlacklist)

    override suspend fun homeSuggestions(): List<Suggestion> {
        return listOf(
            topArtistsSuggestion(),
            topAlbumsSuggestion(),
            recentArtistsSuggestion(),
            recentAlbumsSuggestion(),
            favoritesSuggestion(),
            recommendedSongSuggestion()
        ).filter {
            it.items.isNotEmpty()
        }
    }

    override suspend fun topArtistsSuggestion(): Suggestion {
        val artists = smartRepository.topAlbumArtists().take(10)
        return Suggestion(ContentType.TopArtists, artists)
    }

    override suspend fun topAlbumsSuggestion(): Suggestion {
        val albums = smartRepository.topAlbums().take(10)
        return Suggestion(ContentType.TopAlbums, albums)
    }

    override suspend fun recentArtistsSuggestion(): Suggestion {
        val artists = smartRepository.recentAlbumArtists().take(10)
        return Suggestion(ContentType.RecentArtists, artists)
    }

    override suspend fun recentAlbumsSuggestion(): Suggestion {
        val albums = smartRepository.recentAlbums().take(10)
        return Suggestion(ContentType.RecentAlbums, albums)
    }

    override suspend fun favoritesSuggestion(): Suggestion {
        val songs = favoriteSongs()
        return Suggestion(ContentType.Favorites, songs.take(10))
    }

    override suspend fun recommendedSongSuggestion(): Suggestion {
        val songs = smartRepository.notRecentlyPlayedSongs().take(10)
        return Suggestion(ContentType.NotRecentlyPlayed, songs)
    }

    override suspend fun recentSongs(): List<Song> = smartRepository.recentSongs()

    override suspend fun topArtists(): List<Artist> = smartRepository.topAlbumArtists()

    override suspend fun recentArtists(): List<Artist> = smartRepository.recentAlbumArtists()

    override suspend fun topAlbums(): List<Album> = smartRepository.topAlbums()

    override suspend fun recentAlbums(): List<Album> = smartRepository.recentAlbums()

    override suspend fun playCountSongs(): List<Song> = smartRepository.playCountSongs()

    override fun playCountSongsFlow(): Flow<List<Song>> = smartRepository.playCountSongsFlow()

    override suspend fun findSongsInPlayCount(songs: List<Song>): List<PlayCountEntity> =
        smartRepository.findSongsInPlayCount(songs)

    override suspend fun findSongInPlayCount(songId: Long): PlayCountEntity? =
        smartRepository.findSongInPlayCount(songId)

    override suspend fun insertOrIncrementPlayCount(song: Song, timePlayed: Long) =
        smartRepository.insetOrIncrementPlayCount(song, timePlayed)

    override suspend fun insertOrIncrementSkipCount(song: Song) =
        smartRepository.insetOrIncrementSkipCount(song)

    override suspend fun clearPlayCount() = smartRepository.clearPlayCount()

    override suspend fun upsertSongInHistory(currentSong: Song) =
        smartRepository.upsertSongInHistory(currentSong)

    override suspend fun deleteSongInHistory(songId: Long) =
        smartRepository.deleteSongInHistory(songId)

    override suspend fun clearSongHistory() =
        smartRepository.clearSongHistory()

    override suspend fun historySongs(): List<Song> =
        smartRepository.historySongs()

    override fun historySongsFlow(): Flow<List<Song>> =
        smartRepository.historySongsFlow()

    override suspend fun notRecentlyPlayedSongs(): List<Song> =
        smartRepository.notRecentlyPlayedSongs()

    override suspend fun initializeBlacklist() {
        songRepository.initializeBlacklist()
    }

    override suspend fun search(query: SearchQuery, filter: SearchFilter?): List<Any> =
        searchRepository.searchAll(context, query, filter)

    override suspend fun searchSongs(query: String): List<Song> = songRepository.songs(query)

    override suspend fun searchArtists(query: String): List<Artist> =
        artistRepository.artists(query)

    override suspend fun searchAlbums(query: String): List<Album> = albumRepository.albums(query)

    override suspend fun searchPlaylists(query: String): List<PlaylistWithSongs> = playlistRepository.searchPlaylists(query)

    override suspend fun searchGenres(query: String): List<Genre> = genreRepository.genres(query)

    override suspend fun deezerTrack(artist: String, title: String): Result<DeezerTrack> {
        return try {
            Success(deezerService.track(artist, title))
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun deezerAlbum(artist: String, name: String): Result<DeezerAlbum> {
        return try {
            Success(deezerService.album(artist, name))
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun artistInfo(
        name: String,
        lang: String?,
        cache: String?
    ): Result<LastFmArtist> {
        return try {
            Success(lastFmService.artistInfo(name, lang, cache))
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun albumInfo(
        artist: String,
        album: String,
        lang: String?
    ): Result<LastFmAlbum> {
        return try {
            Success(lastFmService.albumInfo(album, artist, lang))
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun contributors(): List<Contribution> =
        Contribution.loadContributions(context, "contributors.json")

    override suspend fun translators(): List<Contribution> =
        Contribution.loadContributions(context, "translators.json")
}