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
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PlayCountDao {

    @Upsert
    fun upsertSongInPlayCount(playCountEntity: PlayCountEntity)

    @Delete
    fun deleteSongInPlayCount(playCountEntity: PlayCountEntity)

    @Query("SELECT * FROM PlayCountEntity WHERE id =:songId LIMIT 1")
    fun findSongExistInPlayCount(songId: Long): PlayCountEntity?

    @Query("SELECT * FROM PlayCountEntity WHERE play_count > 0 ORDER BY play_count DESC")
    fun playCountSongs(): List<PlayCountEntity>

    @Query("SELECT * FROM PlayCountEntity WHERE skip_count > 0 ORDER BY skip_count DESC")
    fun skipCountSongs(): List<PlayCountEntity>

    @Query("DELETE FROM PlayCountEntity")
    fun clearPlayCount()
}
