/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.mardous.booming.data.remote.lyrics.api.unison

import android.util.Log
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.lyrics.RawLyrics
import com.mardous.booming.data.remote.lyrics.api.LyricsApi
import com.mardous.booming.data.remote.lyrics.api.LyricsApiResult
import com.mardous.booming.data.remote.lyrics.api.LyricsProvider
import com.mardous.booming.data.remote.lyrics.api.LyricsResultConfidence
import com.mardous.booming.data.remote.lyrics.api.LyricsResultQuality
import com.mardous.booming.data.remote.lyrics.model.UnisonLyricsResponse
import com.mardous.booming.util.Constants.USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.userAgent
import java.io.IOException

class UnisonApi(private val client: HttpClient) : LyricsApi {

    override val provider = LyricsProvider.Unison

    override suspend fun downloadLyrics(
        song: Song,
        title: String,
        artist: String
    ): LyricsApiResult? {
        val response = client.get(UNISON_API_URL) {
            userAgent(USER_AGENT)
            parameter("song", title)
            parameter("artist", artist)
            song.albumName.takeIf { it.isNotBlank() }?.let { parameter("album", it) }
            (song.duration / 1000).takeIf { it > 0 }?.let { parameter("duration", it) }
            timeout {
                connectTimeoutMillis = 5000
                socketTimeoutMillis = 10000
                requestTimeoutMillis = 15000
            }
        }

        if (response.status == HttpStatusCode.NotFound) return null
        if (response.status == HttpStatusCode.TooManyRequests) {
            Log.w(TAG, "Unison rate limited the request; Retry-After=${response.headers[HttpHeaders.RetryAfter]}")
            throw IOException("Unison rate limit exceeded")
        }
        if (response.status != HttpStatusCode.OK) {
            throw IOException("Unexpected Unison response: ${response.status.value}")
        }

        val result = response.body<UnisonLyricsResponse>()
        val data = result.data
        if (!result.success || data == null || data.lyrics.isBlank()) {
            throw IOException("Invalid Unison lyrics response")
        }

        val format = data.format.lowercase()
        val quality = when {
            format == "plain" -> LyricsResultQuality.Plain
            data.syncType.equals("richsync", ignoreCase = true) -> LyricsResultQuality.WordSynced
            else -> LyricsResultQuality.LineSynced
        }
        val lyrics = if (quality == LyricsResultQuality.Plain) {
            RawLyrics.Remote(plain = RawLyrics.Remote.Content(provider.displayName, data.lyrics))
        } else {
            RawLyrics.Remote(synced = RawLyrics.Remote.Content(provider.displayName, data.lyrics))
        }
        val confidence = when (data.confidence?.lowercase()) {
            "low" -> LyricsResultConfidence.Low
            "medium" -> LyricsResultConfidence.Medium
            "high" -> LyricsResultConfidence.High
            else -> null
        }
        return LyricsApiResult(lyrics, quality, confidence)
    }

    companion object {
        private const val TAG = "UnisonApi"
        private const val UNISON_API_URL = "https://unison.boidu.dev/lyrics"
    }
}
