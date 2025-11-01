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

package com.mardous.booming.data.remote.deezer

import android.util.Log
import com.mardous.booming.data.remote.deezer.model.DeezerAlbum
import com.mardous.booming.data.remote.deezer.model.DeezerArtist
import com.mardous.booming.data.remote.deezer.model.DeezerTrack
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter

class DeezerService(private val client: HttpClient) {
    suspend fun track(artistName: String, title: String) =
        client.get("https://api.deezer.com/search?limit=1") {
            url {
                encodedParameters.append("q", "$artistName $title".encodeURLParameter())
            }
        }.body<DeezerTrack>()

    suspend fun album(artistName: String, name: String, limit: Int = 1) =
        client.get("https://api.deezer.com/search/album?limit=$limit") {
            url {
                encodedParameters.append("q", "$artistName $name".encodeURLParameter())
            }
        }.body<DeezerAlbum>()

    suspend fun artist(artistName: String, limit: Int = 1, index: Int = 0) =
        try {
            client.get("https://api.deezer.com/search/artist") {
                url {
                    parameters.append("limit", limit.toString())
                    parameters.append("index", index.toString())
                    parameters.append("q", artistName)
                }
            }.body<DeezerArtist>()
        } catch (e: Exception) {
            Log.w("DeezerService", "Couldn't decode Deezer response for $artistName", e)
            null
        }
}