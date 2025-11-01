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

package com.mardous.booming.core.model.filesystem

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * Simplified wrapper for [android.os.storage.StorageVolume].
 */
@Parcelize
class StorageDevice(
    override val filePath: String,
    override val fileName: String,
    val iconRes: Int
) : Parcelable, FileSystemItem {

    val file: File
        get() = File(filePath)

    @IgnoredOnParcel
    override val fileDateAdded: Long = -1

    @IgnoredOnParcel
    override val fileDateModified: Long = -1

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StorageDevice) return false
        return filePath == other.filePath && fileName == other.fileName
    }

    override fun hashCode(): Int {
        var result = filePath.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }

    override fun toString(): String {
        return "StorageDevice{path='$filePath', name='$fileName'}"
    }
}