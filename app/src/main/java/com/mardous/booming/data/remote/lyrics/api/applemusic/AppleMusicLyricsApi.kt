/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.mardous.booming.data.remote.lyrics.api.applemusic

import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.model.AppleLyricsResponse
import com.mardous.booming.data.remote.lyrics.model.AppleSearchResponse
import com.mardous.booming.data.remote.lyrics.model.DownloadedLyrics
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.remote.lyrics.model.toDownloadedLyrics
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.encodeURLParameter

class AppleMusicLyricsApi(private val client: HttpClient) : LyricsApi {

    override suspend fun songLyrics(
        song: Song,
        title: String,
        artist: String
    ): DownloadedLyrics? {
        return client.get("https://paxsenix.alwaysdata.net/searchAppleMusic.php") {
            url.encodedParameters.append("q", "$title $artist".encodeURLParameter())
        }.body<List<AppleSearchResponse>>().first().let {
            client.get("https://paxsenix.alwaysdata.net/getAppleMusicLyrics.php") {
                parameter("id", it.id)
            }.body<AppleLyricsResponse>().let { parseLyrics(song, it) }
        }
    }

    private fun parseLyrics(song: Song, response: AppleLyricsResponse): DownloadedLyrics? {
        if (response.content.isNullOrEmpty()) {
            return null
        }
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
                    for (syllable in line.text) {
                        syncedLyrics.append("<${syllable.timestamp!!.toLrcTimestamp()}>${syllable.text}")
                        if (!syllable.part) {
                            syncedLyrics.append(" ")
                        }
                        syncedLyrics.append("<${syllable.endtime?.toLrcTimestamp()}>")
                    }
                    syncedLyrics.append("<${line.endtime.toLrcTimestamp()}>\n")
                }
            }

            "Line" -> {
                for (line in lines) {
                    syncedLyrics.append("[${line.timestamp.toLrcTimestamp()}] ${line.text[0].text}\n")
                }
            }

            else -> return null
        }
        return song.toDownloadedLyrics(syncedLyrics = syncedLyrics.toString().dropLast(1))
    }

    private fun Int.toLrcTimestamp(): String {
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
}