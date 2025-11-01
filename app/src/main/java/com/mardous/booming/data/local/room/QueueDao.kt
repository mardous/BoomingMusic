package com.mardous.booming.data.local.room

import androidx.room.*

@Dao
interface QueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQueue(items: List<QueueEntity>)

    @Query("SELECT * FROM QueueEntity ORDER BY `order`")
    suspend fun savedItems(): List<QueueEntity>

    @Query("DELETE FROM QueueEntity WHERE id NOT IN (:currentIds)")
    suspend fun removeNotIn(currentIds: List<String>)

    @Transaction
    suspend fun replaceQueue(newQueue: List<QueueEntity>) {
        removeNotIn(newQueue.map { it.id })
        saveQueue(newQueue)
    }
}