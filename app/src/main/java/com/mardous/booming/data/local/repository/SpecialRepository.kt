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

import android.provider.MediaStore.Audio.AudioColumns
import com.mardous.booming.core.model.filesystem.FileSystemQuery
import com.mardous.booming.data.model.Folder
import com.mardous.booming.data.model.ReleaseYear
import com.mardous.booming.data.model.Song
import com.mardous.booming.util.StorageUtil
import com.mardous.booming.util.sort.SortOrder
import com.mardous.booming.util.sort.sortedFolders
import com.mardous.booming.util.sort.sortedSongs
import com.mardous.booming.util.sort.sortedYears

interface SpecialRepository {
    suspend fun releaseYears(): List<ReleaseYear>
    suspend fun releaseYear(year: Int): ReleaseYear
    suspend fun songsByYear(year: Int, query: String): List<Song>
    suspend fun musicFolders(): FileSystemQuery
    suspend fun folderByPath(path: String): Folder
    suspend fun songsByFolder(path: String, includeSubfolders: Boolean): List<Song>
    suspend fun songsByFolder(path: String, query: String): List<Song>
    suspend fun musicFilesInPath(path: String, recursiveSubfolders: Boolean = true): FileSystemQuery
}

class RealSpecialRepository(private val songRepository: RealSongRepository) : SpecialRepository {

    override suspend fun releaseYears(): List<ReleaseYear> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor("${AudioColumns.YEAR} > 0", null)
        )
        val grouped = songs.groupBy { it.year }
        return grouped.map { ReleaseYear(it.key, it.value) }.sortedYears(SortOrder.yearSortOrder)
    }

    override suspend fun releaseYear(year: Int): ReleaseYear {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                selection = "${AudioColumns.YEAR}=?",
                selectionValues = arrayOf(year.toString())
            )
        )
        return ReleaseYear(year, songs.sortedSongs(SortOrder.yearSongSortOrder))
    }

    override suspend fun songsByYear(year: Int, query: String): List<Song> {
        return songRepository.songs(
            songRepository.makeSongCursor(
                selection = "${AudioColumns.YEAR}=? AND ${AudioColumns.TITLE} LIKE ?",
                selectionValues = arrayOf(year.toString(), "%$query%")
            )
        )
    }

    override suspend fun musicFolders(): FileSystemQuery {
        val allSongs = songRepository.songs()
        val songsByFolder = allSongs.groupBy { song ->
            song.folderPath()
        }.filter {
            it.key.isNotEmpty()
        }

        val folders = songsByFolder.map { (folderPath, songs) -> Folder(folderPath, songs) }
            .sortedFolders(SortOrder.folderSortOrder)

        return FileSystemQuery.createFlatView(folders)
    }

    override suspend fun folderByPath(path: String): Folder {
        val songs = songRepository.songs().filter { song ->
            path == song.folderPath()
        }
        return Folder(path, songs.sortedSongs(SortOrder.folderSongSortOrder))
    }

    override suspend fun songsByFolder(path: String, includeSubfolders: Boolean): List<Song> {
        if (includeSubfolders) {
            val dirPath = path.takeIf { it.endsWith("/") } ?: "$path/"
            val cursor = songRepository.makeSongCursor(
                selection = "${AudioColumns.DATA} LIKE ?",
                selectionValues = arrayOf("$dirPath%")
            )
            return songRepository.songs(cursor)
        }
        return songRepository.songs().filter { song -> path == song.folderPath() }
    }

    override suspend fun songsByFolder(path: String, query: String): List<Song> {
        val cursor = songRepository.makeSongCursor(
            selection = "${AudioColumns.TITLE} LIKE ?",
            selectionValues = arrayOf("%$query%")
        )
        return songRepository.songs(cursor).filter { song ->
            path == song.folderPath()
        }
    }

    override suspend fun musicFilesInPath(path: String, recursiveSubfolders: Boolean): FileSystemQuery {
        if (!FileSystemQuery.isNavigablePath(path)) {
            return FileSystemQuery(path, null, StorageUtil.storageVolumes, true)
        }
        val cursor = songRepository.makeSongCursor(
            "lower(${AudioColumns.DATA}) LIKE ?",
            arrayOf("${path.lowercase()}%")
        )
        val allSongs = songRepository.songs(cursor)
        val parentPath = path.substringBeforeLast("/")
        if (allSongs.isEmpty()) {
            return FileSystemQuery(path, parentPath, emptyList())
        }
        val subFoldersMap = allSongs
            .mapNotNull { song ->
                val folderPath = song.folderPath()
                if (folderPath != path && folderPath.startsWith("$path/")) {
                    val relative = folderPath.removePrefix("$path/")
                    val firstSegment = relative.substringBefore("/")
                    if (firstSegment.isNotEmpty()) {
                        val normalized = "$path/${firstSegment.lowercase()}"
                        normalized to "$path/$firstSegment"
                    } else null
                } else null
            }
            .toMap()

        val subfolders = subFoldersMap.values.map { subPath ->
            val innerMusicFiles = if (recursiveSubfolders) {
                musicFilesInPath(subPath, recursiveSubfolders = false).children
            } else emptyList()

            Folder(subPath, innerMusicFiles)
        }

        val songsInThisFolder = allSongs.filter { it.folderPath().equals(path, ignoreCase = true) }

        val children = buildList {
            addAll(subfolders)
            addAll(songsInThisFolder)
        }.sortedWith(compareBy({ it is Song }, { it.fileName.lowercase() }))

        return FileSystemQuery(path, parentPath, children)
    }

    private fun Song.folderPath() = data.substringBeforeLast("/", missingDelimiterValue = "")
}