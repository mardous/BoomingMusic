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
import com.mardous.booming.data.local.MediaQueryDispatcher
import com.mardous.booming.data.local.room.HistoryDao
import com.mardous.booming.data.local.room.HistoryEntity
import com.mardous.booming.data.local.room.PlayCountDao
import com.mardous.booming.data.local.room.PlayCountEntity
import com.mardous.booming.data.mapper.fromPlayCountToSongs
import com.mardous.booming.data.mapper.toHistoryEntity
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.ContentType
import com.mardous.booming.data.model.Song
import com.mardous.booming.util.Preferences

interface SmartRepository {
    suspend fun topAlbums(): List<Album>
    suspend fun topAlbumArtists(): List<Artist>
    suspend fun recentSongs(): List<Song>
    suspend fun recentSongs(query: String, contentType: ContentType): List<Song>
    suspend fun recentAlbums(): List<Album>
    suspend fun recentAlbumArtists(): List<Artist>
    suspend fun notRecentlyPlayedSongs(): List<Song>
    suspend fun playCountSongs(): List<PlayCountEntity>
    suspend fun findSongsInPlayCount(songs: List<Song>): List<PlayCountEntity>
    suspend fun findSongInPlayCount(songId: Long): PlayCountEntity?
    suspend fun deleteSongInPlayCount(playCountEntity: PlayCountEntity)
    suspend fun insetOrIncrementPlayCount(song: Song, timePlayed: Long)
    suspend fun insetOrIncrementSkipCount(song: Song)
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

    override suspend fun topAlbums(): List<Album> =
        albumRepository.splitIntoAlbums(playCountSongs().fromPlayCountToSongs(), sorted = false)

    override suspend fun topAlbumArtists(): List<Artist> =
        artistRepository.splitIntoAlbumArtists(topAlbums())

    override suspend fun recentSongs(): List<Song> =
        songRepository.songs(makeLastAddedCursor(null, ContentType.RecentSongs))

    override suspend fun recentSongs(query: String, contentType: ContentType): List<Song> =
        songRepository.songs(makeLastAddedCursor(query, contentType))

    override suspend fun recentAlbums(): List<Album> =
        albumRepository.splitIntoAlbums(recentSongs(), sorted = false)

    override suspend fun recentAlbumArtists(): List<Artist> =
        artistRepository.splitIntoAlbumArtists(recentAlbums())

    override suspend fun notRecentlyPlayedSongs(): List<Song> {
        return buildList {
            addAll(songRepository.songs())

            val playedSongIds = historyDao.playedSongIds()
            removeAll { it.id in playedSongIds }

            val oldSongIds = historyDao.notPlayedSongIds(
                cutoff = Preferences.getHistoryCutoff(context).interval
            )
            val oldSongs = songRepository.songs(
                songRepository.makeSongCursor(
                    selection = "${AudioColumns._ID} IN (${oldSongIds.joinToString(",") { "?" }})",
                    selectionValues = oldSongIds.map { it.toString() }.toTypedArray()
                )
            )
            addAll(oldSongs)
        }
    }


    override suspend fun playCountSongs(): List<PlayCountEntity> =
        playCountDao.playCountSongs()

    override suspend fun findSongsInPlayCount(songs: List<Song>): List<PlayCountEntity> {
        if (songs.isEmpty()) return emptyList()
        return playCountDao.findSongsExistInPlayCount(songs.map { it.id })
    }

    override suspend fun findSongInPlayCount(songId: Long): PlayCountEntity? =
        playCountDao.findSongExistInPlayCount(songId)

    override suspend fun deleteSongInPlayCount(playCountEntity: PlayCountEntity) =
        playCountDao.deleteSongInPlayCount(playCountEntity)

    override suspend fun insetOrIncrementPlayCount(song: Song, timePlayed: Long) =
        playCountDao.insertOrIncrementPlayCount(song, timePlayed)

    override suspend fun insetOrIncrementSkipCount(song: Song) =
        playCountDao.insertOrIncrementSkipCount(song)

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

    companion object {
        const val NUMBER_OF_TOP_TRACKS = 100
    }
}