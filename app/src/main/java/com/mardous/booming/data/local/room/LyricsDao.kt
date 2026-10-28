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

    @Query("""
    INSERT INTO LyricsEntity (id, lyrics, provider, is_instrumental) 
    VALUES (:songId, :lyrics, :provider, :instrumental)
    ON CONFLICT(id) DO UPDATE SET 
        lyrics = excluded.lyrics, 
        provider = excluded.provider, 
        is_instrumental = excluded.is_instrumental
    """)
    suspend fun upsertDownloadedLyrics(songId: Long, lyrics: String?, provider: String?, instrumental: Boolean)

    @Query("DELETE FROM LyricsEntity")
    suspend fun removeLyrics()

    @Query("SELECT * FROM LyricsEntity")
    suspend fun getAllLyrics(): List<LyricsEntity>

    @Query("SELECT * FROM LyricsEntity WHERE id = :songId")
    suspend fun getLyrics(songId: Long): LyricsEntity?

    @Query("SELECT `offset` FROM LyricsEntity WHERE id = :songId")
    suspend fun getOffset(songId: Long): Long?

    @Query("""
    INSERT INTO LyricsEntity (id, `offset`) 
    VALUES (:songId, :offset)
    ON CONFLICT(id) DO UPDATE SET `offset` = excluded.offset
    """)
    suspend fun upsertOffset(songId: Long, offset: Long)
}