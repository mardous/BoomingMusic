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
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.mardous.booming.R
import com.mardous.booming.core.sort.PlaylistSortMode
import com.mardous.booming.data.local.room.PlaylistDao
import com.mardous.booming.data.local.room.PlaylistEntity
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.local.room.SongEntity
import com.mardous.booming.data.mapper.toSongEntity
import com.mardous.booming.data.mapper.toSongs
import com.mardous.booming.data.model.Playlist
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.utilities.mapIfValid
import com.mardous.booming.extensions.utilities.takeOrDefault
import com.mardous.booming.util.cursor.SortedCursorUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface PlaylistRepository {
    fun devicePlaylists(): List<Playlist>
    fun devicePlaylist(playlistId: Long): Playlist
    fun devicePlaylistSongs(playlistId: Long): List<Song>
    suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long
    suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity>
    fun checkPlaylistExists(playListId: Long): LiveData<Boolean>
    suspend fun playlists(): List<PlaylistEntity>
    suspend fun playlistsWithSongs(sorted: Boolean = false): List<PlaylistWithSongs>
    suspend fun playlistWithSongs(playlistId: Long): PlaylistWithSongs
    fun playlistWithSongsObservable(playlistId: Long): LiveData<PlaylistWithSongs>
    suspend fun playlistSongs(playlistId: Long): List<SongEntity>
    fun playlistSongsObservable(playListId: Long): LiveData<List<SongEntity>>
    suspend fun searchPlaylists(searchQuery: String): List<PlaylistWithSongs>
    suspend fun searchPlaylistSongs(playlistId: Long, searchQuery: String): List<SongEntity>
    suspend fun insertSongs(songs: List<SongEntity>)
    suspend fun deletePlaylistEntities(playlistEntities: List<PlaylistEntity>)
    suspend fun renamePlaylistEntity(playlistId: Long, name: String)
    suspend fun updatePlaylist(playlist: PlaylistEntity)
    suspend fun favoritePlaylist(): PlaylistEntity
    suspend fun checkFavoritePlaylist(): PlaylistEntity?
    suspend fun favoriteSongs(): List<Song>
    fun favoriteSongsFlow(): Flow<List<Song>>
    suspend fun toggleFavorite(song: Song): Boolean
    suspend fun isSongFavorite(songId: Long): Boolean
    suspend fun findSongsInFavorites(songs: List<Song>): List<SongEntity>
    suspend fun findSongInPlaylist(playlistId: Long, song: Song): SongEntity?
    suspend fun findSongsInPlaylist(playlistId: Long, songs: List<Song>): List<SongEntity>
    suspend fun removeSongFromPlaylist(songEntity: SongEntity)
    suspend fun checkSongExistInPlaylist(playlistEntity: PlaylistEntity, song: Song): Boolean
    suspend fun deleteSongsFromPlaylist(songs: List<SongEntity>)
    suspend fun deleteSongsFromPlaylists(playlists: List<PlaylistEntity>)
    suspend fun deleteSongFromAllPlaylists(songId: Long)
    suspend fun deleteSongsFromAllPlaylists(songsIds: List<Long>)
}

class RealPlaylistRepository(
    private val context: Context,
    private val songRepository: SongRepository,
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun devicePlaylists(): List<Playlist> {
        return makePlaylistCursor().use {
            it.mapIfValid {
                Playlist(getLong(0), getString(1))
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun devicePlaylist(playlistId: Long): Playlist {
        return makePlaylistCursor("${MediaStore.Audio.Playlists._ID}=?", arrayOf(playlistId.toString())).use {
            it.takeOrDefault(Playlist.EmptyPlaylist) {
                Playlist(getLong(0), getString(1))
            }
        }
    }

    override fun devicePlaylistSongs(playlistId: Long): List<Song> {
        val sortedCursor = SortedCursorUtil.makeSortedCursor(makePlaylistSongsCursor(playlistId), 0)
        return songRepository.songs(sortedCursor)
    }

    override suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long =
        playlistDao.createPlaylist(playlistEntity)

    override suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity> =
        playlistDao.playlist(playlistName)

    override fun checkPlaylistExists(playListId: Long): LiveData<Boolean> =
        playlistDao.checkPlaylistExists(playListId)

    override suspend fun playlists(): List<PlaylistEntity> = playlistDao.playlists()

    override suspend fun playlistsWithSongs(sorted: Boolean): List<PlaylistWithSongs> =
        playlistDao.playlistsWithSongs().let { playlistWithSongs ->
            if (sorted) with(PlaylistSortMode.AllPlaylists) {
                playlistWithSongs.sorted()
            } else {
                playlistWithSongs
            }
        }

    override suspend fun playlistWithSongs(playlistId: Long): PlaylistWithSongs =
        playlistDao.playlistWithSongs(playlistId) ?: PlaylistWithSongs.Empty

    override fun playlistWithSongsObservable(playlistId: Long): LiveData<PlaylistWithSongs> =
        playlistDao.playlistWithSongsObservable(playlistId).map { result -> result ?: PlaylistWithSongs.Empty }

    override suspend fun playlistSongs(playlistId: Long): List<SongEntity> =
        playlistDao.songsFromPlaylist(playlistId)

    override fun playlistSongsObservable(playListId: Long): LiveData<List<SongEntity>> =
        playlistDao.songsFromPlaylistObservable(playListId)

    override suspend fun searchPlaylists(searchQuery: String): List<PlaylistWithSongs> =
        playlistDao.searchPlaylists("%$searchQuery%")

    override suspend fun searchPlaylistSongs(playlistId: Long, searchQuery: String): List<SongEntity> =
        playlistDao.searchSongs(playlistId, "%$searchQuery%")

    override suspend fun insertSongs(songs: List<SongEntity>) {
        playlistDao.insertSongsToPlaylist(songs)
    }

    override suspend fun deletePlaylistEntities(playlistEntities: List<PlaylistEntity>) =
        playlistDao.deletePlaylists(playlistEntities)

    override suspend fun renamePlaylistEntity(playlistId: Long, name: String) =
        playlistDao.renamePlaylist(playlistId, name)

    override suspend fun updatePlaylist(playlist: PlaylistEntity) =
        playlistDao.updatePlaylist(playlist)

    override suspend fun favoritePlaylist(): PlaylistEntity {
        val favorite = context.getString(R.string.favorites_label)
        val playlist: PlaylistEntity? = playlistDao.playlist(favorite).firstOrNull()
        return if (playlist != null) {
            playlist
        } else {
            createPlaylist(PlaylistEntity(playlistName = favorite))
            playlistDao.playlist(favorite).first()
        }
    }

    override suspend fun checkFavoritePlaylist(): PlaylistEntity? {
        val favorite = context.getString(R.string.favorites_label)
        return playlistDao.playlist(favorite).firstOrNull()
    }

    override suspend fun favoriteSongs(): List<Song> {
        val favorite = context.getString(R.string.favorites_label)
        val favoritesPlaylist = playlistDao.playlist(favorite)
        return if (favoritesPlaylist.isNotEmpty())
            playlistDao.favoritesSongs(favoritesPlaylist.single().playListId).toSongs()
        else emptyList()
    }

    override fun favoriteSongsFlow(): Flow<List<Song>> =
        playlistDao.favoritesSongsFlow(context.getString(R.string.favorites_label)).map {
            withContext(Dispatchers.Default) {
                it.toSongs()
            }
        }

    override suspend fun toggleFavorite(song: Song): Boolean {
        val playlist = favoritePlaylist()
        val songEntity = song.toSongEntity(playlist.playListId)
        val favoriteSong = findSongInPlaylist(playlist.playListId, song)
        return if (favoriteSong != null) {
            removeSongFromPlaylist(songEntity)
            false
        } else {
            insertSongs(listOf(songEntity))
            true
        }
    }

    override suspend fun isSongFavorite(songId: Long): Boolean {
        val favorites = playlistDao.playlist(context.getString(R.string.favorites_label)).firstOrNull()
        if (favorites != null) {
            return playlistDao.checkSongExistInPlaylist(favorites.playListId, songId)
        }
        return false
    }

    override suspend fun findSongsInFavorites(songs: List<Song>): List<SongEntity> {
        val favorites = playlistDao.playlist(context.getString(R.string.favorites_label)).firstOrNull()
        if (favorites != null) {
            return buildList {
                songs.map { it.id }
                    .chunked(MAX_ITEMS_PER_CHUNK)
                    .forEach { chunkIds ->
                        addAll(playlistDao.findSongsInPlaylist(favorites.playListId, chunkIds))
                    }
            }
        }
        return emptyList()
    }

    override suspend fun findSongInPlaylist(playlistId: Long, song: Song) =
        playlistDao.findSongInPlaylist(playlistId, song.id)

    override suspend fun findSongsInPlaylist(playlistId: Long, songs: List<Song>): List<SongEntity> {
        if (songs.isEmpty()) return emptyList()
        return buildList {
            songs.map { it.id }
                .chunked(MAX_ITEMS_PER_CHUNK)
                .forEach { chunkIds ->
                    addAll(playlistDao.findSongsInPlaylist(playlistId, chunkIds))
                }
        }
    }

    override suspend fun removeSongFromPlaylist(songEntity: SongEntity) =
        playlistDao.deleteSongsFromPlaylist(songEntity.playlistCreatorId, listOf(songEntity.id))

    override suspend fun checkSongExistInPlaylist(playlistEntity: PlaylistEntity, song: Song): Boolean =
        playlistDao.checkSongExistInPlaylist(playlistEntity.playListId, song.id)

    override suspend fun deleteSongsFromPlaylist(songs: List<SongEntity>) {
        songs.groupBy { it.playlistCreatorId }
            .forEach { group ->
                group.value
                    .map { it.id }
                    .chunked(MAX_ITEMS_PER_CHUNK)
                    .forEach {
                        playlistDao.deleteSongsFromPlaylist(group.key, it)
                    }
            }
    }

    override suspend fun deleteSongsFromPlaylists(playlists: List<PlaylistEntity>) {
        playlists.map { it.playListId }
            .chunked(MAX_ITEMS_PER_CHUNK)
            .forEach {
                playlistDao.deleteAllSongsFromPlaylists(it)
            }
    }

    override suspend fun deleteSongFromAllPlaylists(songId: Long) {
        playlistDao.deleteSongsFromAllPlaylists(listOf(songId))
    }

    override suspend fun deleteSongsFromAllPlaylists(songsIds: List<Long>) {
        if (songsIds.isEmpty()) return
        songsIds.chunked(MAX_ITEMS_PER_CHUNK).forEach { chunkIds ->
            playlistDao.deleteSongsFromAllPlaylists(chunkIds)
        }
    }

    @Suppress("DEPRECATION")
    private fun makePlaylistCursor(selection: String? = null, selectionArguments: Array<String>? = null): Cursor? {
        try {
            val newSelection = if (selection.isNullOrEmpty()) {
                "${MediaStore.Audio.Playlists.NAME} != ''"
            } else {
                "${MediaStore.Audio.Playlists.NAME} != '' AND $selection"
            }
            return context.contentResolver.query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME),
                newSelection, selectionArguments, "${MediaStore.Audio.Playlists.NAME} ASC"
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun makePlaylistSongsCursor(playlistId: Long): Cursor? {
        try {
            return context.contentResolver.query(
                MediaStore.Audio.Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistId),
                arrayOf(MediaStore.Audio.Playlists.Members.AUDIO_ID),
                RealSongRepository.BASE_SELECTION,
                null,
                MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER
            )
        } catch (e: SecurityException) {
            Log.e("DevicePlaylists", "Failed to get songs from playlist with ID $playlistId", e)
        }
        return null
    }
}