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

package com.mardous.booming.repository

import android.annotation.SuppressLint
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.util.Log
import com.mardous.booming.database.InclExclDao
import com.mardous.booming.database.InclExclEntity
import com.mardous.booming.extensions.files.getCanonicalPathSafe
import com.mardous.booming.extensions.hasQ
import com.mardous.booming.extensions.hasR
import com.mardous.booming.extensions.utilities.getStringSafe
import com.mardous.booming.extensions.utilities.mapIfValid
import com.mardous.booming.extensions.utilities.takeOrDefault
import com.mardous.booming.model.Album
import com.mardous.booming.model.Song
import com.mardous.booming.providers.MediaQueryDispatcher
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.sort.SortOrder
import com.mardous.booming.util.sort.sortedSongs

interface SongRepository {
    fun songs(): List<Song>
    fun songs(query: String): List<Song>
    fun songs(cursor: Cursor?): List<Song>
    fun sortedSongs(cursor: Cursor?): List<Song>
    fun songsByFilePath(filePath: String, ignoreBlacklist: Boolean = false): List<Song>
    fun song(cursor: Cursor?): Song
    fun song(songId: Long): Song
    suspend fun initializeBlacklist()
}

/**
 * @author Christians M. A. (mardous)
 */
@SuppressLint("InlinedApi")
class RealSongRepository(private val inclExclDao: InclExclDao) : SongRepository {

    override fun songs(): List<Song> {
        return sortedSongs(makeSongCursor(null, null))
    }

    override fun songs(query: String): List<Song> {
        return songs(
            makeSongCursor(
                "${AudioColumns.TITLE} LIKE ? OR ${AudioColumns.ARTIST} LIKE ?",
                arrayOf("%$query%", "%$query%")
            )
        )
    }

    override fun songs(cursor: Cursor?): List<Song> {
        return cursor.use {
            it.mapIfValid { getSongFromCursorImpl(this) }
        }
    }

    override fun sortedSongs(cursor: Cursor?): List<Song> {
        val songs = songs(cursor)
        return songs.sortedSongs(SortOrder.songSortOrder)
    }

    override fun song(cursor: Cursor?): Song {
        return cursor.use {
            it.takeOrDefault(Song.emptySong) { getSongFromCursorImpl(this) }
        }
    }

    private fun getSongFromCursorImpl(cursor: Cursor): Song {
        val id = cursor.getLong(0)
        val data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
        val trackNumber = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK))
        val year = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR))
        val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
        val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
        val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED))
        val dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED))
        val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
        val albumName = cursor.getStringSafe(MediaStore.Audio.Media.ALBUM) ?: Album.UNKNOWN_ALBUM_DISPLAY_NAME
        val artistId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID))
        val artistName = cursor.getStringSafe(MediaStore.Audio.Media.ARTIST) ?: ""
        val albumArtistName = cursor.getStringSafe(MediaStore.Audio.Media.ALBUM_ARTIST)
        val genreName = cursor.getStringSafe(MediaStore.Audio.Media.GENRE)
        return Song(
            id,
            data,
            title,
            trackNumber,
            year,
            size,
            duration,
            dateAdded,
            dateModified,
            albumId,
            albumName,
            artistId,
            artistName,
            albumArtistName,
            genreName
        )
    }

    override fun song(songId: Long): Song {
        return song(makeSongCursor("${AudioColumns._ID}=?", arrayOf(songId.toString())))
    }

    override fun songsByFilePath(filePath: String, ignoreBlacklist: Boolean): List<Song> {
        return songs(makeSongCursor("${AudioColumns.DATA}=?", arrayOf(filePath), ignoreBlacklist = ignoreBlacklist))
    }

    override suspend fun initializeBlacklist() {
        val excludedPaths = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
        )
        for (path in excludedPaths) {
            inclExclDao.insertPath(InclExclEntity(path.getCanonicalPathSafe(), InclExclDao.BLACKLIST))
        }
    }

    fun makeSongCursor(queryDispatcher: MediaQueryDispatcher, ignoreBlacklist: Boolean = false): Cursor? {
        val minimumSongDuration = Preferences.minimumSongDuration
        if (minimumSongDuration > 0) {
            queryDispatcher.addSelection("${AudioColumns.DURATION} >= ${minimumSongDuration * 1000}")
        }

        if (!ignoreBlacklist) {
            // Whitelist
            if (Preferences.whitelistEnabled) {
                val whitelisted = inclExclDao.whitelistPaths().map { it.path }
                if (whitelisted.isNotEmpty()) {
                    queryDispatcher.addSelection(generateWhitelistSelection(whitelisted.size))
                    queryDispatcher.addArguments(*addLibrarySelectionValues(whitelisted))
                }
            }

            // Blacklist
            if (Preferences.blacklistEnabled) {
                val blacklisted = inclExclDao.blackListPaths().map { it.path }
                if (blacklisted.isNotEmpty()) {
                    queryDispatcher.addSelection(generateBlacklistSelection(blacklisted.size))
                    queryDispatcher.addArguments(*addLibrarySelectionValues(blacklisted))
                }
            }
        }

        return try {
            queryDispatcher.dispatch()
        } catch (e: SecurityException) {
            Log.e(TAG, "Couldn't load songs", e)
            null
        }
    }

    fun makeSongCursor(
        selection: String?,
        selectionValues: Array<String>?,
        sortOrder: String? = null,
        ignoreBlacklist: Boolean = false
    ): Cursor? {
        val queryDispatcher = MediaQueryDispatcher()
            .setProjection(getBaseProjection())
            .setSelection(BASE_SELECTION)
            .setSelectionArguments(selectionValues)
            .addSelection(selection)
            .setSortOrder(sortOrder ?: MediaStore.Audio.Media.DEFAULT_SORT_ORDER)
        return makeSongCursor(queryDispatcher, ignoreBlacklist)
    }

    private fun generateWhitelistSelection(pathCount: Int): String {
        val builder = StringBuilder("(${AudioColumns.DATA} LIKE ?")
        for (i in 1 until pathCount) {
            builder.append(" OR ${AudioColumns.DATA} LIKE ?")
        }
        return builder.append(")").toString()
    }

    private fun generateBlacklistSelection(pathCount: Int): String {
        val builder = StringBuilder("${AudioColumns.DATA} NOT LIKE ?")
        for (i in 1 until pathCount) {
            builder.append(" AND ${AudioColumns.DATA} NOT LIKE ?")
        }
        return builder.toString()
    }

    private fun addLibrarySelectionValues(paths: List<String>): Array<String> {
        return Array(paths.size) { index -> "${paths[index]}%" }
    }

    companion object {
        private val TAG = RealSongRepository::class.java.simpleName

        const val BASE_SELECTION = "${AudioColumns.TITLE} != '' AND ${AudioColumns.IS_MUSIC} = 1"

        @SuppressLint("InlinedApi")
        private val BASE_PROJECTION = arrayOf(
            AudioColumns._ID, //0
            AudioColumns.DATA, //1
            AudioColumns.TITLE, //2
            AudioColumns.TRACK, //3
            AudioColumns.YEAR, //4
            AudioColumns.SIZE, //5
            AudioColumns.DURATION, //6
            AudioColumns.DATE_ADDED, //7
            AudioColumns.DATE_MODIFIED, //8
            AudioColumns.ALBUM_ID, //9
            AudioColumns.ALBUM, //10
            AudioColumns.ARTIST_ID, //11
            AudioColumns.ARTIST, //12
            AudioColumns.ALBUM_ARTIST, //13
        )

        fun getAudioContentUri(): Uri = if (hasQ())
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        fun getBaseProjection(idColumn: String = AudioColumns._ID): Array<String> {
            var baseProjection = BASE_PROJECTION
            if (hasR()) {
                baseProjection += AudioColumns.GENRE
            }
            if (idColumn != AudioColumns._ID) {
                return baseProjection.copyOf().apply { set(0, idColumn) }
            }
            return baseProjection
        }
    }
}