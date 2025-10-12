package com.mardous.booming.core

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mardous.booming.data.local.room.*

@Database(
    entities = [
        PlaylistEntity::class,
        SongEntity::class,
        HistoryEntity::class,
        PlayCountEntity::class,
        QueueEntity::class,
        InclExclEntity::class,
        LyricsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BoomingDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playCountDao(): PlayCountDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun inclExclDao(): InclExclDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE PlaylistEntity ADD COLUMN custom_cover_uri TEXT")
                db.execSQL("ALTER TABLE PlaylistEntity ADD COLUMN description TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `QueueEntity` (`id` TEXT NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
    }
}