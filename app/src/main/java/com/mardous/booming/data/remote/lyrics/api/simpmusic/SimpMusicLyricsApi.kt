package com.mardous.booming.data.remote.lyrics.api.simpmusic

import com.mardous.booming.data.model.Song
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.model.DownloadedLyrics
import com.mardous.booming.data.remote.lyrics.model.SimpMusicLyricsResponse
import com.mardous.booming.data.remote.lyrics.model.toDownloadedLyrics
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import kotlin.math.max
import kotlin.math.min

class SimpMusicLyricsApi(private val client: HttpClient) : LyricsApi {

    override suspend fun songLyrics(
        song: Song,
        title: String,
        artist: String
    ): DownloadedLyrics? {
        val response = client.get("https://api-lyrics.simpmusic.org/v1/search") {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, "SimpMusicLyrics/1.0")
            header(HttpHeaders.ContentType, "application/json")
            parameter("limit", "10")
            url.encodedParameters.append("q", "$artist $title".encodeURLParameter())
        }
        if (response.status == HttpStatusCode.OK) {
            val searchResult = response.body<SimpMusicLyricsResponse>()
            if (searchResult.success) {
                val songDurationInSeconds = (song.duration / 1000).toInt()
                val filtered = searchResult.data.firstOrNull {
                    val titleSingularityScore = JW_SIMILARITY.apply(it.songTitle, title)
                    val artistSingularityScore = JW_SIMILARITY.apply(it.artistName, artist)
                    if (titleSingularityScore >= 0.75 && artistSingularityScore >= 0.85) {
                        return@firstOrNull true
                    } else {
                        val durationDifference = (max(songDurationInSeconds, it.durationInSeconds) -
                                min(songDurationInSeconds, it.durationInSeconds))
                        return@firstOrNull durationDifference <= 2
                    }
                }
                if (filtered != null) {
                    return song.toDownloadedLyrics(
                        plainLyrics = filtered.plainLyrics,
                        syncedLyrics = filtered.syncedLyrics
                    )
                }
            }
        }
        return null
    }

    companion object {
        val JW_SIMILARITY = JaroWinklerSimilarity()
    }
}