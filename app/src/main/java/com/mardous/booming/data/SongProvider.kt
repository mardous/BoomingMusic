package com.mardous.booming.data

import com.mardous.booming.data.model.Song

interface SongProvider {
    val songs: List<Song>
}