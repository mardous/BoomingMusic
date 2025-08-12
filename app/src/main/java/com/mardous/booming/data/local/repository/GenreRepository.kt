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

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore.Audio.Genres
import com.mardous.booming.extensions.utilities.getLongSafe
import com.mardous.booming.extensions.utilities.getStringSafe
import com.mardous.booming.extensions.utilities.mapIfValid
import com.mardous.booming.extensions.utilities.takeOrDefault
import com.mardous.booming.data.model.Genre
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.local.repository.GenreRepository
import com.mardous.booming.util.sort.SortOrder
import com.mardous.booming.util.sort.sortedGenres
import com.mardous.booming.util.sort.sortedSongs

interface GenreRepository {
    suspend fun genres(query: String): List<Genre>
    suspend fun genres(): List<Genre>
    suspend fun genre(song: Song): Genre
    suspend fun songs(genreId: Long): List<Song>
    suspend fun songs(genreId: Long, query: String): List<Song>
    fun song(genreId: Long): Song
}

@SuppressLint("InlinedApi")
class RealGenreRepository(
    private val contentResolver: ContentResolver,
    private val songRepository: RealSongRepository
) : GenreRepository {

    override suspend fun genres(query: String): List<Genre> {
        return getGenresFromCursor(
            makeGenreCursor(selection = "${Genres.NAME} LIKE ?", selectionValues = arrayOf("%$query%"))
        )
    }

    override suspend fun genres(): List<Genre> {
        return getGenresFromCursor(makeGenreCursor()).sortedGenres(SortOrder.genreSortOrder)
    }

    override suspend fun genre(song: Song): Genre {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
            .appendEncodedPath(song.id.toString())
            .appendEncodedPath("genres")
            .build()

        return makeGenreCursor(uri = uri).use {
            it.takeOrDefault(Genre.EmptyGenre) { getGenreFromCursor(this) }
        }
    }

    override suspend fun songs(genreId: Long): List<Song> {
        // The genres table only stores songs that have a genre specified,
        // so we need to get songs without a genre a different way.
        return if (genreId == -1L) {
            getSongsWithNoGenre()
        } else songRepository.songs(makeGenreSongCursor(genreId)).sortedSongs(SortOrder.genreSongSortOrder)
    }

    override suspend fun songs(genreId: Long, query: String): List<Song> {
        // The genres table only stores songs that have a genre specified,
        // so we need to get songs without a genre a different way.
        return if (genreId == -1L) {
            emptyList()
        } else songRepository.songs(
            makeGenreSongCursor(
                genreId,
                "${RealSongRepository.BASE_SELECTION} AND ${AudioColumns.TITLE} LIKE ?",
                arrayOf("%$query%")
            )
        )
    }

    override fun song(genreId: Long): Song {
        return songRepository.song(makeGenreSongCursor(genreId))
    }

    private fun getSongCount(genreId: Long): Int {
        contentResolver.query(
            Genres.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, genreId),
            null,
            null,
            null,
            null
        ).use {
            return it?.count ?: 0
        }
    }

    private fun getGenreFromCursor(cursor: Cursor): Genre {
        val id = cursor.getLongSafe(Genres._ID)
        val name = cursor.getStringSafe(Genres.NAME)
        val songCount = getSongCount(id)
        return Genre(id, name ?: "", songCount)
    }

    private fun getSongsWithNoGenre(): List<Song> {
        val selection = "${BaseColumns._ID} NOT IN (SELECT ${Genres.Members.AUDIO_ID} FROM audio_genres_map)"
        return songRepository.songs(songRepository.makeSongCursor(selection, null))
            .sortedSongs(SortOrder.genreSongSortOrder)
    }

    private fun getGenresFromCursor(cursor: Cursor?): List<Genre> {
        return cursor.use {
            it.mapIfValid { getGenreFromCursor(this) }
                .filter { genre -> genre.id > -1 && genre.songCount > 0 }
        }
    }

    private fun makeGenreCursor(
        uri: Uri = Genres.EXTERNAL_CONTENT_URI,
        selection: String? = null,
        selectionValues: Array<String>? = null
    ): Cursor? {
        return try {
            contentResolver.query(
                uri,
                arrayOf(Genres._ID, Genres.NAME),
                selection,
                selectionValues,
                Genres.DEFAULT_SORT_ORDER
            )
        } catch (e: SecurityException) {
            return null
        }
    }

    private fun makeGenreSongCursor(
        genreId: Long,
        selection: String = RealSongRepository.BASE_SELECTION,
        selectionValues: Array<String>? = null
    ): Cursor? {
        return try {
            contentResolver.query(
                Genres.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, genreId),
                RealSongRepository.getBaseProjection(),
                selection,
                selectionValues,
                Genres.Members.DEFAULT_SORT_ORDER
            )
        } catch (e: SecurityException) {
            return null
        }
    }
}
