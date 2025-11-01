package com.mardous.booming.ui.screen.info

import com.mardous.booming.data.local.room.PlayCountEntity

data class PlayInfoResult(
    val playCount: Int,
    val skipCount: Int,
    val lastPlayDate: Long,
    val mostPlayedTracks: List<PlayCountEntity>
)

data class SongInfoResult(
    val playCount: String? = null,
    val skipCount: String? = null,
    val lastPlayedDate: String? = null,
    val filePath: String? = null,
    val fileSize: String? = null,
    val trackLength: String? = null,
    val dateModified: String? = null,
    val audioHeader: String? = null,
    val title: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val albumYear: String? = null,
    val trackNumber: String? = null,
    val discNumber: String? = null,
    val composer: String? = null,
    val conductor: String? = null,
    val publisher: String? = null,
    val genre: String? = null,
    val replayGain: String? = null,
    val comment: String? = null
) {
    companion object {
        val Empty = SongInfoResult()
    }
}