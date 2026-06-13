package com.mardous.booming.data.remote.lyrics.api.betterlyrics

import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.model.network.NetworkFeature
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.model.BetterLyricsResponse
import com.mardous.booming.util.Constants.BETTERLYRICS_API_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode

class BetterLyricsApi(private val client: HttpClient) : LyricsApi {

    override val name: String = "BetterLyrics"
    override val networkFeature = NetworkFeature.Lyrics.BetterLyrics

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): RawLyrics.Remote? {
        val response = client.get(BETTERLYRICS_API_URL) {
            parameter("s", title)
            parameter("a", artist)
            parameter("d", (song.duration / 1000))
            parameter("al", song.albumName)
            timeout {
                connectTimeoutMillis = 5000
                socketTimeoutMillis = 10000
                requestTimeoutMillis = 15000
            }
        }
        if (response.status == HttpStatusCode.OK) {
            val result = response.body<BetterLyricsResponse>()
            if (result.ttml.isNotEmpty()) {
                return RawLyrics.Remote(
                    synced = RawLyrics.Remote.Content(name, result.ttml)
                )
            }
        }
        return null
    }
}