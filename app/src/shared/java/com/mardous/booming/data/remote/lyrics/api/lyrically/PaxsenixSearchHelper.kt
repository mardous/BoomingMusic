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

import android.util.Log
import com.mardous.booming.data.remote.lyrics.model.ITunesSearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

class PaxsenixSearchHelper(
    private val client: HttpClient
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAppleMusicSearchResponse(
        songTitle: String,
        artistName: String
    ): ITunesSearchResponse? {
        val response = client.get("${SEARCH_URL}/search") {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, ITUNES_USER_AGENT)
            parameter("media", "music")
            parameter("entity", "song")
            parameter("limit", "10")
            url.encodedParameters.append("term", "$songTitle $artistName")
        }

        val responseBody = response.bodyAsText(Charsets.UTF_8)
        val searchResponse = try {
            json.decodeFromString<ITunesSearchResponse>(responseBody)
        } catch (e: Exception) {
            Log.e("PaxsenixSearchHelper", "Failed to decode search response", e)
            return null
        }
        return searchResponse
    }

    companion object {
        private const val SEARCH_URL = "https://itunes.apple.com"
        private const val ITUNES_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
    }
}