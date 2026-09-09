package com.mardous.booming.data.remote.lyrics.api.lrclib

import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.api.LyricsApiResult
import com.mardous.booming.data.remote.lyrics.api.LyricsProvider
import com.mardous.booming.data.remote.lyrics.api.LyricsResultQuality
import com.mardous.booming.data.remote.lyrics.model.LRCLibResponse
import com.mardous.booming.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter
import io.ktor.http.userAgent

class LrcLibApi(private val client: HttpClient) : LyricsApi {

    override val provider = LyricsProvider.LRCLib

    override suspend fun downloadLyrics(song: Song, title: String, artist: String): LyricsApiResult? {
        val lyrics = client.get(LRCLIB_API_URL) {
            userAgent(USER_AGENT)
            timeout {
                connectTimeoutMillis = 5000
                socketTimeoutMillis = 10000
                requestTimeoutMillis = 15000
            }
            url.encodedParameters.append("q", "$artist $title".encodeURLParameter())
            url.encodedParameters.append("album_name", song.albumName.encodeURLParameter())
        }.body<List<LRCLibResponse>>()
        if (lyrics.isEmpty()) {
            return null
        } else {
            val songDurationInSeconds = (song.duration / 1000).toDouble()
            var matchingLyrics = lyrics.firstOrNull {
                val resultDuration = it.durationInSeconds ?: return@firstOrNull false
                val maxValue = maxOf(songDurationInSeconds, resultDuration)
                val minValue = minOf(songDurationInSeconds, resultDuration)
                ((maxValue - minValue) < 2)
            }
            if (matchingLyrics == null) {
                matchingLyrics = lyrics.firstOrNull {
                    !it.plainLyrics.isNullOrEmpty() || !it.syncedLyrics.isNullOrEmpty()
                } ?: return null
            }
            val remoteLyrics = RawLyrics.Remote(
                plain = RawLyrics.Remote.Content(provider.displayName, matchingLyrics.plainLyrics),
                synced = RawLyrics.Remote.Content(provider.displayName, matchingLyrics.syncedLyrics),
                instrumental = matchingLyrics.instrumental
            )
            val quality = if (remoteLyrics.hasSynced) {
                LyricsResultQuality.LineSynced
            } else {
                LyricsResultQuality.Plain
            }
            return LyricsApiResult(remoteLyrics, quality)
        }
    }

    companion object {
        private const val LRCLIB_API_URL = "https://lrclib.net/api/search"
    }
}
