/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package com.mardous.booming.data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    companion object {
        private const val HISTORY_LIMIT = 100
    }

    @Upsert
    suspend fun upsertSongInHistory(historyEntity: HistoryEntity)

    @Query("DELETE FROM HistoryEntity WHERE id = :songId")
    fun deleteSongInHistory(songId: Long)

    @Query("DELETE FROM HistoryEntity WHERE id IN (:songIds)")
    fun deleteSongsInHistory(songIds: List<Long>)

    @Query("""
    SELECT * FROM HistoryEntity
    WHERE (:cutoff = 0 OR time_played > :cutoff)
    ORDER BY time_played DESC
    LIMIT $HISTORY_LIMIT""")
    fun historySongs(cutoff: Long = 0): List<HistoryEntity>

    @Query("""
    SELECT * FROM HistoryEntity
    WHERE (:cutoff = 0 OR time_played > :cutoff)
    ORDER BY time_played DESC
    LIMIT $HISTORY_LIMIT""")
    fun historySongsFlow(cutoff: Long = 0): Flow<List<HistoryEntity>>

    @Query("""
    SELECT id FROM HistoryEntity
    WHERE (:cutoff = 0 OR time_played > :cutoff)
    ORDER BY time_played DESC
    LIMIT :limit""")
    suspend fun playedSongIds(cutoff: Long = 0, limit: Int = HISTORY_LIMIT): List<Long>

    @Query("""
    SELECT id FROM HistoryEntity
    WHERE (:cutoff = 0 OR time_played < :cutoff)
    ORDER BY time_played ASC
    LIMIT :limit""")
    suspend fun notPlayedSongIds(cutoff: Long = 0, limit: Int = HISTORY_LIMIT): List<Long>

    @Query("DELETE FROM HistoryEntity")
    suspend fun clearHistory()
}
