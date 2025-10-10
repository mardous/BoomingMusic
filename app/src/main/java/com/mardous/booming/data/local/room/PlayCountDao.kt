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
import androidx.room.Transaction
import androidx.room.Upsert
import com.mardous.booming.data.mapper.toPlayCount
import com.mardous.booming.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayCountDao {

    @Upsert
    suspend fun upsertSongInPlayCount(playCountEntity: PlayCountEntity)

    @Query("DELETE FROM PlayCountEntity WHERE id = :songId")
    suspend fun deleteSongInPlayCount(songId: Long)

    @Query("DELETE FROM PlayCountEntity WHERE id IN(:songIds)")
    suspend fun deleteSongsInPlayCount(songIds: List<Long>)

    @Query("SELECT * FROM PlayCountEntity WHERE id IN (:songIds)")
    suspend fun findSongsExistInPlayCount(songIds: List<Long>): List<PlayCountEntity>

    @Query("SELECT * FROM PlayCountEntity WHERE id =:songId LIMIT 1")
    suspend fun findSongExistInPlayCount(songId: Long): PlayCountEntity?

    @Transaction
    suspend fun insertOrIncrementPlayCount(song: Song, timePlayed: Long) {
        val playCountEntity = findSongExistInPlayCount(song.id)
            ?: song.toPlayCount(timePlayed = timePlayed)

        upsertSongInPlayCount(
            playCountEntity.copy(
                playCount = playCountEntity.playCount + 1,
                timePlayed = timePlayed
            )
        )
    }

    @Transaction
    suspend fun insertOrIncrementSkipCount(song: Song) {
        val playCountEntity = findSongExistInPlayCount(song.id)
            ?: song.toPlayCount()

        upsertSongInPlayCount(playCountEntity.copy(skipCount = playCountEntity.skipCount + 1))
    }

    @Query("SELECT * FROM PlayCountEntity WHERE play_count > 0 ORDER BY play_count DESC")
    suspend fun playCountSongs(): List<PlayCountEntity>

    @Query("SELECT * FROM PlayCountEntity WHERE play_count > 0 ORDER BY play_count DESC")
    fun playCountSongsFlow(): Flow<List<PlayCountEntity>>

    @Query("SELECT * FROM PlayCountEntity WHERE skip_count > 0 ORDER BY skip_count DESC")
    suspend fun skipCountSongs(): List<PlayCountEntity>

    @Query("DELETE FROM PlayCountEntity")
    suspend fun clearPlayCount()
}
