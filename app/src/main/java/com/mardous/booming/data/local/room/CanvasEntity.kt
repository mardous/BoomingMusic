package com.mardous.booming.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class CanvasEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "canvas_url")
    val url: String,
    @ColumnInfo(name = "fetch_time")
    val fetchTimeMs: Long
)