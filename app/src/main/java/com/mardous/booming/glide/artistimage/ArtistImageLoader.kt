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
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.mardous.booming.data.remote.deezer.DeezerService
import io.ktor.client.HttpClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.InputStream

class ArtistImageLoader private constructor(
    private val context: Context,
    private val deezerService: DeezerService,
    private val httpClient: HttpClient
) : ModelLoader<ArtistImage, InputStream> {

    override fun buildLoadData(model: ArtistImage, width: Int, height: Int, options: Options): LoadData<InputStream> {
        return LoadData(ObjectKey(model.name), ArtistImageFetcher(context, deezerService, httpClient, model))
    }

    override fun handles(artistImage: ArtistImage): Boolean {
        return true
    }

    class Factory(private val context: Context) : ModelLoaderFactory<ArtistImage, InputStream>, KoinComponent {

        private val httpClient by inject<HttpClient>()
        private val deezerService by inject<DeezerService>()

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ArtistImage, InputStream> {
            return ArtistImageLoader(context, deezerService, httpClient)
        }

        override fun teardown() {}
    }
}