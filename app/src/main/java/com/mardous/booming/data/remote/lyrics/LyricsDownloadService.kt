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

package com.mardous.booming.data.remote.lyrics

import android.util.Log
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.model.network.NetworkFeature
import com.mardous.booming.data.remote.lyrics.api.betterlyrics.BetterLyricsApi
import com.mardous.booming.data.remote.lyrics.api.lrclib.LrcLibApi
import com.mardous.booming.data.remote.lyrics.api.lyrically.LyricallyApi
import com.mardous.booming.extensions.media.albumArtistName
import com.mardous.booming.extensions.media.extractMainArtistName
import io.ktor.client.HttpClient
import java.io.IOException

class LyricsDownloadService(client: HttpClient) {

    private val lyricsApi = listOf(
        LyricallyApi(client),
        BetterLyricsApi(client),
        LrcLibApi(client)
    )

    @Throws(IOException::class)
    suspend fun remoteLyrics(
        song: Song,
        title: String = song.title,
        artist: String = song.albumArtistName(),
        fromUser: Boolean = false
    ): RawLyrics.Remote {
        var result = RawLyrics.Remote()

        if (song == Song.emptySong || !NetworkFeature.isOnline(ignoreWifiSetting = fromUser))
            return result

        try {
            val cleanedTitle = cleanTitle(title)
            val cleanedArtist = artist.extractMainArtistName()
            for (api in lyricsApi) {
                if (!api.networkFeature.isEnabled)
                    continue

                val apiResult = runCatching { api.downloadLyrics(song, cleanedTitle, cleanedArtist) }
                if (apiResult.isFailure) {
                    Log.e(TAG, "Error during lyrics request", apiResult.exceptionOrNull())
                }

                val response = apiResult.getOrNull() ?: continue

                result = result.accept(response)
                if (result.hasBoth) break
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics download failed with error:", e)
        }

        return result
    }

    /**
     * Taken from [Metrolist](https://github.com/MetrolistGroup/Metrolist).
     */
    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in TITLE_CLEANUP_PATTERNS) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    companion object {
        private const val TAG = "LyricsDownloadService"

        private val TITLE_CLEANUP_PATTERNS = listOf(
            Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
            Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
            Regex("""\s*【.*?】"""),
            Regex("""\s*\|.*$"""),
            Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
            Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
            Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*\([^)]*\d{4}[^)]*\)""", RegexOption.IGNORE_CASE),
        )
    }
}