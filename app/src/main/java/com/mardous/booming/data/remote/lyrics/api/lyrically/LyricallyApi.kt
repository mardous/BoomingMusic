/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.data.remote.lyrics.api.lyrically

import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.model.network.NetworkFeature
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.model.ITunesSearchResponse
import com.mardous.booming.data.remote.lyrics.model.LyricallyLyricText
import com.mardous.booming.data.remote.lyrics.model.LyricallyLyricsResponse
import com.mardous.booming.util.Constants.LYRICALLY_API_URL
import com.mardous.booming.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.userAgent
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import kotlin.math.abs

/**
 * Fetch lyrics from Lyrically API.
 *
 * Based on [Metrolist](https://github.com/MetrolistGroup/Metrolist) and
 * [SongSync](https://github.com/Lambada10/SongSync) implementations.
 */
class LyricallyApi(private val client: HttpClient) : LyricsApi {

    override val name: String = "Lyrically"
    override val networkFeature = NetworkFeature.Lyrics.Lyrically

    private val searchHelper = PaxsenixSearchHelper(client)

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): RawLyrics.Remote? {
        val searchResponse = searchHelper.getAppleMusicSearchResponse(title, artist)
        if (searchResponse != null && searchResponse.size > 0) {
            var lyrics: RawLyrics.Remote? = null

            val scoredIds = getScoredAppleMusicIds(title, artist, song.duration, searchResponse)
            for ((result, score) in scoredIds.take(5)) {
                if (score <= 0.0) continue

                val lyricsResponse = client.paxsenix("${LYRICALLY_API_URL}/apple-music/lyrics") {
                    parameter("id", result)
                }.body<LyricallyLyricsResponse>()

                val newLyrics = parseLyricallyResponse(lyricsResponse)

                lyrics = lyrics?.accept(newLyrics) ?: newLyrics
                if (lyrics.hasBoth) {
                    return lyrics
                }
            }

            return lyrics
        }

        return null
    }

    private fun parseLyricallyResponse(response: LyricallyLyricsResponse): RawLyrics.Remote {
        var lyrics = if (!response.ttml.isNullOrEmpty()) {
            RawLyrics.Remote(synced = RawLyrics.Remote.Content(name, response.ttml))
        } else if (!response.elrcMultiPerson.isNullOrEmpty()) {
            RawLyrics.Remote(synced = RawLyrics.Remote.Content(name, response.elrcMultiPerson))
        } else if (!response.elrc.isNullOrEmpty()) {
            RawLyrics.Remote(synced = RawLyrics.Remote.Content(name, response.elrc))
        } else if (!response.lrc.isNullOrEmpty()) {
            RawLyrics.Remote(synced = RawLyrics.Remote.Content(name, response.lrc))
        } else {
            RawLyrics.Remote(synced = RawLyrics.Remote.Content(name, parseLyricallyContent(response)))
        }
        if (!response.plain.isNullOrEmpty()) {
            lyrics = lyrics.copy(plain = RawLyrics.Remote.Content(name, response.plain))
        }
        return lyrics
    }

    private fun parseLyricallyContent(response: LyricallyLyricsResponse): String? {
        if (response.content.isEmpty()) return null

        val syncedLyrics = StringBuilder()
        val lines = response.content
        when (response.type) {
            "Syllable" -> {
                val isMultiPerson = lines.any { it.oppositeTurn }
                for (line in lines) {
                    syncedLyrics.append("[${line.timestamp.toLrcTimestamp()}]")

                    if (isMultiPerson) {
                        syncedLyrics.append(if (line.oppositeTurn) "v2:" else "v1:")
                    }

                    formatSyllableToLrc(syncedLyrics, line.text)

                    if (line.background) {
                        syncedLyrics.append("\n[bg:")
                        formatSyllableToLrc(syncedLyrics, line.backgroundText)
                        syncedLyrics.append("]")
                    }
                    syncedLyrics.append("\n")
                }
            }

            "Line" -> {
                for (line in lines) {
                    syncedLyrics.append("[${line.timestamp.toLrcTimestamp()}] ${line.text[0].text}\n")
                }
            }
        }

        return syncedLyrics.toString().dropLast(1)
    }

    private fun formatSyllableToLrc(output: StringBuilder, content: List<LyricallyLyricText>) {
        for (syllable in content) {
            val formatedBeginTimestamp = "<${syllable.timestamp.toLrcTimestamp()}>"
            val formatedEndTimestamp = "<${syllable.endtime.toLrcTimestamp()}>"
            if (!output.endsWith(formatedBeginTimestamp)) {
                output.append(formatedBeginTimestamp)
            }
            output.append(syllable.text)
            if (!syllable.part) {
                output.append(" ")
            }
            output.append(formatedEndTimestamp)
        }
    }

    private fun getScoredAppleMusicIds(
        songTitle: String,
        songArtist: String,
        songDurationInMillis: Long,
        searchResponse: ITunesSearchResponse
    ): List<Pair<Long, Double>> {
        return searchResponse.results.map { song ->
            val songId = song.id

            val titleScore = JW_SIMILARITY.apply(songTitle, song.name)
            val artistScore = JW_SIMILARITY.apply(songArtist, song.artist)

            val durationDiff = song.durationInMillis
                ?.let { duration -> abs(duration - songDurationInMillis) } ?: 0

            val durationScore = when {
                durationDiff <= 2000 -> 1.0 // Excellent match
                durationDiff <= 5000 -> 0.6 // Good match
                durationDiff <= 10000 -> 0.2 // Acceptable match
                else -> -1.0 // Likely wrong version
            }

            songId to (artistScore + titleScore + durationScore)
        }.sortedByDescending { it.second }
    }

    private suspend fun HttpClient.paxsenix(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ) = get(url) {
        header(HttpHeaders.Accept, "application/json")
        header(HttpHeaders.ContentType, "application/json")
        userAgent(USER_AGENT)
        timeout {
            connectTimeoutMillis = 5000
            socketTimeoutMillis = 10000
            requestTimeoutMillis = 15000
        }
        block()
    }

    private fun Long.toLrcTimestamp(): String {
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

    companion object {
        private const val TAG = "LyricallyApi"

        private val JW_SIMILARITY = JaroWinklerSimilarity()
    }
}