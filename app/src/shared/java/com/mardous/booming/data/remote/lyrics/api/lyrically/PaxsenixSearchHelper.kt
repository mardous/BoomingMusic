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
import com.mardous.booming.data.remote.lyrics.model.AppleMusicSearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URLEncoder

class PaxsenixSearchHelper(
    private val client: HttpClient,
    private val tokenManager: TokenManager,
    private val json: Json
) {
    suspend fun getAppleMusicSearchResponse(
        songTitle: String,
        artistName: String
    ): AppleMusicSearchResponse? {
        val search = withContext(Dispatchers.IO) {
            URLEncoder.encode("$songTitle $artistName", Charsets.UTF_8.toString())
        }
        val token = tokenManager.getToken(client)
        val response = client.get(
            "${SEARCH_URL}/search?" +
                    "term=$search&" +
                    "types=songs&" +
                    "limit=25&" +
                    "l=en-US&" +
                    "platform=web&" +
                    "format[resources]=map&" +
                    "include[songs]=artists&" +
                    "extend=artistUrl"
        ) {
            header("Authorization", "Bearer $token")
            header("Origin", "https://music.apple.com/")
            header("Referer", "https://music.apple.com/")
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0")
            header("Accept", "application/json")
            header("Accept-Language", "en-US,en;q=0.5")
            header("x-apple-renewal", "true")
        }

        val responseBody = response.bodyAsText(Charsets.UTF_8)

        if (response.status.value !in 200..299) {
            // Token might be expired, clear it and retry once
            if (response.status.value == 401) {
                tokenManager.clearToken()
            }
            return null
        }

        val searchResponse = try {
            json.decodeFromString<AppleMusicSearchResponse>(responseBody)
        } catch (e: Exception) {
            Log.e("PaxsenixSearchHelper", "Failed to decode search response", e)
            return null
        }
        return searchResponse
    }

    companion object {
        private const val SEARCH_URL = "https://amp-api.music.apple.com/v1/catalog/us"
    }
}