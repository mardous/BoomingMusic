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
import com.mardous.booming.data.remote.lyrics.model.DownloadedLyrics
import com.mardous.booming.data.remote.lyrics.model.SpotifySearchResult
import com.mardous.booming.data.remote.lyrics.model.SyncedLinesResponse
import com.mardous.booming.data.remote.lyrics.model.toDownloadedLyrics
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.encodeURLParameter

class SpotifyLyricsApi(private val client: HttpClient) : LyricsApi {

    private val searchResults = hashMapOf<Long, SpotifySearchResult>()

    override suspend fun songLyrics(
        song: Song,
        title: String,
        artist: String
    ): DownloadedLyrics? {
        return searchTrack(song, title, artist).let {
            val response = client.get("https://spotify-lyrics-api-one.vercel.app/get_lyrics") {
                parameter("format", "lrc")
                parameter("trackid", it.tracks.items.first().id)
            }.body<SyncedLinesResponse>()
            parseLyrics(song, response)
        }
    }

    private suspend fun searchTrack(song: Song, title: String, artist: String): SpotifySearchResult {
        return searchResults.getOrPut(song.id) {
            client.get("https://spotify-lyrics-api-one.vercel.app/search") {
                url.encodedParameters.append("q", "$artist $title".encodeURLParameter())
            }.body<SpotifySearchResult>()
        }
    }

    private fun parseLyrics(song: Song, result: SyncedLinesResponse): DownloadedLyrics? {
        val lines = result.lines.filter { it.words.isNotBlank() }
        return song.toDownloadedLyrics(
            plainLyrics = lines.joinToString("\n") { it.words },
            syncedLyrics = lines.joinToString("\n") { "[${it.timeTag}] ${it.words}" }
        )
    }
}