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

package com.mardous.booming.data.remote.lyrics.api.spotify

import com.mardous.booming.data.model.Song
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.api.SpotifySource
import com.mardous.booming.data.remote.lyrics.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter

class SpotifyLyricsApi(private val client: HttpClient) : LyricsApi {

    private val searchResults = hashMapOf<Long, LyricsSearchResponse>()

    override suspend fun songLyrics(
        song: Song,
        title: String,
        artist: String
    ): DownloadedLyrics? {
        return searchTrack(song, title, artist).let {
            client.get("https://booming-music-api.vercel.app/api/lyrics?source=$SpotifySource&id=${it.id}")
                .body<SpotifyLyricsResponse>()
                .let { response -> parseLyrics(song, response) }
        }
    }

    private suspend fun searchTrack(song: Song, title: String, artist: String): LyricsSearchResponse {
        return searchResults.getOrPut(song.id) {
            client.get("https://booming-music-api.vercel.app/api/search?source=$SpotifySource") {
                url.encodedParameters.append("q", "$artist $title".encodeURLParameter())
            }.body<List<LyricsSearchResponse>>().first()
        }
    }

    private fun parseLyrics(song: Song, response: SpotifyLyricsResponse): DownloadedLyrics? {
        if (response.lines.isNullOrEmpty()) return null

        val lines = response.lines.filter { it.words.isNotBlank() }
        return song.toDownloadedLyrics(
            plainLyrics = lines.joinToString("\n") { it.words },
            syncedLyrics = lines.joinToString("\n") { "[${it.startTime.toLrcTimestamp()}] ${it.words}" }
        )
    }
}