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

package com.mardous.booming.data.remote.musicbrainz

import com.mardous.booming.data.remote.musicbrainz.model.MusicBrainzRecording
import com.mardous.booming.data.remote.musicbrainz.model.MusicBrainzSearchResponse
import com.mardous.booming.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

class MusicBrainzService(private val client: HttpClient) {

    suspend fun searchRecording(title: String, artist: String): MusicBrainzSearchResponse {
        val query = "recording:\"$title\" AND artist:\"$artist\""
        val body = client.get("${BASE_URL}recording") {
            parameter("query", query)
            parameter("fmt", "json")
            header("User-Agent", USER_AGENT)
        }.body<MusicBrainzSearchResponse>()
        return body
    }

    suspend fun getRecordingDetails(id: String): MusicBrainzRecording {
        val body = client.get("${BASE_URL}recording/$id") {
            parameter("inc", "artist-credits+releases+genres+work-rels+artist-rels+work-level-rels+media")
            parameter("fmt", "json")
            header("User-Agent", USER_AGENT)
        }.body<MusicBrainzRecording>()
        return body
    }

    companion object {
        const val BASE_URL = "https://musicbrainz.org/ws/2/"
        const val COVER_ARCHIVE_URL = "https://coverartarchive.org/"
    }
}
