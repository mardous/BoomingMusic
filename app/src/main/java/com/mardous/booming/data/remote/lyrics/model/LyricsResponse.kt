package com.mardous.booming.data.remote.lyrics.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LRCLibResponse(
    @SerialName("trackName")
    val title: String,
    @SerialName("artistName")
    val artist: String,
    @SerialName("albumName")
    val album: String,
    val instrumental: Boolean,
    @SerialName("duration")
    val durationInSeconds: Double,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

@Serializable
class BetterLyricsResponse(
    val ttml: String
)

@Serializable
data class LyricallyLyricsResponse(
    val type: String,
    val content: List<LyricallyLyricsContent> = emptyList(),
    val lrc: String? = null,
    val elrc: String? = null,
    val elrcMultiPerson: String? = null,
    val ttml: String? = null,
    val plain: String? = null
)

@Serializable
data class LyricallyLyricsContent(
    val timestamp: Long,
    val endtime: Long,
    val duration: Long,
    val structure: String? = null,
    val text: List<LyricallyLyricText> = emptyList(),
    val background: Boolean = false,
    val backgroundText: List<LyricallyLyricText> = emptyList(),
    val oppositeTurn: Boolean = false
)

@Serializable
data class LyricallyLyricText(
    val text: String,
    val timestamp: Long,
    val endtime: Long,
    val part: Boolean
)