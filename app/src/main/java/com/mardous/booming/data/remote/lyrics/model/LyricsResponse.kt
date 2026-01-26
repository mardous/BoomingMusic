package com.mardous.booming.data.remote.lyrics.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BetterLyricsResponse(
    val ttml: String
)

@Serializable
class SimpMusicLyricsResponse(
    val success: Boolean,
    val data: List<Data>
) {
    @Serializable
    class Data(
        val id: String,
        val songTitle: String,
        val artistName: String,
        @SerialName("durationSeconds")
        val durationInSeconds: Int,
        @SerialName("plainLyric")
        val plainLyrics: String?,
        val syncedLyrics: String?,
        @SerialName("richSyncLyrics")
        val richSyncedLyrics: String?
    )
}