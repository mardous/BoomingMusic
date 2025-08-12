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

package com.mardous.booming.util

import android.content.Context
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.core.net.toUri
import com.mardous.booming.extensions.files.deleteUsingSAF
import com.mardous.booming.extensions.hasQ
import com.mardous.booming.extensions.onUI
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.local.repository.Repository
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

typealias ProgressCallback = (song: Song, progress: Int, total: Int) -> Unit
typealias CompletionCallback = (deleted: Int) -> Unit

object MusicUtil : KoinComponent {

    suspend fun deleteTracks(
        context: Context,
        songs: List<Song>,
        onProgress: ProgressCallback,
        onCompleted: CompletionCallback? = null
    ) {
        val repository = get<Repository>()
        val projection = arrayOf(
            BaseColumns._ID, MediaStore.MediaColumns.DATA
        )

        // Split the query into multiple batches, and merge the resulting cursors
        var batchEnd = 0
        val batchSize =
            1000000 / 10 // 10^6 being the SQLite limit on the query length in bytes, 10 being the max number of digits in an int, used to store the track ID

        var deleted = 0
        val songCount = songs.size

        while (batchEnd < songCount) {
            val selection = StringBuilder()
            selection.append(BaseColumns._ID + " IN (")

            var i = 0
            while (i < batchSize - 1 && batchEnd < songCount - 1) {
                selection.append(songs[batchEnd].id)
                selection.append(",")
                i++
                batchEnd++
            }

            // The last element of a batch
            selection.append(songs[batchEnd].id)
            batchEnd++
            selection.append(")")

            try {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection.toString(), null, null
                ).use { cursor ->
                    // TODO: At this point, there is no guarantee that the size of the cursor is the same as the size of the selection string.
                    if (cursor != null) {
                        // Step 1: Remove selected tracks from the current playlist, as well
                        // as from the album art cache and the Music Database
                        cursor.moveToFirst()
                        while (!cursor.isAfterLast) {
                            val position = cursor.position + 1
                            val count = cursor.count

                            val id = cursor.getLong(0)
                            val deletedSong = repository.deleteSong(id)

                            context.onUI {
                                onProgress(deletedSong, position.coerceAtMost(count), count)
                            }

                            cursor.moveToNext()
                        }

                        // Step 2: Remove selected tracks from the database
                        deleted += context.contentResolver.delete(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            selection.toString(), null
                        )

                        // Step 3: Remove files from card - Android Q takes care of this if the element is remove via MediaStore
                        if (!hasQ()) {
                            cursor.moveToFirst()
                            while (!cursor.isAfterLast) {
                                context.deleteUsingSAF(cursor.getString(1))
                                cursor.moveToNext()
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        context.onUI {
            context.contentResolver.notifyChange("content://media".toUri(), null)
            onCompleted?.invoke(deleted)
        }
    }
}