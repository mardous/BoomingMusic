package com.mardous.booming.coil.fetcher

import android.content.SharedPreferences
import android.util.Log
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.kyant.taglib.TagLib
import com.mardous.booming.R
import com.mardous.booming.coil.model.AudioCover
import com.mardous.booming.coil.util.AudioCoverUtils
import com.mardous.booming.data.remote.deezer.DeezerService
import com.mardous.booming.extensions.isAllowedToDownloadMetadata
import com.mardous.booming.extensions.media.asAlbumCoverUri
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.util.ALLOW_ONLINE_ALBUM_COVERS
import okio.IOException
import okio.buffer
import okio.source

class AudioCoverFetcher(
    private val loader: ImageLoader,
    private val options: Options,
    private val deezerService: DeezerService,
    private val cover: AudioCover,
    private val downloadImage: Boolean
) : Fetcher {

    private val contentResolver get() = options.context.contentResolver

    override suspend fun fetch(): FetchResult? {
        val audioCover = cover.getComplete(contentResolver)
        val stream = try {
            if (audioCover.isIgnoreMediaStore) {
                AudioCoverUtils.fallback(audioCover.path, audioCover.isUseFolderArt)
                    ?: contentResolver.openFileDescriptor(audioCover.uri, "r")?.use { fd ->
                        TagLib.getFrontCover(fd.dup().detachFd())?.data?.inputStream()
                    }
            } else {
                contentResolver.openInputStream(audioCover.albumId.asAlbumCoverUri())
            }
        } catch (e: IOException) {
            Log.e("AudioCoverFetcher", "Unable to decode cover image for ${audioCover.path}", e)
            null
        }

        if (stream == null && downloadImage &&
            !cover.artistName.isArtistNameUnknown() &&
            options.context.isAllowedToDownloadMetadata()) {
            val imageUrl = if (cover.isAlbum) {
                deezerService.album(cover.artistName, cover.title).imageUrl
            } else {
                deezerService.track(cover.artistName, cover.title).imageUrl
            }
            if (imageUrl != null) {
                val data = loader.components.map(imageUrl, options)
                val output = loader.components.newFetcher(data, options, loader)
                val (fetcher) = checkNotNull(output) { "no supported fetcher for $imageUrl" }
                return fetcher.fetch()
            }
        }

        if (stream == null) return null
        return SourceFetchResult(
            source = ImageSource(
                source = stream.source().buffer(),
                fileSystem = options.fileSystem,
                metadata = null
            ),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory(
        private val preferences: SharedPreferences,
        private val deezerService: DeezerService
    ) : Fetcher.Factory<AudioCover> {
        override fun create(
            data: AudioCover,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            val resources = options.context.resources
            return AudioCoverFetcher(
                loader = imageLoader,
                options = options,
                deezerService = deezerService,
                cover = data,
                downloadImage = preferences.getBoolean(
                    ALLOW_ONLINE_ALBUM_COVERS,
                    resources.getBoolean(R.bool.default_images_download)
                )
            )
        }
    }
}