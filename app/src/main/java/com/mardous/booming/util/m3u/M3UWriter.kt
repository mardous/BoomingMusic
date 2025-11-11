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

package com.mardous.booming.util.m3u

import android.content.Context
import android.provider.MediaStore
import com.mardous.booming.R
import com.mardous.booming.data.local.MediaStoreWriter
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.local.room.SongEntity
import com.mardous.booming.data.mapper.toSongs
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.hasQ
import com.mardous.booming.extensions.showToast
import com.mardous.booming.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.BufferedWriter
import java.io.File
import java.io.IOException

object M3UWriter : KoinComponent {

    suspend fun export(context: Context, playlist: PlaylistWithSongs) {
        val success = withContext(Dispatchers.IO) {
            runCatching { write(context, playlist) }
        }
        val message = if (success.getOrDefault(false)) {
            context.getString(R.string.saved_playlist_x, playlist.playlistEntity.playlistName)
        } else {
            context.getString(R.string.failed_to_save_playlist)
        }
        context.showToast(message)
    }

    suspend fun export(context: Context, playlists: List<PlaylistWithSongs>) {
        var exported = 0
        var failed = 0

        withContext(Dispatchers.IO) {
            for (playlist in playlists) {
                val saved = runCatching { write(context, playlist) }
                if (saved.getOrDefault(false)) exported++ else failed++
            }
        }

        val message = if (failed == 0) {
            context.getString(R.string.saved_x_playlists, exported)
        } else {
            context.getString(R.string.saved_x_playlists_failed_to_save_x, exported, failed)
        }
        context.showToast(message)
    }

    @Suppress("DEPRECATION")
    @Throws(IOException::class)
    fun write(context: Context?, playlist: PlaylistWithSongs): Boolean {
        if (context != null) {
            val exportedPlaylistName =
                String.format("%s.%s", playlist.playlistEntity.playlistName, M3UConstants.EXTENSION)
            val playlistsUri = when {
                hasQ() -> MediaStore.Audio.Playlists.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else -> MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
            }
            val scopedStorageResult = get<MediaStoreWriter>().toMediaStore(
                playlistsUri,
                MediaStoreWriter.Request.forPlaylist(exportedPlaylistName)
            ) { stream ->
                stream.bufferedWriter().use {
                    writeImpl(it, playlist.songs)
                }
            }
            if (scopedStorageResult.resultCode == MediaStoreWriter.Result.Code.NO_SCOPED_STORAGE) {
                val directory = FileUtil.playlistsDirectory()
                if (directory != null) {
                    return writeToDirectory(directory, playlist).isFile
                }
                return false
            }
            return scopedStorageResult.resultCode == MediaStoreWriter.Result.Code.SUCCESS
        }
        return false
    }

    @Throws(IOException::class)
    fun writeToDirectory(dir: File, playlist: PlaylistWithSongs): File {
        val exportFile = File(dir, String.format("%s.%s", playlist.playlistEntity.playlistName, M3UConstants.EXTENSION))
        if (exportFile.createNewFile()) {
            exportFile.bufferedWriter().use {
                if (!writeImpl(it, playlist.songs)) {
                    exportFile.delete()
                }
            }
        }
        return exportFile
    }

    @Throws(IOException::class)
    private fun writeImpl(bw: BufferedWriter, songs: List<SongEntity>): Boolean {
        val songs: List<Song> = songs.sortedBy {
            it.songPrimaryKey
        }.toSongs()
        if (songs.isNotEmpty()) {
            bw.write(M3UConstants.HEADER)
            for (song in songs) {
                bw.newLine()
                bw.write("${M3UConstants.ENTRY}${song.duration}${M3UConstants.DURATION_SEPARATOR}${song.artistName} - ${song.title}")
                bw.newLine()
                bw.write(song.data)
            }
            return true
        }
        return false
    }
}