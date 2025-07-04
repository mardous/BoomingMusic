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

package com.mardous.booming.http.lyrics

import com.mardous.booming.extensions.media.albumArtistName
import com.mardous.booming.http.lyrics.applemusic.AppleMusicLyricsApi
import com.mardous.booming.http.lyrics.lrclib.LrcLibApi
import com.mardous.booming.http.lyrics.spotify.SpotifyLyricsApi
import com.mardous.booming.model.DownloadedLyrics
import com.mardous.booming.model.Song
import com.mardous.booming.model.toDownloadedLyrics
import io.ktor.client.HttpClient
import java.io.IOException

class LyricsDownloadService(client: HttpClient) {

    private val lyricsApi = listOf(
        LrcLibApi(client),
        AppleMusicLyricsApi(client),
        SpotifyLyricsApi(client)
    )

    @Throws(IOException::class)
    suspend fun getLyrics(
        song: Song,
        title: String = song.title,
        artist: String = song.albumArtistName()
    ): DownloadedLyrics {
        var downloadedLyrics = song.toDownloadedLyrics()
        if (song == Song.emptySong) {
            return downloadedLyrics
        }
        for (api in lyricsApi) {
            val apiResult = runCatching { api.songLyrics(song, title, artist) }.getOrNull()
                ?: continue

            val plainLyrics = downloadedLyrics.plainLyrics ?: apiResult.plainLyrics
            val syncedLyrics = downloadedLyrics.syncedLyrics ?: apiResult.syncedLyrics

            downloadedLyrics = downloadedLyrics.copy(
                plainLyrics = plainLyrics,
                syncedLyrics = syncedLyrics
            )

            if (downloadedLyrics.hasMultiOptions)
                break
        }
        return downloadedLyrics
    }
}