/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.model

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import com.mardous.booming.R
import com.mardous.booming.extensions.plurals
import com.mardous.booming.model.filesystem.FileSystemItem
import java.io.File

/**
 * @author Christians M. A. (mardous)
 */
class Folder(
    override val filePath: String,
    val musicFiles: List<FileSystemItem>
) : FileSystemItem, SongProvider {

    override val fileName: String
        get() = filePath.substringAfterLast("/")

    override val fileDateAdded: Long
        get() = musicFiles.minOf { it.fileDateAdded }

    override val fileDateModified: Long
        get() = musicFiles.maxOf { it.fileDateModified }

    override val songs: List<Song>
        get() = musicFiles.filterIsInstance<Song>()

    val file: File
        get() = File(filePath)

    val songCount: Int
        get() = musicFiles.count { it is Song }

    override fun getFileIcon(context: Context): Drawable? {
        return AppCompatResources.getDrawable(context, R.drawable.ic_folder_24dp)
    }

    override fun getFileDescription(context: Context): CharSequence {
        return context.plurals(R.plurals.x_items, musicFiles.size)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Folder) return false
        return filePath == other.filePath && musicFiles == other.musicFiles
    }

    override fun hashCode(): Int {
        return 31 * filePath.hashCode() + musicFiles.hashCode()
    }

    override fun toString(): String {
        return "Folder(filePath='$filePath', musicFiles=$musicFiles)"
    }

    companion object {
        val empty = Folder("", emptyList())
    }
}