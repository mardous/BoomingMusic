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

package com.mardous.booming.data.repository

import android.annotation.SuppressLint
import android.content.ClipDescription
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.OpenableColumns
import android.util.Log
import androidx.media3.common.MediaItem
import com.mardous.booming.core.sort.SongSortMode
import com.mardous.booming.data.local.MediaQueryDispatcher
import com.mardous.booming.data.local.room.InclExclDao
import com.mardous.booming.data.local.room.InclExclEntity
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.UnindexedSong
import com.mardous.booming.extensions.files.getCanonicalPathSafe
import com.mardous.booming.extensions.hasQ
import com.mardous.booming.extensions.hasR
import com.mardous.booming.extensions.utilities.getStringSafe
import com.mardous.booming.extensions.utilities.mapIfValid
import com.mardous.booming.extensions.utilities.takeOrDefault
import com.mardous.booming.playback.resolvedFromFile
import com.mardous.booming.util.Preferences
import okhttp3.internal.toLongOrDefault

interface SongRepository {
    fun song(songId: Long): Song
    fun song(cursor: Cursor?): Song
    fun songs(): List<Song>
    fun songs(songIds: List<Long>): List<Song>
    fun songs(query: String): List<Song>
    fun songs(cursor: Cursor?): List<Song>
    suspend fun songsByUri(uri: Uri): List<Song>
    suspend fun songsByMediaItems(mediaItems: List<MediaItem>, ignoreBlacklist: Boolean): Pair<List<Song>, List<MediaItem>>
    suspend fun songByMediaItem(mediaItem: MediaItem?, ignoreBlacklist: Boolean): Song
    fun songByFilePath(filePath: String, ignoreBlacklist: Boolean = false): Song
    suspend fun initializeBlacklist()
}

@SuppressLint("InlinedApi")
class RealSongRepository(
    private val context: Context,
    private val inclExclDao: InclExclDao
) : SongRepository {

    private val nonIndexedFiles = mutableMapOf<String, UnindexedSong>()

    override fun song(songId: Long): Song {
        return resolveSongById(songId, false)
    }

    override fun song(cursor: Cursor?): Song {
        return resolveSongFromCursor(cursor, false)
    }

    override fun songs(): List<Song> {
        val songs = songs(makeSongCursor(null, null))
        return with(SongSortMode.AllSongs) { songs.sorted() }
    }

    override fun songs(songIds: List<Long>): List<Song> {
        val selection = "${AudioColumns._ID} IN (${songIds.joinToString(",") { "?" }})"
        val selectionArgs = songIds.map { it.toString() }.toTypedArray()
        return songs(makeSongCursor(selection, selectionArgs))
    }

    override fun songs(query: String): List<Song> {
        return songs(
            makeSongCursor(
                selection = "${AudioColumns.TITLE} LIKE ? OR ${AudioColumns.ARTIST} LIKE ? OR ${AudioColumns.ALBUM} LIKE ?",
                selectionValues = arrayOf("%$query%", "%$query%", "%$query%")
            )
        )
    }

    override fun songs(cursor: Cursor?): List<Song> {
        return cursor.use {
            it.mapIfValid { getSongFromCursorImpl(this) }
        }
    }

    override suspend fun songsByUri(uri: Uri): List<Song> {
        var songs: List<Song> = emptyList()
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val authority = uri.authority ?: ""
            when (authority) {
                MediaStore.AUTHORITY -> {
                    val songId = uri.lastPathSegment?.toLongOrNull()
                    if (songId != null) {
                        songs = listOf(resolveSongById(songId, resolvedFromFile = true))
                    }
                }

                else -> {
                    try {
                        if (hasQ()) {
                            val context = context.applicationContext
                            val id = MediaStore.getMediaUri(context, uri)
                                ?.lastPathSegment?.toLongOrNull()
                            if (id != null) {
                                songs = listOf(resolveSongById(id, resolvedFromFile = true))
                            }
                        } else {
                            if (authority == "com.android.providers.media.documents") {
                                val id = getSongIdFromMediaProvider(uri)
                                if (id > -1) {
                                    songs = listOf(resolveSongById(id, resolvedFromFile = true))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to retrieve song info from Uri: $uri", e)
                    }
                }
            }
        } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val path = uri.path
            if (path != null) {
                songs = listOf(songByFilePath(path, true))
            }
        }

        if (songs.isEmpty() && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            songs = listOfNotNull(findSongFromFileProviderUri(uri))
        }

        if (songs.isEmpty()) {
            Log.e(TAG, "Couldn't resolve songs from Uri: $uri")
        }

        return songs.filter { it != Song.emptySong }
    }

    override suspend fun songsByMediaItems(
        mediaItems: List<MediaItem>,
        ignoreBlacklist: Boolean
    ): Pair<List<Song>, List<MediaItem>> {
        if (mediaItems.isEmpty()) return (emptyList<Song>() to mediaItems)

        // Preload whitelist and blacklist if enabled to avoid redundant Room queries in the loop
        val whitelistedPaths = if (Preferences.whitelistEnabled) {
            inclExclDao.whitelistPaths().map { it.path }
        } else null

        val blacklistedPaths = if (Preferences.blacklistEnabled) {
            inclExclDao.blackListPaths().map { it.path }
        } else null

        val ids = mediaItems.map { it.mediaId }
        val allSongs = buildList {
            ids.chunked(900).forEach { chunk ->
                val selection = "${AudioColumns._ID} IN (${chunk.joinToString(",") { "?" }})"
                val selectionArgs = chunk.toTypedArray()
                addAll(
                    songs(
                        makeSongCursor(
                            selection = selection,
                            selectionValues = selectionArgs,
                            whitelistedPaths = whitelistedPaths,
                            blacklistedPaths = blacklistedPaths,
                            ignoreBlacklist = ignoreBlacklist
                        )
                    )
                )
            }
        }

        val songMap = allSongs.associateBy { it.id.toString() }

        val resultSongs = ArrayList<Song>(mediaItems.size)
        val missing = ArrayList<MediaItem>()

        for (item in mediaItems) {
            val song = songMap[item.mediaId]

            val resolvedFromFile = item.resolvedFromFile
            if (song != null && song != Song.emptySong) {
                resultSongs.add(if (resolvedFromFile) song.copy(resolvedFromFile = true) else song)
            } else {
                val nonIndexedFile = nonIndexedFiles[item.mediaId]
                if (nonIndexedFile != null) {
                    resultSongs.add(nonIndexedFile)
                } else {
                    missing.add(item)
                }
            }
        }

        return resultSongs to missing
    }

    override suspend fun songByMediaItem(mediaItem: MediaItem?, ignoreBlacklist: Boolean): Song {
        if (mediaItem != null) {
            // If we get `resolvedFromFile=true`, we're dealing with a song coming
            // from an external app. This could be, for example, a file explorer;
            // in that case, the user might be trying to play an audio file from the
            // blacklisted folders. We must ensure that this information is retained
            // in the resulting song, which will also let PlaybackService know that
            // this song shouldn't be included in scrobbling, history, or play counts.
            val resolvedFromFile = mediaItem.resolvedFromFile
            val song = resolveSongFromCursor(
                cursor = makeSongCursor(
                    selection = "${AudioColumns._ID}=?",
                    selectionValues = arrayOf(mediaItem.mediaId),
                    ignoreBlacklist = ignoreBlacklist
                ),
                resolvedFromFile = resolvedFromFile
            )
            if (song == Song.emptySong) {
                val nonIndexedFile = nonIndexedFiles[mediaItem.mediaId]
                if (nonIndexedFile != null) return nonIndexedFile
            }
        }
        return Song.emptySong
    }

    override fun songByFilePath(filePath: String, ignoreBlacklist: Boolean): Song {
        return resolveSongFromCursor(
            cursor = makeSongCursor(
                selection = "${AudioColumns.DATA}=?",
                selectionValues = arrayOf(filePath),
                ignoreBlacklist = ignoreBlacklist
            ),
            resolvedFromFile = ignoreBlacklist
        )
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

    private fun resolveSongById(songId: Long, resolvedFromFile: Boolean): Song {
        return resolveSongFromCursor(
            cursor = makeSongCursor("${AudioColumns._ID}=?", arrayOf(songId.toString())),
            resolvedFromFile = resolvedFromFile
        )
    }

    private fun resolveSongFromCursor(cursor: Cursor?, resolvedFromFile: Boolean): Song {
        return cursor.use {
            it.takeOrDefault(Song.emptySong) { getSongFromCursorImpl(this, resolvedFromFile) }
        }
    }

    fun makeSongCursor(
        queryDispatcher: MediaQueryDispatcher,
        ignoreBlacklist: Boolean = false,
        whitelistedPaths: List<String>? = null,
        blacklistedPaths: List<String>? = null
    ): Cursor? {
        val minimumSongDuration = Preferences.minimumSongDuration
        if (minimumSongDuration > 0) {
            queryDispatcher.addSelection("${AudioColumns.DURATION} >= ${minimumSongDuration * 1000}")
        }

        if (!ignoreBlacklist) {
            // Whitelist
            if (Preferences.whitelistEnabled) {
                val whitelisted = whitelistedPaths ?: inclExclDao.whitelistPaths().map { it.path }
                if (whitelisted.isNotEmpty()) {
                    queryDispatcher.addSelection(generateWhitelistSelection(whitelisted.size))
                    queryDispatcher.addArguments(*addLibrarySelectionValues(whitelisted))
                }
            }

            // Blacklist
            if (Preferences.blacklistEnabled) {
                val blacklisted = blacklistedPaths ?: inclExclDao.blackListPaths().map { it.path }
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
        ignoreBlacklist: Boolean = false,
        whitelistedPaths: List<String>? = null,
        blacklistedPaths: List<String>? = null
    ): Cursor? {
        val queryDispatcher = MediaQueryDispatcher()
            .setProjection(getBaseProjection())
            .setSelection(BASE_SELECTION)
            .setSelectionArguments(selectionValues)
            .addSelection(selection)
            .setSortOrder(sortOrder ?: MediaStore.Audio.Media.DEFAULT_SORT_ORDER)
        return makeSongCursor(queryDispatcher, ignoreBlacklist, whitelistedPaths, blacklistedPaths)
    }

    private fun generateWhitelistSelection(pathCount: Int): String =
        buildString {
            append("(")
            append((1..pathCount).joinToString(" OR ") { "${AudioColumns.DATA} LIKE ?" })
            append(")")
        }

    private fun generateBlacklistSelection(pathCount: Int): String =
        (1..pathCount).joinToString(" AND ") { "${AudioColumns.DATA} NOT LIKE ?" }


    private fun addLibrarySelectionValues(paths: List<String>): Array<String> {
        return Array(paths.size) { index -> "${paths[index]}%" }
    }

    private fun getSongIdFromMediaProvider(uri: Uri): Long {
        val docId = DocumentsContract.getDocumentId(uri)
        val parts = docId.split(":")
        return if (parts.size == 2) parts[1].toLongOrDefault(-1) else -1
    }

    private fun findSongFromFileProviderUri(uri: Uri): Song? {
        var song: Song? = null

        var fileName: String? = null
        var fileSize = 0L

        try {
            val columns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
            context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))

                    val selection = "${AudioColumns.DISPLAY_NAME} = ? AND ${AudioColumns.SIZE} = ?"
                    val selectionArgs = arrayOf(fileName, fileSize.toString())

                    val cursor = makeSongCursor(selection, selectionArgs, ignoreBlacklist = true)
                    if (cursor != null && cursor.count > 0) {
                        song = resolveSongFromCursor(cursor, resolvedFromFile = true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve file info from FileProvider (uri: $uri)", e)
        }

        val uriPath = uri.path
        if (song == null && fileName != null && fileSize > 0 && uriPath != null) {
            val dataMimeType = context.contentResolver.getType(uri)
            if (ClipDescription.compareMimeTypes(dataMimeType, "audio/*")) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)

                    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        .orEmpty().ifEmpty { fileName }
                    val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        .orEmpty().ifEmpty { Artist.UNKNOWN }
                    val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        .orEmpty().ifEmpty { Album.UNKNOWN_ALBUM_DISPLAY_NAME }
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrDefault(0) ?: 0L

                    val id = (nonIndexedFiles.size + 1).toLong()
                    song = UnindexedSong(uri, id, uriPath, title, fileSize, duration, album, artist)
                        .also { nonIndexedFiles[uriPath] = it }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to retrieve file metadata (uri: $uri)", e)
                }
            }
        }

        return song
    }

    private fun getSongFromCursorImpl(cursor: Cursor, resolvedFromFile: Boolean = false): Song {
        val id = cursor.getLong(0)
        val data = cursor.getString(cursor.getColumnIndexOrThrow(AudioColumns.DATA))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(AudioColumns.TITLE))
        val trackNumber = cursor.getInt(cursor.getColumnIndexOrThrow(AudioColumns.TRACK))
        val year = cursor.getInt(cursor.getColumnIndexOrThrow(AudioColumns.YEAR))
        val size = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.SIZE))
        val duration = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.DURATION))
        val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.DATE_ADDED))
        val dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.DATE_MODIFIED))
        val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.ALBUM_ID))
        val albumName = cursor.getStringSafe(AudioColumns.ALBUM) ?: Album.UNKNOWN_ALBUM_DISPLAY_NAME
        val artistId = cursor.getLong(cursor.getColumnIndexOrThrow(AudioColumns.ARTIST_ID))
        val artistName = cursor.getStringSafe(AudioColumns.ARTIST) ?: ""
        val albumArtistName = cursor.getStringSafe(AudioColumns.ALBUM_ARTIST)
        val genreName = cursor.getStringSafe(AudioColumns.GENRE)
        val volumeName = cursor.getStringSafe(AudioColumns.VOLUME_NAME)
        return Song(
            id = id,
            data = data,
            title = title,
            trackNumber = trackNumber,
            year = year,
            size = size,
            duration = duration,
            dateAdded = dateAdded,
            rawDateModified = dateModified,
            albumId = albumId,
            albumName = albumName,
            artistId = artistId,
            artistName = artistName,
            albumArtistName = albumArtistName,
            genreName = genreName,
            volumeName = volumeName,
            resolvedFromFile = resolvedFromFile
        )
    }

    companion object {
        private val TAG = RealSongRepository::class.java.simpleName

        const val BASE_SELECTION = "${AudioColumns.TITLE} != '' AND ${AudioColumns.IS_MUSIC} = 1"
        const val SEARCH_SELECTION = "${AudioColumns.TITLE} LIKE ? OR ${AudioColumns.ARTIST} LIKE ? OR ${AudioColumns.ALBUM} LIKE ?"

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
            if (hasQ()) {
                baseProjection += AudioColumns.VOLUME_NAME
            }
            if (idColumn != AudioColumns._ID) {
                return baseProjection.copyOf().apply { set(0, idColumn) }
            }
            return baseProjection
        }

        fun generateSearchPattern(term: String, selection: String = SEARCH_SELECTION) =
            selection to Array(selection.count { it == '?' }) { "%$term%" }
    }
}