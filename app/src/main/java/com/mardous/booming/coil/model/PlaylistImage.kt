package com.mardous.booming.coil.model

import com.mardous.booming.data.local.room.PlaylistEntity
import com.mardous.booming.data.model.Song

class PlaylistImage(val playlistEntity: PlaylistEntity, val songs: List<Song>) {
    override fun toString(): String {
        return buildString {
            append("PlaylistImage{")
            append("playlistEntity=$playlistEntity,")
            append("songs=$songs")
            append("}")
        }
    }
}