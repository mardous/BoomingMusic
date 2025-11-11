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
package com.mardous.booming.coil.util

import java.io.File
import java.io.IOException
import java.io.InputStream

object AudioCoverUtils {

    // TODO: find a cleaner way to do this
    private val FALLBACKS = arrayOf(
        "cover.jpg",
        "album.jpg",
        "folder.jpg",
        "cover.png",
        "album.png",
        "folder.png",
        "cover.jpeg",
        "album.jpeg",
        "folder.jpeg"
    )

    @Throws(IOException::class)
    fun fallback(path: String, useFolderImage: Boolean): InputStream? {
        if (useFolderImage) {
            val parent = File(path).getParentFile()
            for (fallback in FALLBACKS) {
                val cover = File(parent, fallback)
                if (cover.isFile) {
                    return cover.inputStream()
                }
            }
        }
        return null
    }
}
