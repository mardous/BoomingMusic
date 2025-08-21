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

package com.mardous.booming.glide.artistimage

import android.content.Context
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.mardous.booming.data.remote.deezer.DeezerService
import com.mardous.booming.extensions.glide.GlideScope
import com.mardous.booming.extensions.isAllowedToDownloadMetadata
import com.mardous.booming.extensions.media.albumCoverUri
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.util.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.*
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.io.InputStream

class ArtistImageFetcher(
    private val context: Context,
    private val deezerService: DeezerService,
    private val httpClient: HttpClient,
    private val model: ArtistImage
) : DataFetcher<InputStream>, CoroutineScope by GlideScope() {

    private val downloadArtistImages = Preferences.downloadArtistImages
    private val preferredImageSize = Preferences.preferredArtistImageSize

    private val isAllowedToDownloadImage: Boolean
        get() = !model.name.isArtistNameUnknown() && downloadArtistImages && context.isAllowedToDownloadMetadata()

    private var artistImageStream: InputStream? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        launch {
            try {
                artistImageStream = if (isAllowedToDownloadImage) {
                    val deezerArtist = deezerService.artist(model.name)
                    val imageUrl = deezerArtist?.getImageUrl(preferredImageSize)
                    if (imageUrl != null) {
                        fetchImageInputStream(imageUrl)
                    } else {
                        getFallbackAlbumImage()
                    }
                } else {
                    getFallbackAlbumImage()
                }
                if (isActive) {
                    withContext(Dispatchers.Main) {
                        if (artistImageStream != null) {
                            callback.onDataReady(artistImageStream)
                        } else {
                            callback.onLoadFailed(IOException("No image available"))
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    withContext(Dispatchers.Main) {
                        callback.onLoadFailed(e)
                    }
                }
            }
        }
    }

    private suspend fun fetchImageInputStream(imageUrl: String): InputStream? {
        return try {
            val response = httpClient.get(imageUrl)
            if (!response.status.isSuccess()) {
                throw IOException("Request failed with code: ${response.status.value}")
            }
            response.bodyAsChannel().toInputStream()
        } catch (e: Exception) {
            throw IOException("Error during Ktor image request", e)
        }
    }

    private fun getFallbackAlbumImage(): InputStream? {
        return try {
            if (model.imageId <= -1) {
                throw IOException("Artist \"${model.name}\" is not valid since imageId = ${model.imageId}")
            }
            return context.contentResolver.openInputStream(model.imageId.albumCoverUri())
        } catch (e: Exception) {
            throw IOException("Error during fallback image resolution", e)
        }
    }

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun getDataSource(): DataSource =
        if (isAllowedToDownloadImage) DataSource.REMOTE else DataSource.LOCAL

    override fun cleanup() {
        cancel(null)
        artistImageStream?.closeQuietly()
    }

    override fun cancel() {
        cancel(null)
    }

    companion object {
        const val TAG = "ArtistImageFetcher"
    }
}