package com.mardous.booming.data.remote.lyrics.api.lrclib

import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.remote.RateLimiter
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.api.LyricsProvider
import com.mardous.booming.data.remote.lyrics.model.LRCLibResponse
import com.mardous.booming.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter
import io.ktor.http.userAgent
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import kotlin.math.abs

class LrcLibApi(private val client: HttpClient) : LyricsApi {

    override val provider = LyricsProvider.LRCLib

    private val rateLimiter = RateLimiter("LrcLibApi")

    override suspend fun downloadLyrics(song: Song, title: String, artist: String): RawLyrics.Remote? {
        if (rateLimiter.isRateLimited) return null

        val response = client.get(LRCLIB_API_URL) {
            userAgent(USER_AGENT)
            timeout {
                connectTimeoutMillis = 5000
                socketTimeoutMillis = 10000
                requestTimeoutMillis = 15000
            }
            url.encodedParameters.append("q", "$artist $title".encodeURLParameter())
            url.encodedParameters.append("album_name", song.albumName.encodeURLParameter())
        }

        if (rateLimiter.handleResponse(response)) return null

        val lyrics = response.body<List<LRCLibResponse>>()
        if (lyrics.isEmpty()) {
            return null
        } else {
            val songDurationInSeconds = (song.duration / 1000).toDouble()
            var matchingLyrics = lyrics.maxByOrNull {
                val titleScore = JW_SIMILARITY.apply(title, it.title)
                val artistScore = JW_SIMILARITY.apply(artist, it.artist)

                val durationScore = it.durationInSeconds
                    ?.let { resultDurationInSeconds ->
                        val durationDiff = abs(resultDurationInSeconds - songDurationInSeconds)
                        when {
                            durationDiff <= 2000 -> 1.0 // Excellent match
                            durationDiff <= 5000 -> 0.6 // Good match
                            durationDiff <= 10000 -> 0.2 // Acceptable match
                            else -> -1.0 // Likely wrong version
                        }
                    } ?: 0.0

                (artistScore + titleScore + durationScore)
            }
            if (matchingLyrics == null) {
                matchingLyrics = lyrics.first { !it.plainLyrics.isNullOrEmpty() }
            }
            return RawLyrics.Remote(
                plain = RawLyrics.Remote.Content(provider.displayName, matchingLyrics.plainLyrics),
                synced = RawLyrics.Remote.Content(provider.displayName, matchingLyrics.syncedLyrics),
                instrumental = matchingLyrics.instrumental
            )
        }
    }

    companion object {
        private const val LRCLIB_API_URL = "https://lrclib.net/api/search"
        private val JW_SIMILARITY = JaroWinklerSimilarity()
    }
}