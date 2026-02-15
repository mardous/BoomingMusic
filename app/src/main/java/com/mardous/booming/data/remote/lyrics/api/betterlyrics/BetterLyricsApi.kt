package com.mardous.booming.data.remote.lyrics.api.betterlyrics

import com.mardous.booming.data.model.Song
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.model.BetterLyricsResponse
import com.mardous.booming.data.remote.lyrics.model.DownloadedLyrics
import com.mardous.booming.data.remote.lyrics.model.toDownloadedLyrics
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode

class BetterLyricsApi(private val client: HttpClient) : LyricsApi {

    override suspend fun songLyrics(
        song: Song,
        title: String,
        artist: String
    ): DownloadedLyrics? {
        val response = client.get("https://lyrics-api.boidu.dev/getLyrics") {
            parameter("s", title)
            parameter("a", artist)
            parameter("d", (song.duration / 1000))
            parameter("al", song.albumName)
        }
        if (response.status == HttpStatusCode.OK) {
            val result = response.body<BetterLyricsResponse>()
            if (result.ttml.isNotEmpty()) {
                return song.toDownloadedLyrics(
                    syncedLyrics = result.ttml
                )
            }
        }
        return null
    }
}