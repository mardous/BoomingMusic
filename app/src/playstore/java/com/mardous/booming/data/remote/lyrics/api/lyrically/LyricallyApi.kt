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
import io.ktor.client.HttpClient

class LyricallyApi(private val client: HttpClient) : LyricsApi {

    override val name: String = "Lyrically"
    override val networkFeature = NetworkFeature.Lyrics.Lyrically

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): RawLyrics.Remote? {
        return null
    }
}
