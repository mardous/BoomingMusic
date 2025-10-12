package com.mardous.booming.coil.fetcher

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.kyant.taglib.TagLib
import com.mardous.booming.coil.model.AudioCover
import com.mardous.booming.coil.util.AudioCoverUtils
import com.mardous.booming.extensions.media.asAlbumCoverUri
import okio.buffer
import okio.source

class AudioCoverFetcher(
    private val options: Options,
    private val cover: AudioCover
) : Fetcher {

    private val contentResolver get() = options.context.contentResolver

    override suspend fun fetch(): FetchResult? {
        val audioCover = cover.getComplete(contentResolver)
        val stream = if (audioCover.isIgnoreMediaStore) {
            AudioCoverUtils.fallback(audioCover.path, audioCover.isUseFolderArt)
                ?: contentResolver.openFileDescriptor(audioCover.uri, "r")?.use { fd ->
                    TagLib.getFrontCover(fd.dup().detachFd())?.data?.inputStream()
                }
        } else {
            contentResolver.openInputStream(audioCover.albumId.asAlbumCoverUri())
        } ?: return null

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

    class Factory : Fetcher.Factory<AudioCover> {
        override fun create(
            data: AudioCover,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            return AudioCoverFetcher(options, data)
        }
    }
}