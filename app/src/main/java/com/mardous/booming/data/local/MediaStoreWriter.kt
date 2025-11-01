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

package com.mardous.booming.data.local

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.mardous.booming.extensions.hasQ
import com.mardous.booming.util.FileUtil.PLAYLISTS_DIRECTORY_NAME
import java.io.File
import java.io.IOException
import java.io.OutputStream

/**
 * @author Christians M. A. (mardous)
 */
class MediaStoreWriter(private val context: Context, private val contentResolver: ContentResolver) {

    class Request(
        val displayName: String,
        val relativePath: String,
        val mimeType: String,
        val valuesInterceptor: ((ContentValues) -> Unit)? = null
    ) {

        companion object {
            fun forImage(displayName: String, subFolder: String? = null, imageMimeType: String): Request {
                val relativePath = if (subFolder.isNullOrEmpty())
                    Environment.DIRECTORY_PICTURES else "${Environment.DIRECTORY_PICTURES}/$subFolder"

                return Request(displayName, relativePath, imageMimeType)
            }

            fun forPlaylist(displayName: String): Request {
                return Request(
                    displayName,
                    String.format("%s/%s", Environment.DIRECTORY_MUSIC, PLAYLISTS_DIRECTORY_NAME),
                    "vnd.android.cursor.dir/playlist"
                )
            }
        }
    }

    class Result(val resultCode: Int, val uri: Uri? = null) {
        class Code {
            companion object {
                const val SUCCESS = 0
                const val NO_SCOPED_STORAGE = 1
                const val ERROR = 2
            }
        }
    }

    fun toMediaStore(
        contentUri: Uri,
        request: Request,
        streamConsumer: (OutputStream) -> Boolean
    ): Result {
        if (!hasQ()) {
            return Result(Result.Code.NO_SCOPED_STORAGE)
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, request.displayName)
            put(MediaStore.MediaColumns.RELATIVE_PATH, request.relativePath)
            put(MediaStore.MediaColumns.MIME_TYPE, request.mimeType)
        }.also { contentValues ->
            request.valuesInterceptor?.invoke(contentValues)
        }

        var uri: Uri? = null

        try {
            uri = contentResolver.insert(contentUri, values)
            if (uri != null) {
                return toContentResolver(contentUri, uri, streamConsumer)
            }
        } catch (e: IOException) {
            if (uri != null) contentResolver.delete(uri, null, null)
        } catch (e: SecurityException) {
            if (uri != null) contentResolver.delete(uri, null, null)
        }
        return Result(Result.Code.ERROR)
    }

    fun toContentResolver(
        contentUri: Uri?,
        dest: Uri,
        streamConsumer: (OutputStream) -> Boolean
    ): Result {
        try {
            contentResolver.openOutputStream(dest).use { outputStream ->
                if (outputStream != null && streamConsumer(outputStream)) {
                    if (contentUri != null) {
                        contentResolver.notifyChange(contentUri, null)
                    }
                    return Result(Result.Code.SUCCESS, dest)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return Result(Result.Code.ERROR)
    }

    fun toFile(directory: File? = null, fileName: String, streamConsumer: (OutputStream) -> Boolean): File? {
        val result = runCatching {
            val file = File(directory ?: context.filesDir, fileName)
            if (file.createNewFile()) {
                file.outputStream().use {
                    if (streamConsumer(it))
                        return file
                    else null
                }
            } else null
        }
        return result.getOrNull()
    }
}