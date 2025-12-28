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

package com.mardous.booming.data.local.room

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM PlaylistEntity WHERE playlist_name = :name")
    fun playlist(name: String): List<PlaylistEntity>

    @Query("SELECT * FROM PlaylistEntity")
    suspend fun playlists(): List<PlaylistEntity>

    @Insert
    suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE PlaylistEntity SET playlist_name = :name WHERE playlist_id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Transaction
    @Query("SELECT * FROM PlaylistEntity")
    suspend fun playlistsWithSongs(): List<PlaylistWithSongs>

    @Transaction
    @Query("SELECT * FROM PlaylistEntity WHERE playlist_id = :playlistId")
    fun playlistWithSongsObservable(playlistId: Long): LiveData<PlaylistWithSongs?>

    @Transaction
    @Query("SELECT * FROM PlaylistEntity WHERE playlist_id = :playlistId")
    fun playlistWithSongs(playlistId: Long): PlaylistWithSongs?

    @Transaction
    @Query("SELECT * FROM PlaylistEntity WHERE playlist_name LIKE :playlistName")
    fun searchPlaylists(playlistName: String): List<PlaylistWithSongs>

    @Transaction
    @Query("SELECT * FROM SongEntity WHERE playlist_creator_id = :playlistId AND title LIKE :songName")
    fun searchSongs(playlistId: Long, songName: String): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongsToPlaylist(songEntities: List<SongEntity>)

    @Query("SELECT * FROM SongEntity WHERE playlist_creator_id = :playlistId AND id = :songId LIMIT 1")
    suspend fun findSongInPlaylist(playlistId: Long, songId: Long): SongEntity?

    @Query("SELECT * FROM SongEntity WHERE playlist_creator_id = :playlistId AND id IN(:songIds)")
    suspend fun findSongsInPlaylist(playlistId: Long, songIds: List<Long>): List<SongEntity>

    @Query("SELECT * FROM SongEntity WHERE playlist_creator_id = :playlistId ORDER BY song_key asc")
    fun songsFromPlaylistObservable(playlistId: Long): LiveData<List<SongEntity>>

    @Query("SELECT * FROM SongEntity WHERE playlist_creator_id = :playlistId ORDER BY song_key asc")
    suspend fun songsFromPlaylist(playlistId: Long): List<SongEntity>

    @Transaction
    suspend fun removeSongsAndDeletePlaylists(playlistIds: List<Long>) {
        deleteAllSongsFromPlaylists(playlistIds)
        deletePlaylists(playlistIds)
    }

    @Query("DELETE FROM PlaylistEntity WHERE playlist_id IN (:playlistIds)")
    suspend fun deletePlaylists(playlistIds: List<Long>)

    @Query("DELETE FROM SongEntity WHERE playlist_creator_id = :playlistId AND id IN(:songIds)")
    suspend fun deleteSongsFromPlaylist(playlistId: Long, songIds: List<Long>)

    @Delete
    suspend fun deleteSongsFromPlaylists(songs: List<SongEntity>)

    @Query("DELETE FROM SongEntity WHERE playlist_creator_id IN(:playlistIds)")
    suspend fun deleteAllSongsFromPlaylists(playlistIds: List<Long>)

    @Query("DELETE FROM SongEntity WHERE id IN (:songIds)")
    suspend fun deleteSongsFromAllPlaylists(songIds: List<Long>)

    @RewriteQueriesToDropUnusedColumns
    @Query("""
    SELECT * FROM SongEntity,
    (SELECT playlist_id FROM PlaylistEntity WHERE playlist_name = :playlistName LIMIT 1) AS playlist
    WHERE playlist_creator_id = playlist.playlist_id""")
    fun favoritesSongsFlow(playlistName: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM SongEntity WHERE playlist_creator_id = :playlistId")
    fun favoritesSongs(playlistId: Long): List<SongEntity>

    @Query("SELECT EXISTS(SELECT * FROM PlaylistEntity WHERE playlist_id = :playlistId)")
    fun checkPlaylistExists(playlistId: Long): LiveData<Boolean>

    @Query("SELECT EXISTS(SELECT * FROM SongEntity WHERE id = :songId AND playlist_creator_id = :playlistId)")
    fun checkSongExistInPlaylist(playlistId: Long, songId: Long): Boolean
}