package com.mardous.booming.playback.library

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mardous.booming.R
import com.mardous.booming.coil.CoverProvider.Companion.ALBUM_ARTIST_COVER_PATH
import com.mardous.booming.coil.CoverProvider.Companion.ALBUM_COVER_PATH
import com.mardous.booming.coil.CoverProvider.Companion.ARTIST_COVER_PATH
import com.mardous.booming.coil.CoverProvider.Companion.GENRE_COVER_PATH
import com.mardous.booming.coil.CoverProvider.Companion.PLAYLIST_COVER_PATH
import com.mardous.booming.coil.CoverProvider.Companion.SONG_COVER_PATH
import com.mardous.booming.coil.CoverProvider.Companion.getImageUri
import com.mardous.booming.core.model.CategoryInfo
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.mapper.toSongs
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.media.albumInfo
import com.mardous.booming.extensions.media.artistInfo
import com.mardous.booming.extensions.media.asNumberOfSongs
import com.mardous.booming.extensions.media.songCountStr
import com.mardous.booming.playback.toMediaItems
import com.mardous.booming.util.Preferences

class LibraryProvider(private val repository: Repository) {

    private val _searchResult = mutableListOf<MediaItem>()
    val searchResult: List<MediaItem> get() = _searchResult

    suspend fun getMediaItemsForPlayback(mediaItems: List<MediaItem>): List<MediaItem> {
        val resolvedMediaItems = mediaItems.filter { item -> item.localConfiguration != null }
            .toMutableList()
        if (resolvedMediaItems.size == mediaItems.size) {
            return resolvedMediaItems
        }
        val (songs, missingMediaItems) = (mediaItems - resolvedMediaItems.toSet()).let { invalidItems ->
            repository.songsByMediaItems(invalidItems)
        }
        if (songs.isNotEmpty()) {
            resolvedMediaItems.addAll(songs.toMediaItems())
        }
        missingMediaItems.forEach {
            getPlayableSongs(it.mediaId).let { playableSongs ->
                if (playableSongs.isNotEmpty()) {
                    resolvedMediaItems.addAll(playableSongs.toMediaItems())
                }
            }
        }
        return resolvedMediaItems
    }

    suspend fun getMediaItemsForAAOSPlayback(
        mediaItems: List<MediaItem>
    ): Pair<List<MediaItem>, Int>? {
        val single = mediaItems.singleOrNull()
        return if (single != null) {
            val path = MediaIDs.splitPath(single.mediaId)
            when (path.firstOrNull()) {
                SEARCH -> {
                    val id = path.getOrNull(1)
                    if (id == null || searchResult.isEmpty()) return null
                    val transformedMediaItems = searchResult.map { it.buildUpon().setMediaId(id).build() }
                    Pair(
                        transformedMediaItems,
                        transformedMediaItems.indexOfFirst { it.mediaId == id }.coerceAtLeast(0)
                    )
                }

                MediaIDs.SONGS -> {
                    val id = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val allSongs = repository.allSongs()
                    Pair(
                        allSongs.map { it.toAutoMediaItem() },
                        allSongs.indexOfFirst { it.id == id }.coerceAtLeast(0)
                    )
                }

                MediaIDs.ALBUMS -> {
                    val albumId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val album = repository.albumById(albumId)
                    Pair(
                        album.songs.map { it.toAutoMediaItem() },
                        album.songs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.ARTISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val artistId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val artistSongs = repository.artistById(artistId).sortedSongs
                    Pair(
                        artistSongs.map { it.toAutoMediaItem() },
                        artistSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.ALBUM_ARTISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val albumArtistName = path.getOrNull(1) ?: return null
                    val albumArtistSongs = repository.albumArtistByName(albumArtistName).sortedSongs
                    Pair(
                        albumArtistSongs.map { it.toAutoMediaItem() },
                        albumArtistSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.PLAYLISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val playlistId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val playlist = repository.playlistWithSongs(playlistId)
                    Pair(
                        playlist.songs.toSongs().map { it.toAutoMediaItem() },
                        playlist.songs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.GENRES -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val genreId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val songsByGenre = repository.songsByGenre(genreId)
                    Pair(
                        songsByGenre.map { it.toAutoMediaItem() },
                        songsByGenre.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.TOP_TRACKS -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val playCountSongs = repository.playCountSongs()
                    Pair(
                        playCountSongs.map { it.toAutoMediaItem() },
                        playCountSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                MediaIDs.RECENT_SONGS -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val historySongs = repository.historySongs()
                    Pair(
                        historySongs.map { it.toAutoMediaItem() },
                        historySongs.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                    )
                }

                else -> null
            }
        } else null
    }

    suspend fun getChildren(
        context: Context,
        parentId: String
    ): List<MediaItem> {
        return if (MediaIDs.isPath(parentId)) {
            val parts = MediaIDs.splitPath(parentId)
            if (parts.size < 2) {
                listOf(MediaItem.EMPTY)
            } else {
                getPlayableMediaItems(parts[0], parts[1])
            }
        } else when (parentId) {
            MediaIDs.ROOT -> {
                getRootChildren(context)
            }

            MediaIDs.ALBUMS -> {
                repository.allAlbums().map { album ->
                    MediaItem.Builder()
                        .setMediaId(MediaIDs.getPathId(parentId, album.id))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                                .setArtworkUri(getImageUri(ALBUM_COVER_PATH, album.id))
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(album.name)
                                .setSubtitle(album.albumInfo())
                                .build()
                        )
                        .build()
                }
            }

            MediaIDs.ALBUM_ARTISTS -> {
                repository.allAlbumArtists().map { albumArtist ->
                    MediaItem.Builder()
                        .setMediaId(MediaIDs.getPathId(parentId, albumArtist.name))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                                .setArtworkUri(getImageUri(ALBUM_ARTIST_COVER_PATH, albumArtist.name))
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(albumArtist.name)
                                .setSubtitle(albumArtist.artistInfo(context))
                                .build()
                        )
                        .build()
                }
            }

            MediaIDs.ARTISTS -> {
                repository.allArtists().map { artist ->
                    MediaItem.Builder()
                        .setMediaId(MediaIDs.getPathId(parentId, artist.id))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                                .setArtworkUri(getImageUri(ARTIST_COVER_PATH, artist.id))
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(artist.name)
                                .setSubtitle(artist.artistInfo(context))
                                .build()
                        )
                        .build()
                }
            }

            MediaIDs.PLAYLISTS -> {
                repository.playlistsWithSongs(sorted = true).map { playlistWithSongs ->
                    MediaItem.Builder()
                        .setMediaId(MediaIDs.getPathId(parentId, playlistWithSongs.playlistEntity.playListId))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                .setArtworkUri(getImageUri(PLAYLIST_COVER_PATH, playlistWithSongs.playlistEntity.playListId))
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(playlistWithSongs.playlistEntity.playlistName)
                                .setSubtitle(playlistWithSongs.songCount.asNumberOfSongs(context))
                                .build()
                        )
                        .build()
                }
            }

            MediaIDs.GENRES -> {
                repository.allGenres().map { genre ->
                    MediaItem.Builder()
                        .setMediaId(MediaIDs.getPathId(parentId, genre.id))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_GENRE)
                                .setArtworkUri(getImageUri(GENRE_COVER_PATH, genre.id))
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(genre.name)
                                .setSubtitle(genre.songCount.asNumberOfSongs(context))
                                .build()
                        )
                        .build()
                }
            }

            // SONGS, TOP_TRACKS, RECENT_SONGS
            else -> getPlayableMediaItems(parentId)
        }
    }

    fun getItem(itemId: String): MediaItem {
        val songId = itemId.toLongOrNull() ?: return MediaItem.EMPTY
        return repository.songById(songId).toAutoMediaItem()
    }

    suspend fun search(query: String): List<MediaItem> {
        _searchResult.clear()
        _searchResult.addAll(repository.searchSongs(query).map { it.toAutoMediaItem(SEARCH) })
        return _searchResult
    }

    private suspend fun getRootChildren(context: Context): List<MediaItem> {
        val resources = context.resources
        val mediaItems: MutableList<MediaItem> = ArrayList()
        val libraryCategories = Preferences.libraryCategories
        libraryCategories.forEach { categoryInfo ->
            if (categoryInfo.visible) {
                when (categoryInfo.category) {
                    CategoryInfo.Category.Songs -> {
                        mediaItems.add(
                            MediaItem.Builder()
                                .setMediaId(MediaIDs.SONGS)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .build()
                                )
                                .build()
                        )
                    }

                    CategoryInfo.Category.Albums -> {
                        mediaItems.add(
                            MediaItem.Builder()
                                .setMediaId(MediaIDs.ALBUMS)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .build()
                                )
                                .build()
                        )
                    }

                    CategoryInfo.Category.Artists -> {
                        if (Preferences.onlyAlbumArtists) {
                            mediaItems.add(
                                MediaItem.Builder()
                                    .setMediaId(MediaIDs.ALBUM_ARTISTS)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setTitle(resources.getString(R.string.album_artists_label))
                                            .build()
                                    )
                                    .build()
                            )
                        } else {
                            mediaItems.add(
                                MediaItem.Builder()
                                    .setMediaId(MediaIDs.ARTISTS)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setTitle(resources.getString(R.string.artists_label))
                                            .build()
                                    )
                                    .build()
                            )
                        }
                    }

                    CategoryInfo.Category.Genres -> {
                        mediaItems.add(
                            MediaItem.Builder()
                                .setMediaId(MediaIDs.GENRES)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_GENRES)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .build()
                                )
                                .build()
                        )
                    }

                    CategoryInfo.Category.Playlists -> {
                        mediaItems.add(
                            MediaItem.Builder()
                                .setMediaId(MediaIDs.PLAYLISTS)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .build()
                                )
                                .build()
                        )
                    }

                    else -> { /*no-op*/ }
                }
            }
        }

        mediaItems.add(
            MediaItem.Builder()
                .setMediaId(MediaIDs.TOP_TRACKS)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle(resources.getString(R.string.top_tracks_label))
                        .setSubtitle(repository.playCountSongs().songCountStr(context))
                        .build()
                )
                .build()
        )

        return mediaItems
    }

    private suspend fun getPlayableSongs(
        parentId: String,
        childId: String? = null
    ): List<Song> {
        return if (childId == null) {
            when (parentId) {
                MediaIDs.SONGS -> repository.allSongs()
                MediaIDs.TOP_TRACKS -> repository.playCountSongs()
                MediaIDs.LAST_ADDED -> repository.recentSongs()
                MediaIDs.RECENT_SONGS -> repository.historySongs()
                MediaIDs.FAVORITES -> repository.favoriteSongs()
                else -> emptyList()
            }
        } else {
            val childIdLong = childId.toLongOrNull()
            if (childIdLong == null) {
                if (parentId == MediaIDs.ALBUM_ARTISTS) {
                    repository.albumArtistByName(childId).sortedSongs
                } else {
                    emptyList()
                }
            } else when (parentId) {
                MediaIDs.ALBUMS -> repository.albumById(childIdLong).songs
                MediaIDs.ARTISTS -> repository.artistById(childIdLong).sortedSongs
                MediaIDs.PLAYLISTS -> repository.playlistWithSongs(childIdLong).songs.toSongs()
                MediaIDs.GENRES -> repository.songsByGenre(childIdLong)
                else -> emptyList()
            }
        }
    }

    private suspend fun getPlayableMediaItems(parentId: String, childId: String? = null) =
        getPlayableSongs(parentId, childId)
            .filterNot { it == Song.emptySong }
            .map { song ->
                song.toAutoMediaItem(
                    if (childId.isNullOrEmpty()) parentId else MediaIDs.getPathId(parentId, childId)
                )
            }

    private fun Song.toAutoMediaItem(parent: String? = null) = MediaItem.Builder()
        .setUri(uri)
        .setMediaId(if (parent.isNullOrEmpty()) id.toString() else MediaIDs.getPathId(parent, id))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setArtworkUri(getImageUri(SONG_COVER_PATH, id))
                .setTitle(title)
                .setArtist(artistName)
                .setAlbumTitle(albumName)
                .setAlbumArtist(albumArtistName)
                .setGenre(genreName)
                .setTrackNumber(trackNumber)
                .setReleaseYear(year)
                .setDurationMs(duration.coerceAtLeast(0))
                .build()
        )
        .build()

    companion object {
        // Internal ID for search requests
        private const val SEARCH = "SEARCH"
    }
}
