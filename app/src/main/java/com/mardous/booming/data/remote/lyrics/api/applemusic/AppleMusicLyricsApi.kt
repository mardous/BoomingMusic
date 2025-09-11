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

import com.mardous.booming.data.model.Song
import com.mardous.booming.data.remote.lyrics.api.AppleMusicSource
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter

class AppleMusicLyricsApi(private val client: HttpClient) : LyricsApi {

    private val searchResults = hashMapOf<Long, LyricsSearchResponse>()

    override suspend fun songLyrics(
        song: Song,
        title: String,
        artist: String
    ): DownloadedLyrics? {
        return searchTrack(song, title, artist).let {
            client.get("https://booming-music-api.vercel.app/api/lyrics?source=$AppleMusicSource&id=${it.id}")
                .body<AppleLyricsResponse>()
                .let { response -> parseLyrics(song, response) }
        }
    }

    private suspend fun searchTrack(song: Song, title: String, artist: String): LyricsSearchResponse {
        return searchResults.getOrPut(song.id) {
            val response = client.get("https://booming-music-api.vercel.app/api/search?source=$AppleMusicSource") {
                url.encodedParameters.append("q", "$title $artist".encodeURLParameter())
            }
            response.body<List<LyricsSearchResponse>>().first()
        }
    }

    private fun parseLyrics(song: Song, response: AppleLyricsResponse): DownloadedLyrics? {
        if (response.lines.isNullOrEmpty()) {
            return null
        }
        val syncedLyrics = StringBuilder()
        val lines = response.lines
        if (response.syllable) {
            val isMultiPerson = lines.any { it.oppositeTurn }
            for (line in lines) {
                syncedLyrics.append("[${line.startTime.toLrcTimestamp()}]")
                if (isMultiPerson) {
                    syncedLyrics.append(if (line.oppositeTurn) "v2:" else "v1:")
                }
                for (syllable in line.words) {
                    syncedLyrics.append("<${syllable.startTime!!.toLrcTimestamp()}>${syllable.text}")
                    if (!syllable.breaks) {
                        syncedLyrics.append(" ")
                    }
                    syncedLyrics.append("<${syllable.endTime?.toLrcTimestamp()}>")
                }
                syncedLyrics.append("<${line.endTime.toLrcTimestamp()}>\n")
            }
        } else {
            for (line in lines) {
                syncedLyrics.append("[${line.startTime.toLrcTimestamp()}] ${line.words[0].text}\n")
            }
        }
        return song.toDownloadedLyrics(syncedLyrics = syncedLyrics.toString().dropLast(1))
    }
}