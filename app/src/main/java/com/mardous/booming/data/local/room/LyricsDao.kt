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

package com.mardous.booming.data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LyricsDao {
    @Upsert
    suspend fun insertLyrics(lyrics: List<LyricsEntity>)

    @Upsert
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("DELETE FROM LyricsEntity WHERE id = :songId")
    suspend fun removeLyrics(songId: Long)

    @Query("DELETE FROM LyricsEntity")
    suspend fun removeLyrics()

    @Query("SELECT * FROM LyricsEntity")
    suspend fun getAllLyrics(): List<LyricsEntity>

    @Query("SELECT * FROM LyricsEntity WHERE id = :songId")
    suspend fun getLyrics(songId: Long): LyricsEntity?
}