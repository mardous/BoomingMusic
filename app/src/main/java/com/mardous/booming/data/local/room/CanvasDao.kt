package com.mardous.booming.data.local.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CanvasDao {
    @Upsert
    fun insertCanvas(entity: CanvasEntity)

    @Delete
    fun deleteCanvas(entity: CanvasEntity)

    @Query("SELECT * FROM CanvasEntity WHERE id = :songId")
    fun getCanvas(songId: Long): CanvasEntity?

    @Query("DELETE FROM CanvasEntity")
    fun deleteCanvases()
}