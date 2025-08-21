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
import android.provider.MediaStore.Audio.AudioColumns
import androidx.lifecycle.LiveData
import com.mardous.booming.core.legacy.HistoryStore
import com.mardous.booming.core.legacy.SongPlayCountStore
import com.mardous.booming.data.local.MediaQueryDispatcher
import com.mardous.booming.data.local.room.HistoryDao
import com.mardous.booming.data.local.room.HistoryEntity
import com.mardous.booming.data.local.room.PlayCountDao
import com.mardous.booming.data.local.room.PlayCountEntity
import com.mardous.booming.data.mapper.toHistoryEntity
import com.mardous.booming.data.mapper.toPlayCount
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.ContentType
import com.mardous.booming.data.model.Song
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.cursor.SortedCursorUtil
import com.mardous.booming.util.cursor.SortedLongCursor

interface SmartRepository {
    fun topPlayedSongs(): List<Song>
    fun topAlbums(): List<Album>
    fun topAlbumArtists(): List<Artist>
    fun recentSongs(): List<Song>
    fun recentSongs(query: String, contentType: ContentType): List<Song>
    fun recentAlbums(): List<Album>
    fun recentAlbumArtists(): List<Artist>
    fun notRecentlyPlayedSongs(): List<Song>
    suspend fun playCountSongs(): List<PlayCountEntity>
    suspend fun playCountEntities(songs: List<Song>): List<PlayCountEntity>
    suspend fun findSongInPlayCount(songId: Long): PlayCountEntity?
    suspend fun upsertSongInPlayCount(playCountEntity: PlayCountEntity)
    suspend fun deleteSongInPlayCount(playCountEntity: PlayCountEntity)
    suspend fun clearPlayCount()
    fun historySongs(): List<HistoryEntity>
    fun historySongsObservable(): LiveData<List<HistoryEntity>>
    suspend fun upsertSongInHistory(currentSong: Song)
    suspend fun deleteSongInHistory(songId: Long)
    suspend fun clearSongHistory()
}

class RealSmartRepository(
    private val context: Context,
    private val songRepository: RealSongRepository,
    private val albumRepository: RealAlbumRepository,
    private val artistRepository: RealArtistRepository,
    private val historyDao: HistoryDao,
    private val playCountDao: PlayCountDao,
) : SmartRepository {

    override fun topPlayedSongs(): List<Song> = songRepository.songs(makeTopTracksCursor(context))

    override fun topAlbums(): List<Album> =
        albumRepository.splitIntoAlbums(topPlayedSongs(), sorted = false)

    override fun topAlbumArtists(): List<Artist> =
        artistRepository.splitIntoAlbumArtists(topAlbums())

    override fun recentSongs(): List<Song> =
        songRepository.songs(makeLastAddedCursor(null, ContentType.RecentSongs))

    override fun recentSongs(query: String, contentType: ContentType): List<Song> =
        songRepository.songs(makeLastAddedCursor(query, contentType))

    override fun recentAlbums(): List<Album> =
        albumRepository.splitIntoAlbums(recentSongs(), sorted = false)

    override fun recentAlbumArtists(): List<Artist> =
        artistRepository.splitIntoAlbumArtists(recentAlbums())

    override fun notRecentlyPlayedSongs(): List<Song> {
        val cursor = songRepository.makeSongCursor(null, null, AudioColumns.DATE_ADDED + " ASC")
        val allSongs = songRepository.songs(cursor).toMutableList()
        val playedSongs = songRepository.songs(makePlayedTracksCursor(context))
        val notRecentlyPlayedSongs = songRepository.songs(makeNotRecentPlayedTracksCursor(context))
        allSongs.removeAll(playedSongs)
        allSongs.addAll(notRecentlyPlayedSongs)
        return allSongs
    }

    override suspend fun playCountSongs(): List<PlayCountEntity> =
        playCountDao.playCountSongs()

    override suspend fun playCountEntities(songs: List<Song>): List<PlayCountEntity> = songs.map {
        playCountDao.findSongExistInPlayCount(it.id) ?: it.toPlayCount()
    }

    override suspend fun findSongInPlayCount(songId: Long): PlayCountEntity? =
        playCountDao.findSongExistInPlayCount(songId)

    override suspend fun upsertSongInPlayCount(playCountEntity: PlayCountEntity) =
        playCountDao.upsertSongInPlayCount(playCountEntity)

    override suspend fun deleteSongInPlayCount(playCountEntity: PlayCountEntity) =
        playCountDao.deleteSongInPlayCount(playCountEntity)

    override suspend fun clearPlayCount() {
        playCountDao.clearPlayCount()
    }

    override fun historySongs(): List<HistoryEntity> = historyDao.historySongs()

    override fun historySongsObservable(): LiveData<List<HistoryEntity>> =
        historyDao.observableHistorySongs()

    override suspend fun upsertSongInHistory(currentSong: Song) =
        historyDao.upsertSongInHistory(currentSong.toHistoryEntity(System.currentTimeMillis()))

    override suspend fun deleteSongInHistory(songId: Long) {
        historyDao.deleteSongInHistory(songId)
    }

    override suspend fun clearSongHistory() {
        historyDao.clearHistory()
    }

    private fun makeLastAddedCursor(query: String?, contentType: ContentType): Cursor? {
        val cutoff = Preferences.getLastAddedCutoff().interval
        val queryDispatcher = MediaQueryDispatcher()
            .setProjection(RealSongRepository.getBaseProjection())
            .setSelection("${AudioColumns.DATE_ADDED}>?")
            .setSelectionArguments(arrayOf(cutoff.toString()))
            .setSortOrder("${AudioColumns.DATE_ADDED} DESC")
        if (!query.isNullOrEmpty()) {
            when (contentType) {
                ContentType.RecentAlbums -> queryDispatcher.addSelection("${AudioColumns.ALBUM} LIKE ?")
                ContentType.RecentArtists -> queryDispatcher.addSelection("${AudioColumns.ALBUM_ARTIST} LIKE ?")
                ContentType.RecentSongs -> queryDispatcher.addSelection("${AudioColumns.TITLE} LIKE ?")
                else -> error("Content type is not valid: $contentType")
            }
            queryDispatcher.addArguments("%$query%")
        }
        return songRepository.makeSongCursor(queryDispatcher)
    }

    private fun makeTopTracksCursor(context: Context): Cursor? {
        val cursor = makeCursorFromDatabase(
            SongPlayCountStore.getInstance(context).getTopPlayedResults(
                NUMBER_OF_TOP_TRACKS
            ),
            SongPlayCountStore.SongPlayCountColumns.ID
        )
        return makeCleanCursor(cursor) { missingId: Long ->
            SongPlayCountStore.getInstance(context).removeItem(missingId)
        }
    }

    private fun makeNotRecentPlayedTracksCursor(context: Context): Cursor? {
        return makeRecentTracksCursorAndCleanUpDatabaseImpl(context, false, reverseOrder = true)
    }

    private fun makePlayedTracksCursor(context: Context): Cursor? {
        return makeRecentTracksCursorAndCleanUpDatabaseImpl(context, true, reverseOrder = false)
    }

    private fun makeRecentTracksCursorAndCleanUpDatabaseImpl(
        context: Context,
        ignoreCutoffTime: Boolean,
        reverseOrder: Boolean
    ): Cursor? {
        val cutoff = if (ignoreCutoffTime) 0 else Preferences.getHistoryCutoff().interval
        val cursor = makeCursorFromDatabase(
            HistoryStore.getInstance(context).queryRecentIds(cutoff * if (reverseOrder) -1 else 1),
            HistoryStore.RecentStoreColumns.ID
        )
        return makeCleanCursor(cursor) { missingId: Long ->
            HistoryStore.getInstance(context).removeSongId(missingId)
        }
    }

    private fun makeCursorFromDatabase(query: Cursor?, idColumn: String): SortedLongCursor? {
        query.use {
            if (it != null) return SortedCursorUtil.makeSortedCursor(it, it.getColumnIndex(idColumn))
        }
        return null
    }

    private fun makeCleanCursor(retCursor: SortedLongCursor?, missingIdConsumer: (Long) -> Unit): SortedLongCursor? {
        if (retCursor != null) {
            val missingIds: List<Long> = retCursor.missingIds
            if (missingIds.isNotEmpty()) {
                for (missingId in missingIds) {
                    missingIdConsumer(missingId)
                }
            }
        }
        return retCursor
    }

    companion object {
        const val NUMBER_OF_TOP_TRACKS = 100
    }
}