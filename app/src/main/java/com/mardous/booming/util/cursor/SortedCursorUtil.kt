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

package com.mardous.booming.util.cursor

import android.database.Cursor
import android.provider.MediaStore.Audio.AudioColumns
import com.mardous.booming.data.local.repository.RealSongRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object SortedCursorUtil : KoinComponent {

    fun makeSortedCursor(cursor: Cursor?, idColumn: Int): SortedLongCursor? {
        if (cursor != null && cursor.moveToFirst()) {
            // create the list of ids to select against
            val selection = StringBuilder()
            selection.append(AudioColumns._ID)
            selection.append(" IN (")

            // this tracks the order of the ids
            val order = LongArray(cursor.count)
            var id = cursor.getLong(idColumn)
            selection.append(id)
            order[cursor.position] = id
            while (cursor.moveToNext()) {
                selection.append(",")
                id = cursor.getLong(idColumn)
                order[cursor.position] = id
                selection.append(id)
            }
            selection.append(")")

            // get a list of songs with the data given the selection statement
            val songRepository = get<RealSongRepository>()
            val songCursor = songRepository.makeSongCursor(selection.toString(), null)
            if (songCursor != null) {
                // now return the wrapped cursor to handle sorting given order
                return SortedLongCursor(
                    songCursor,
                    order,
                    AudioColumns._ID
                )
            }
        }
        return null
    }
}