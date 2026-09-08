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
import com.mardous.booming.data.remote.lyrics.api.LyricsProvider
import com.mardous.booming.data.remote.lyrics.api.betterlyrics.BetterLyricsApi
import com.mardous.booming.data.remote.lyrics.api.lrclib.LrcLibApi
import com.mardous.booming.data.remote.lyrics.api.lyrically.LyricallyApi
import com.mardous.booming.extensions.media.albumArtistName
import com.mardous.booming.extensions.media.extractMainArtistName
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException

class LyricsProviderParams(
    val providers: List<LyricsProvider> = LyricsProvider.AvailableProviders,
    val ignoreWifiSetting: Boolean = false,
    val ignoreProviderSetting: Boolean = false
)

class LyricsDownloadService(client: HttpClient) {

    private val apiByProvider = mapOf(
        LyricsProvider.Lyrically to LyricallyApi(client),
        LyricsProvider.BetterLyrics to BetterLyricsApi(client),
        LyricsProvider.LRCLib to LrcLibApi(client)
    )

    @Throws(IOException::class)
    suspend fun remoteLyrics(
        song: Song,
        title: String = song.title,
        artist: String = song.albumArtistName(),
        providerParams: LyricsProviderParams = LyricsProviderParams()
    ): RawLyrics.Remote = searchLyrics(
        song = song,
        title = title,
        artist = artist,
        providerParams = providerParams,
        continueAfterComplete = false
    ).lyrics

    suspend fun searchLyrics(
        song: Song,
        title: String = song.title,
        artist: String = song.albumArtistName(),
        providerParams: LyricsProviderParams = LyricsProviderParams(),
        continueAfterComplete: Boolean = true,
        onProviderResult: suspend (LyricsProviderSearchResult) -> Unit = {}
    ): LyricsSearchResult {
        check(providerParams.providers.isNotEmpty()) { "No providers configured" }

        var result = RawLyrics.Remote()
        val providers = providerParams.providers.filter { provider ->
            provider.isAvailableForCurrentPolicy &&
                (provider.isEnabled || providerParams.ignoreProviderSetting)
        }
        val providerResults = providers.associateWith { provider ->
            LyricsProviderSearchResult(provider, LyricsProviderSearchStatus.Waiting)
        }.toMutableMap()

        if (song == Song.emptySong ||
            !NetworkFeature.isOnline(ignoreWifiSetting = providerParams.ignoreWifiSetting)) {
            providers.forEach { provider ->
                val failedResult = LyricsProviderSearchResult(
                    provider = provider,
                    status = LyricsProviderSearchStatus.Failed
                )
                providerResults[provider] = failedResult
                onProviderResult(failedResult)
            }
            return LyricsSearchResult(result, providerResults.values.toList())
        }

        try {
            val cleanedTitle = cleanTitle(title)
            val cleanedArtist = artist.extractMainArtistName()
            for (provider in providers) {
                val api = apiByProvider.getValue(provider)
                val searchingResult = LyricsProviderSearchResult(
                    provider = provider,
                    status = LyricsProviderSearchStatus.Searching
                )
                providerResults[provider] = searchingResult
                onProviderResult(searchingResult)

                val providerResult = try {
                    val response = api.downloadLyrics(song, cleanedTitle, cleanedArtist)
                    if (response?.let { it.hasPlain || it.hasSynced } == true) {
                        result = result.accept(response)
                        LyricsProviderSearchResult(
                            provider = provider,
                            status = LyricsProviderSearchStatus.Found,
                            lyrics = response
                        )
                    } else {
                        LyricsProviderSearchResult(
                            provider = provider,
                            status = LyricsProviderSearchStatus.NoMatch
                        )
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: HttpRequestTimeoutException) {
                    Log.w(TAG, "Lyrics request timed out for ${provider.displayName}", error)
                    LyricsProviderSearchResult(
                        provider = provider,
                        status = LyricsProviderSearchStatus.TimedOut
                    )
                } catch (error: SocketTimeoutException) {
                    Log.w(TAG, "Lyrics request timed out for ${provider.displayName}", error)
                    LyricsProviderSearchResult(
                        provider = provider,
                        status = LyricsProviderSearchStatus.TimedOut
                    )
                } catch (error: Exception) {
                    Log.e(TAG, "Error during lyrics request for ${provider.displayName}", error)
                    LyricsProviderSearchResult(
                        provider = provider,
                        status = LyricsProviderSearchStatus.Failed
                    )
                }

                providerResults[provider] = providerResult
                onProviderResult(providerResult)
                if (!continueAfterComplete && result.hasBoth) break
            }
        } catch (error: CancellationException) {
            throw error
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics download failed with error:", e)
        }

        return LyricsSearchResult(result, providerResults.values.toList())
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

        private const val KEYWORDS = "official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit"

        private val TITLE_CLEANUP_PATTERNS = listOf(
            Regex("""\s*\((?>[^)]*?(?:$KEYWORDS)[^)]*?)\)""", RegexOption.IGNORE_CASE),
            Regex("""\s*\[(?>[^\]]*?(?:$KEYWORDS)[^\]]*?)\]""", RegexOption.IGNORE_CASE),
            Regex("""\s*【(?>[^】]*?)】"""),
            Regex("""\s*\|.*$"""),
            Regex("""\s*-\s*(?:official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*\((?>feat\.[^)]*?)\)""", RegexOption.IGNORE_CASE),
            Regex("""\s*\((?>ft\.[^)]*?)\)""", RegexOption.IGNORE_CASE),
            Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*\((?>[^)]*?\d{4}[^)]*?)\)""", RegexOption.IGNORE_CASE),
        )
    }
}
