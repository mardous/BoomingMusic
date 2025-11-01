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

import androidx.room.*

@Dao
interface InclExclDao {
    companion object {
        const val WHITELIST = 0
        const val BLACKLIST = 1
    }

    @Query("SELECT * FROM InclExclEntity WHERE type = :type")
    suspend fun getPaths(type: Int): List<InclExclEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPath(blackListStoreEntity: InclExclEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaths(blackListStoreEntities: List<InclExclEntity>)

    @Delete
    suspend fun deletePath(blackListStoreEntity: InclExclEntity)

    @Query("DELETE FROM InclExclEntity WHERE type = :type")
    suspend fun clearPaths(type: Int)

    @Query("SELECT * FROM InclExclEntity WHERE type = 1")
    fun blackListPaths(): List<InclExclEntity>

    @Query("SELECT * FROM InclExclEntity WHERE type = 0")
    fun whitelistPaths(): List<InclExclEntity>
}
