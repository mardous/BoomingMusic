package com.mardous.booming.data.remote.lyrics.model

import kotlinx.serialization.Serializable

@Serializable
class AppleLyricsResponse(val type: String, val content: List<AppleLyrics>?) {
    @Serializable
    class AppleLyrics(
        val text: List<AppleLyricsLine>,
        val oppositeTurn: Boolean,
        val timestamp: Int,
        val endtime: Int
    ) {
        @Serializable
        class AppleLyricsLine(
            val text: String,
            val part: Boolean,
            val timestamp: Int?,
            val endtime: Int?
        )
    }
}