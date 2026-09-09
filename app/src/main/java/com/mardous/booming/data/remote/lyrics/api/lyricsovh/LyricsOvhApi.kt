/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.mardous.booming.data.remote.lyrics.api.lyricsovh

import android.util.Log
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.api.LyricsApiResult
import com.mardous.booming.data.remote.lyrics.api.LyricsProvider
import com.mardous.booming.data.remote.lyrics.api.LyricsResultQuality
import com.mardous.booming.data.remote.lyrics.model.LyricsOvhResponse
import com.mardous.booming.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import io.ktor.http.userAgent
import java.io.IOException

class LyricsOvhApi(private val client: HttpClient) : LyricsApi {

    override val provider = LyricsProvider.LyricsOvh

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): LyricsApiResult? {
        val encodedArtist = artist.encodeURLPathPart()
        val encodedTitle = title.encodeURLPathPart()
        val response = client.get("$LYRICS_OVH_API_URL/$encodedArtist/$encodedTitle") {
            userAgent(USER_AGENT)
            timeout {
                connectTimeoutMillis = 5000
                socketTimeoutMillis = 10000
                requestTimeoutMillis = 15000
            }
        }

        if (response.status == HttpStatusCode.NotFound) return null
        if (response.status == HttpStatusCode.TooManyRequests) {
            Log.w(TAG, "lyrics.ovh rate limited the request; Retry-After=${response.headers[HttpHeaders.RetryAfter]}")
            throw IOException("lyrics.ovh rate limit exceeded")
        }
        if (response.status != HttpStatusCode.OK) {
            throw IOException("Unexpected lyrics.ovh response: ${response.status.value}")
        }

        val lyrics = response.body<LyricsOvhResponse>().lyrics
            ?.takeIf { it.isNotBlank() }
            ?: throw IOException("Invalid lyrics.ovh response")
        return LyricsApiResult(
            lyrics = RawLyrics.Remote(
                plain = RawLyrics.Remote.Content(provider.displayName, lyrics)
            ),
            quality = LyricsResultQuality.Plain
        )
    }

    companion object {
        private const val TAG = "LyricsOvhApi"
        private const val LYRICS_OVH_API_URL = "https://api.lyrics.ovh/v1"
    }
}
