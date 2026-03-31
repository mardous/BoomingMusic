/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.data.remote.lastfm

import com.mardous.booming.BuildConfig
import com.mardous.booming.data.remote.lastfm.model.LastFmAlbum
import com.mardous.booming.data.remote.lastfm.model.LastFmArtist
import com.mardous.booming.data.remote.lastfm.model.LastFmError
import com.mardous.booming.data.remote.lastfm.model.LastFmSessionResponse
import com.mardous.booming.data.remote.lastfm.model.LastFmUserResponse
import com.mardous.booming.data.remote.lastfm.model.NowPlayingResponse
import com.mardous.booming.data.remote.lastfm.model.ScrobbleResponse
import com.mardous.booming.util.encodeMd5
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.encodeURLParameter
import io.ktor.http.userAgent
import kotlinx.serialization.json.Json

class LastFmService(private val client: HttpClient) {

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    suspend fun albumInfo(albumName: String, artistName: String, language: String?) =
        client.lastfm("album.getInfo") {
            parameter("lang", language)
            url.encodedParameters.append("album", albumName.encodeURLParameter())
            url.encodedParameters.append("artist", artistName.encodeURLParameter())
        }.body<LastFmAlbum>()

    suspend fun artistInfo(artistName: String, language: String?, cacheControl: String?) =
        client.lastfm("artist.getInfo") {
            parameter("lang", language)
            header(HttpHeaders.CacheControl, cacheControl)
            url.encodedParameters.append("artist", artistName.encodeURLParameter())
        }.body<LastFmArtist>()

    suspend fun userInfo(username: String) =
        client.lastfm("user.getInfo") {
            parameter("user", username)
        }.body<LastFmUserResponse>()

    suspend fun createSession(username: String, password: String): Any {
        val response = client.lastfmPost(
            method = "auth.getMobileSession",
            params = mapOf(
                "username" to username,
                "password" to password
            )
        )
        if (response.contains("\"error\"")) {
            return json.decodeFromString<LastFmError>(response)
        }
        return json.decodeFromString<LastFmSessionResponse>(response)
    }

    suspend fun scrobble(
        artist: String,
        track: String,
        album: String,
        timestamp: Long,
        sk: String
    ): Any {
        val response = client.lastfmPost("track.scrobble", mutableMapOf(
            "artist" to artist,
            "track" to track,
            "album" to album,
            "timestamp" to timestamp.toString(),
            "sk" to sk
        ))
        if (response.contains("\"error\"")) {
            return json.decodeFromString<LastFmError>(response)
        }
        return json.decodeFromString<ScrobbleResponse>(response)
    }

    suspend fun updateNowPlaying(
        artist: String,
        track: String,
        sk: String
    ): Any {
        val response = client.lastfmPost("track.updateNowPlaying", mutableMapOf(
            "artist" to artist,
            "track" to track,
            "sk" to sk
        ))
        if (response.contains("\"error\"")) {
            return json.decodeFromString<LastFmError>(response)
        }
        return json.decodeFromString<NowPlayingResponse>(response)
    }

    private suspend fun HttpClient.lastfm(method: String, block: HttpRequestBuilder.() -> Unit) =
        get(BASE_URL) {
            userAgent(USER_AGENT)
            parameter("format", "json")
            parameter("autocorrect", 1)
            parameter("api_key", API_KEY)
            parameter("method", method)
            block()
        }

    private suspend fun HttpClient.lastfmPost(
        method: String,
        params: Map<String, String>
    ): String {
        val allParams = params.toMutableMap().apply {
            put("api_key", API_KEY)
            put("method", method)
        }
        val apiSig = generateSignature(allParams)
        allParams["api_sig"] = apiSig
        allParams["format"] = "json"

        val response = submitForm(
            url = BASE_URL,
            formParameters = Parameters.build {
                allParams.forEach { (key, value) -> append(key, value) }
            }
        ) { userAgent(USER_AGENT) }

        return response.bodyAsText()
    }

    private fun generateSignature(params: Map<String, String>): String {
        val sortedParams = params.toSortedMap()
        val signatureBase = sortedParams.entries.joinToString("") { "${it.key}${it.value}" } + API_SECRET
        return signatureBase.encodeMd5()
    }

    companion object {
        private const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
        private const val USER_AGENT = "Booming Music/${BuildConfig.VERSION_NAME} (https://github.com/mardous/BoomingMusic)"
        private const val API_KEY = BuildConfig.LASTFM_API_KEY
        private const val API_SECRET = BuildConfig.LASTFM_SECRET
    }
}
