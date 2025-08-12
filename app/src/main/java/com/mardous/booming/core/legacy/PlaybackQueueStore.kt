/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mardous.booming.core.legacy

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.MediaStore.Audio.AudioColumns
import androidx.core.database.sqlite.transaction
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.local.repository.SongRepository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class PlaybackQueueStore(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createTable(db, PLAYING_QUEUE_TABLE_NAME)
        createTable(db, ORIGINAL_PLAYING_QUEUE_TABLE_NAME)
    }

    private fun createTable(db: SQLiteDatabase, tableName: String) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $tableName (
                ${AudioColumns._ID} LONG NOT NULL,
                ${AudioColumns.DATA} TEXT NOT NULL,
                ${AudioColumns.TITLE} TEXT NOT NULL,
                ${AudioColumns.TRACK} INTEGER NOT NULL,
                ${AudioColumns.YEAR} INTEGER NOT NULL,
                ${AudioColumns.SIZE} LONG NOT NULL,
                ${AudioColumns.DURATION} LONG NOT NULL,
                ${AudioColumns.DATE_ADDED} LONG NOT NULL,
                ${AudioColumns.DATE_MODIFIED} LONG NOT NULL,
                ${AudioColumns.ALBUM_ID} LONG NOT NULL,
                ${AudioColumns.ALBUM} TEXT NOT NULL,
                ${AudioColumns.ARTIST_ID} LONG NOT NULL,
                ${AudioColumns.ARTIST} TEXT NOT NULL,
                ${AudioColumns.ALBUM_ARTIST} TEXT
            );
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $PLAYING_QUEUE_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $ORIGINAL_PLAYING_QUEUE_TABLE_NAME")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $PLAYING_QUEUE_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $ORIGINAL_PLAYING_QUEUE_TABLE_NAME")
        onCreate(db)
    }

    suspend fun saveQueues(
        playingQueue: List<Song>,
        originalPlayingQueue: List<Song>
    ) = withContext(IO) {
        saveQueue(PLAYING_QUEUE_TABLE_NAME, playingQueue)
        saveQueue(ORIGINAL_PLAYING_QUEUE_TABLE_NAME, originalPlayingQueue)
    }

    private fun saveQueue(tableName: String, queue: List<Song>) {
        val database = writableDatabase
        database.transaction {
            delete(tableName, null, null)
        }

        var position = 0
        val chunkSize = 250
        while (position < queue.size) {
            val end = minOf(position + chunkSize, queue.size)
            database.transaction {
                for (i in position until end) {
                    val song = queue[i]
                    val values = ContentValues(14).apply {
                        put(AudioColumns._ID, song.id)
                        put(AudioColumns.DATA, song.data)
                        put(AudioColumns.TITLE, song.title)
                        put(AudioColumns.TRACK, song.trackNumber)
                        put(AudioColumns.YEAR, song.year)
                        put(AudioColumns.SIZE, song.size)
                        put(AudioColumns.DURATION, song.duration)
                        put(AudioColumns.DATE_ADDED, song.dateAdded)
                        put(AudioColumns.DATE_MODIFIED, song.dateModified)
                        put(AudioColumns.ALBUM_ID, song.albumId)
                        put(AudioColumns.ALBUM, song.albumName)
                        put(AudioColumns.ARTIST_ID, song.artistId)
                        put(AudioColumns.ARTIST, song.artistName)
                        put(AudioColumns.ALBUM_ARTIST, song.albumArtistName)
                    }
                    insert(tableName, null, values)
                }
            }
            position += chunkSize
        }
    }

    private fun getSavedQueue(tableName: String, songRepository: SongRepository): List<Song> {
        val cursor = readableDatabase.query(tableName, null, null, null, null, null, null)
        return cursor.use { songRepository.songs(it) }
    }

    fun getSavedPlayingQueue(songRepository: SongRepository): List<Song> =
        getSavedQueue(PLAYING_QUEUE_TABLE_NAME, songRepository)

    fun getSavedOriginalPlayingQueue(songRepository: SongRepository): List<Song> =
        getSavedQueue(ORIGINAL_PLAYING_QUEUE_TABLE_NAME, songRepository)

    companion object {
        private const val DATABASE_NAME = "music_playback_state.db"
        private const val VERSION = 2

        const val PLAYING_QUEUE_TABLE_NAME = "playing_queue"
        const val ORIGINAL_PLAYING_QUEUE_TABLE_NAME = "original_playing_queue"
    }
}
