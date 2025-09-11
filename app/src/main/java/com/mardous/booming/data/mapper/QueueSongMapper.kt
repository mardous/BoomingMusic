package com.mardous.booming.data.mapper

import com.mardous.booming.data.model.QueueSong
import com.mardous.booming.data.model.Song

fun Song.toQueueSong(upcoming: Boolean = false) = QueueSong(this, upcoming)

fun List<Song>.toQueueSongs(upcoming: Boolean = false) = map { QueueSong(it, upcoming) }