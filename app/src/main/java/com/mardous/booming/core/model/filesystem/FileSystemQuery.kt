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
import com.mardous.booming.core.sort.FileSortMode
import com.mardous.booming.util.StorageUtil
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent

class FileSystemQuery(
    val path: String?,
    val parentPath: String?,
    val children: List<FileSystemItem>,
    val isStorageRoot: Boolean = false
) : KoinComponent {

    val isFlatView: Boolean = path.isNullOrEmpty()

    val canGoUp: Boolean = !parentPath.isNullOrEmpty() && !isFlatView && !isStorageRoot

    fun getSortedChildren(sortMode: FileSortMode): List<FileSystemItem> {
        return with(sortMode) { children.sorted() }
    }

    fun getNavigableChildren(sortMode: FileSortMode): List<FileSystemItem> {
        if (isFlatView) {
            return getSortedChildren(sortMode)
        }
        return buildList {
            if (canGoUp) {
                add(GoUpFileSystemItem(fileName = "...", filePath = parentPath!!))
            }
            if (isStorageRoot) {
                addAll(children)
            } else {
                addAll(getSortedChildren(sortMode))
            }
        }
    }

    @Parcelize
    class GoUpFileSystemItem internal constructor(
        override val fileId: Long = GO_UP_ID,
        override val fileName: String,
        override val filePath: String
    ) : Parcelable, FileSystemItem {

        @IgnoredOnParcel
        override val fileDateAdded: Long = -1

        @IgnoredOnParcel
        override val fileDateModified: Long = -1

    }

    companion object {
        private const val GO_UP_ID = -2L

        fun createFlatView(children: List<FileSystemItem>): FileSystemQuery {
            return FileSystemQuery(null, null, children)
        }

        fun isNavigablePath(path: String): Boolean {
            return StorageUtil.storageVolumes.none { it.file.parent == path }
        }
    }
}