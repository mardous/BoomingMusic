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
import com.mardous.booming.data.local.room.PlaylistDao
import com.mardous.booming.data.local.room.PlaylistEntity
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.local.room.SongEntity
import com.mardous.booming.data.mapper.toSongEntity
import com.mardous.booming.data.model.Playlist
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.utilities.mapIfValid
import com.mardous.booming.extensions.utilities.takeOrDefault
import com.mardous.booming.util.cursor.SortedCursorUtil
import com.mardous.booming.util.sort.SortOrder
import com.mardous.booming.util.sort.sortedPlaylists

interface PlaylistRepository {
    fun getSongs(playListId: Long): LiveData<List<SongEntity>>
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
    suspend fun searchPlaylists(searchQuery: String): List<PlaylistWithSongs>
    suspend fun searchPlaylistSongs(playlistId: Long, searchQuery: String): List<SongEntity>
    suspend fun insertSongs(songs: List<SongEntity>)
    suspend fun deletePlaylistEntities(playlistEntities: List<PlaylistEntity>)
    suspend fun renamePlaylistEntity(playlistId: Long, name: String)
    suspend fun updatePlaylist(playlistId: Long, newName: String, customCoverUri: String?, description: String?)
    suspend fun deleteSongsInPlaylist(songs: List<SongEntity>)
    suspend fun deletePlaylistSongs(playlists: List<PlaylistEntity>)
    suspend fun favoritePlaylist(): PlaylistEntity
    suspend fun checkFavoritePlaylist(): PlaylistEntity?
    suspend fun favoriteSongs(): List<SongEntity>
    fun favoriteObservable(): LiveData<List<SongEntity>>
    suspend fun toggleFavorite(song: Song): Boolean
    suspend fun isSongFavorite(songEntity: SongEntity): List<SongEntity>
    suspend fun isSongFavorite(songId: Long): Boolean
    suspend fun removeSongFromPlaylist(songEntity: SongEntity)
    suspend fun checkSongExistInPlaylist(playlistEntity: PlaylistEntity, song: Song): Boolean
    suspend fun deleteSongFromAllPlaylists(songId: Long)
}

class RealPlaylistRepository(
    private val context: Context,
    private val songRepository: SongRepository,
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun getSongs(playListId: Long): LiveData<List<SongEntity>> =
        playlistDao.songsFromPlaylist(playListId)

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
        playlistDao.playlistsWithSongs().let {
            if (sorted) it.sortedPlaylists(SortOrder.playlistSortOrder) else it
        }

    override suspend fun playlistWithSongs(playlistId: Long): PlaylistWithSongs =
        playlistDao.playlistWithSongs(playlistId) ?: PlaylistWithSongs.Empty

    override fun playlistWithSongsObservable(playlistId: Long): LiveData<PlaylistWithSongs> =
        playlistDao.playlistWithSongsObservable(playlistId).map { result -> result ?: PlaylistWithSongs.Empty }

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

    override suspend fun updatePlaylist(playlistId: Long, newName: String, customCoverUri: String?, description: String?) =
        playlistDao.updatePlaylist(playlistId, newName, customCoverUri, description)

    override suspend fun deleteSongsInPlaylist(songs: List<SongEntity>) {
        songs.forEach {
            playlistDao.deleteSongFromPlaylist(it.playlistCreatorId, it.id)
        }
    }

    override suspend fun deletePlaylistSongs(playlists: List<PlaylistEntity>) =
        playlists.forEach {
            playlistDao.deletePlaylistSongs(it.playListId)
        }

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

    override suspend fun favoriteSongs(): List<SongEntity> {
        val favorite = context.getString(R.string.favorites_label)
        return if (playlistDao.playlist(favorite).isNotEmpty())
            playlistDao.favoritesSongs(
                playlistDao.playlist(favorite).first().playListId
            ) else emptyList()
    }

    override fun favoriteObservable(): LiveData<List<SongEntity>> =
        playlistDao.favoritesSongsLiveData(context.getString(R.string.favorites_label))

    override suspend fun toggleFavorite(song: Song): Boolean {
        val playlist = favoritePlaylist()
        val songEntity = song.toSongEntity(playlist.playListId)
        val isFavorite = isSongFavorite(songEntity).isNotEmpty()
        return if (isFavorite) {
            removeSongFromPlaylist(songEntity)
            false
        } else {
            insertSongs(listOf(songEntity))
            true
        }
    }

    override suspend fun isSongFavorite(songEntity: SongEntity): List<SongEntity> =
        playlistDao.isSongExistsInPlaylist(
            songEntity.playlistCreatorId,
            songEntity.id
        )

    override suspend fun isSongFavorite(songId: Long): Boolean {
        return playlistDao.isSongExistsInPlaylist(
            playlistDao.playlist(context.getString(R.string.favorites_label)).firstOrNull()?.playListId
                ?: -1,
            songId
        ).isNotEmpty()
    }

    override suspend fun removeSongFromPlaylist(songEntity: SongEntity) =
        playlistDao.deleteSongFromPlaylist(songEntity.playlistCreatorId, songEntity.id)

    override suspend fun checkSongExistInPlaylist(playlistEntity: PlaylistEntity, song: Song): Boolean =
        playlistDao.checkSongExistInPlaylist(playlistEntity.playListId, song.id)

    override suspend fun deleteSongFromAllPlaylists(songId: Long) {
        playlistDao.deleteSongFromAllPlaylists(songId)
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