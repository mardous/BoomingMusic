package com.mardous.booming.data.remote.lyrics.model

import kotlinx.serialization.Serializable

@Serializable
class LyricsSearchResponse(
    val source: String,
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val url: String
)

@Serializable
class AppleLyricsResponse(val syllable: Boolean, val lines: List<AppleMusicLine>?) {
    @Serializable
    class AppleMusicLine(
        val words: List<AppleMusicWord>,
        val oppositeTurn: Boolean,
        val startTime: Int,
        val endTime: Int
    ) {
        @Serializable
        class AppleMusicWord(
            val text: String,
            val breaks: Boolean,
            val startTime: Int?,
            val endTime: Int?
        )
    }
}

@Serializable
class SpotifyLyricsResponse(val syllable: Boolean, val lines: List<SpotifyLine>?) {
    @Serializable
    class SpotifyLine(
        val words: String,
        val oppositeTurn: Boolean,
        val startTime: Int,
        val endTime: Int
    )
}

internal fun Int.toLrcTimestamp(): String {
    val minutes = this / 60000
    val seconds = (this % 60000) / 1000
    val milliseconds = this % 1000

    val leadingZeros: Array<String> = arrayOf(
        if (minutes < 10) "0" else "",
        if (seconds < 10) "0" else "",
        if (milliseconds < 10) "00" else if (milliseconds < 100) "0" else ""
    )

    return "${leadingZeros[0]}$minutes:${leadingZeros[1]}$seconds.${leadingZeros[2]}$milliseconds"
}