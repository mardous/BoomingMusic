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

import android.os.SystemClock
import android.util.Log
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.model.network.NetworkFeature
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.api.LyricsProvider
import com.mardous.booming.data.remote.lyrics.api.betterlyrics.BetterLyricsApi
import com.mardous.booming.data.remote.lyrics.api.lyricsovh.LyricsOvhApi
import com.mardous.booming.data.remote.lyrics.api.lrclib.LrcLibApi
import com.mardous.booming.data.remote.lyrics.api.lyrically.LyricallyApi
import com.mardous.booming.data.remote.lyrics.api.unison.UnisonApi
import com.mardous.booming.extensions.media.albumArtistName
import com.mardous.booming.extensions.media.extractMainArtistName
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class LyricsProviderParams(
    val providers: List<LyricsProvider> = LyricsProvider.AvailableProviders,
    val ignoreWifiSetting: Boolean = false,
    val ignoreProviderSetting: Boolean = false
)

class LyricsDownloadService(client: HttpClient) {

    private val apiByProvider: Map<LyricsProvider, LyricsApi> = mapOf(
        LyricsProvider.Lyrically to LyricallyApi(client),
        LyricsProvider.BetterLyrics to BetterLyricsApi(client),
        LyricsProvider.LRCLib to LrcLibApi(client),
        LyricsProvider.Unison to UnisonApi(client),
        LyricsProvider.LyricsOvh to LyricsOvhApi(client)
    )

    @Throws(IOException::class)
    suspend fun remoteLyrics(
        song: Song,
        title: String = song.title,
        artist: String = song.albumArtistName(),
        providerParams: LyricsProviderParams = LyricsProviderParams()
    ): RawLyrics.Remote {
        check(providerParams.providers.isNotEmpty()) { "No providers configured" }

        val providers = enabledProviders(providerParams)
        if (providers.isEmpty() || song == Song.emptySong ||
            !NetworkFeature.isOnline(ignoreWifiSetting = providerParams.ignoreWifiSetting)) {
            return RawLyrics.Remote()
        }

        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = artist.extractMainArtistName()
        Log.d(TAG, "Automatic lyrics search started: providers=${providers.providerNames()}")
        return supervisorScope {
            val resultChannel = Channel<LyricsProviderSearchResult>(providers.size)
            val jobs = providers.map { provider ->
                launch {
                    resultChannel.send(
                        fetchProviderResult(provider, song, cleanedTitle, cleanedArtist)
                    )
                }
            }

            repeat(providers.size) {
                val result = resultChannel.receive()
                if (result.isUsable) {
                    Log.d(
                        TAG,
                        "Automatic lyrics search selected ${result.provider.displayName} " +
                            "(${result.quality}); cancelling remaining providers"
                    )
                    jobs.forEach { it.cancel() }
                    return@supervisorScope result.lyrics ?: RawLyrics.Remote()
                }
            }
            RawLyrics.Remote()
        }
    }

    suspend fun searchLyrics(
        song: Song,
        title: String = song.title,
        artist: String = song.albumArtistName(),
        providerParams: LyricsProviderParams = LyricsProviderParams(),
        onProviderResult: suspend (LyricsProviderSearchResult) -> Unit = {}
    ): LyricsSearchResult {
        check(providerParams.providers.isNotEmpty()) { "No providers configured" }

        val providers = enabledProviders(providerParams)

        if (song == Song.emptySong ||
            !NetworkFeature.isOnline(ignoreWifiSetting = providerParams.ignoreWifiSetting)) {
            val providerResults = providers.map { provider ->
                val failedResult = LyricsProviderSearchResult(
                    provider = provider,
                    status = LyricsProviderSearchStatus.Failed
                )
                onProviderResult(failedResult)
                failedResult
            }
            return LyricsSearchResult(RawLyrics.Remote(), providerResults)
        }

        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = artist.extractMainArtistName()
        Log.d(TAG, "Interactive lyrics search started: providers=${providers.providerNames()}")
        val providerResults = coroutineScope {
            providers.map { provider ->
                async {
                    val searchingResult = LyricsProviderSearchResult(
                        provider = provider,
                        status = LyricsProviderSearchStatus.Searching
                    )
                    onProviderResult(searchingResult)
                    val providerResult = fetchProviderResult(
                        provider,
                        song,
                        cleanedTitle,
                        cleanedArtist
                    )
                    onProviderResult(providerResult)
                    providerResult
                }
            }
                .awaitAll()
        }

        val recommended = providerResults
            .filter { it.isUsable }
            .maxByOrNull { it.qualityScore }
        return LyricsSearchResult(recommended?.lyrics ?: RawLyrics.Remote(), providerResults)
    }

    private fun enabledProviders(providerParams: LyricsProviderParams): List<LyricsProvider> {
        return providerParams.providers.filter { provider ->
            provider.isAvailableForCurrentPolicy &&
                (provider.isEnabled || providerParams.ignoreProviderSetting)
        }
    }

    private suspend fun fetchProviderResult(
        provider: LyricsProvider,
        song: Song,
        title: String,
        artist: String
    ): LyricsProviderSearchResult {
        val startedAt = SystemClock.elapsedRealtime()
        Log.d(TAG, "Lyrics provider started: ${provider.displayName}")
        val result = try {
            val response = apiByProvider.getValue(provider).downloadLyrics(song, title, artist)
            if (response?.lyrics?.let { it.hasPlain || it.hasSynced } == true) {
                LyricsProviderSearchResult(
                    provider = provider,
                    status = LyricsProviderSearchStatus.Found,
                    lyrics = response.lyrics,
                    quality = response.quality,
                    confidence = response.confidence
                )
            } else {
                LyricsProviderSearchResult(
                    provider = provider,
                    status = LyricsProviderSearchStatus.NoMatch
                )
            }
        } catch (error: CancellationException) {
            Log.d(
                TAG,
                "Lyrics provider cancelled: ${provider.displayName}; " +
                    "durationMs=${SystemClock.elapsedRealtime() - startedAt}"
            )
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
        Log.d(
            TAG,
            "Lyrics provider finished: ${provider.displayName}; status=${result.status}; " +
                "quality=${result.quality}; durationMs=${SystemClock.elapsedRealtime() - startedAt}"
        )
        return result
    }

    private fun List<LyricsProvider>.providerNames(): String =
        joinToString(prefix = "[", postfix = "]") { it.displayName }

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
