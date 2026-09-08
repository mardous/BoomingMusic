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

package com.mardous.booming.data.remote

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

private fun provideDefaultCache(context: Context): Cache? {
    val cacheDir = File(context.cacheDir.absolutePath, "/okhttp-cache/")
    if (cacheDir.mkdirs() || cacheDir.isDirectory) {
        return Cache(cacheDir, 1024 * 1024 * 10)
    }
    return null
}

private fun cacheInterceptor(): Interceptor {
    return Interceptor { chain ->
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        val response = chain.proceed(originalRequest)

        // Only cache Deezer and Last.fm API responses
        if (url.contains("api.deezer.com") || url.contains("ws.audioscrobbler.com")) {
            val cacheControl = CacheControl.Builder()
                .maxAge(4, TimeUnit.DAYS)
                .build()

            response.newBuilder()
                .header("Cache-Control", cacheControl.toString())
                .build()
        } else {
            response
        }
    }
}

private fun offlineCacheInterceptor(): Interceptor {
    return Interceptor { chain ->
        var request = chain.request()
        val url = request.url.toString()

        if ((url.contains("api.deezer.com") || url.contains("ws.audioscrobbler.com")) &&
            !com.mardous.booming.data.model.network.NetworkFeature.isOnline(ignoreWifiSetting = true)
        ) {
            val cacheControl = CacheControl.Builder()
                .onlyIfCached()
                .maxStale(28, TimeUnit.DAYS)
                .build()
            request = request.newBuilder()
                .cacheControl(cacheControl)
                .build()
        }
        chain.proceed(request)
    }
}

private fun headerInterceptor(context: Context): Interceptor {
    return Interceptor {
        val original = it.request()
        val request = original.newBuilder()
            .header("LastFmUser-Agent", context.packageName)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .method(original.method, original.body)
            .build()
        it.proceed(request)
    }
}

fun provideOkHttp(context: Context): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(headerInterceptor(context))
        .addInterceptor(offlineCacheInterceptor())
        .addNetworkInterceptor(cacheInterceptor())
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .cache(provideDefaultCache(context))
        .build()
}

fun jsonHttpClient(okHttpClient: OkHttpClient) = HttpClient(OkHttp) {
    engine {
        preconfigured = okHttpClient
    }
    install(ContentNegotiation) {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
        json(json)
        json(json, ContentType.Text.Html)
        json(json, ContentType.Text.Plain)
    }
    install(ContentEncoding) {
        gzip()
        deflate()
    }
}