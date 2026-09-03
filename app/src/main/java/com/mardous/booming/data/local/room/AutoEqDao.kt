/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoEqDao {
    @Query("SELECT * FROM AutoEqEntity")
    fun getAll(): Flow<List<AutoEqEntity>>

    @Query("""
        SELECT * FROM AutoEqEntity 
        WHERE model_name LIKE '%' || :query || '%' 
        OR label LIKE '%' || :query || '%' 
        OR source LIKE '%' || :query || '%'
        ORDER BY 
            CASE 
                WHEN label = :query THEN 0
                WHEN label LIKE :query || '%' THEN 1
                ELSE 2
            END, label ASC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 100): List<AutoEqEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AutoEqEntity>)

    @Query("DELETE FROM AutoEqEntity")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM AutoEqEntity")
    suspend fun count(): Int
}
