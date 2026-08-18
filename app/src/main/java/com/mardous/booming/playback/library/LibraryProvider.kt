package com.mardous.booming.playback.library

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.mardous.booming.R
import com.mardous.booming.coil.CoverProvider.Companion.ALBUM_ARTIST_COVER_PATH
import com.mardous.booming.coil.CoverProvider.Companion.ALBUM_COVER_PATH
import com.mardous.booming.coil.CoverProvider.Companion.ARTIST_COVER_PATH
import com.mardous.booming.coil.CoverProvider.Companion.GENRE_COVER_PATH
import com.mardous.booming.coil.CoverProvider.Companion.PLAYLIST_COVER_PATH
import com.mardous.booming.coil.CoverProvider.Companion.getImageUri
import com.mardous.booming.core.model.CategoryInfo
import com.mardous.booming.data.mapper.toSongs
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.repository.Repository
import com.mardous.booming.extensions.media.albumInfo
import com.mardous.booming.extensions.media.artistInfo
import com.mardous.booming.extensions.media.asNumberOfSongs
import com.mardous.booming.extensions.media.songCountStr
import com.mardous.booming.playback.buildBrowsableMediaItem
import com.mardous.booming.playback.buildPlayableMediaItem
import com.mardous.booming.util.Preferences

class LibraryProvider(private val repository: Repository) {
    @Volatile
    private var lastSearch: Pair<Int, List<MediaItem>>? = null

    fun searchResult(callerUid: Int): List<MediaItem> =
        lastSearch?.takeIf { it.first == callerUid }?.second.orEmpty()

    suspend fun resolveMediaItems(mediaItems: List<MediaItem>): List<MediaItem> {
        try {
            val resolvedMediaItems = mediaItems
                .filterTo(arrayListOf()) { item -> item.localConfiguration != null }

            // All the MediaItems had their playback settings configured; we can return them as is
            if (resolvedMediaItems.size == mediaItems.size) return resolvedMediaItems

            fun List<Song>.toPlayableMediaItems() = map { it.toPlayableMediaItem() }

            // We resolve MediaItems from the repository based on their IDs
            val (songs, missingMediaItems) = (mediaItems - resolvedMediaItems.toSet())
                .let { invalidItems ->
                    repository.songsByMediaItems(invalidItems, ignoreBlacklist = false)
                }

            resolvedMediaItems.addAll(songs.toPlayableMediaItems())

            // We must try to resolve any MediaItems that could not be found in the repository:
            if (missingMediaItems.isNotEmpty()) {
                missingMediaItems.forEach { missingMediaItem ->
                    getPlayableSongs(missingMediaItem.mediaId).let { playableSongs ->
                        if (playableSongs.isNotEmpty()) {
                            resolvedMediaItems.addAll(
                                playableSongs.map { song -> song.toPlayableMediaItem() }
                            )
                        }
                    }
                }
            }
            return resolvedMediaItems
        } catch (e: Exception) {
            Log.e("LibraryProvider", "Could not resolve media items", e)
        }
        return emptyList()
    }

    @OptIn(UnstableApi::class)
    suspend fun tryToResolveComplexMediaItems(
        callerUid: Int,
        mediaItems: List<MediaItem>
    ): MediaItemsWithStartPosition? {
        try {
            val mediaItem = mediaItems.single()
            val path = MediaIDs.splitPath(mediaItem.mediaId)
            return when (path.firstOrNull()) {
                SEARCH -> {
                    val id = path.getOrNull(1)
                    val results = searchResult(callerUid)
                    if (id == null || results.isEmpty()) return null
                    val transformedMediaItems = results.map { it.buildUpon().setMediaId(id).build() }
                    MediaItemsWithStartPosition(
                        transformedMediaItems,
                        transformedMediaItems.indexOfFirst { it.mediaId == id }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.SONGS -> {
                    val id = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val allSongs = repository.allSongs()
                    MediaItemsWithStartPosition(
                        allSongs.map { it.toPlayableMediaItem() },
                        allSongs.indexOfFirst { it.id == id }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.ALBUMS -> {
                    val albumId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val album = repository.albumById(albumId)
                    MediaItemsWithStartPosition(
                        album.songs.map { it.toPlayableMediaItem() },
                        album.songs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.ARTISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val artistId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val artistSongs = repository.artistById(artistId).sortedSongs
                    MediaItemsWithStartPosition(
                        artistSongs.map { it.toPlayableMediaItem() },
                        artistSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.ALBUM_ARTISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val albumArtistName = path.getOrNull(1) ?: return null
                    val albumArtistSongs =
                        repository.albumArtistByName(albumArtistName).sortedSongs
                    MediaItemsWithStartPosition(
                        albumArtistSongs.map { it.toPlayableMediaItem() },
                        albumArtistSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.PLAYLISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val playlistId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val playlist = repository.playlistWithSongs(playlistId)
                    MediaItemsWithStartPosition(
                        playlist.songs.toSongs().map { it.toPlayableMediaItem() },
                        playlist.songs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.GENRES -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val genreId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val songsByGenre = repository.songsByGenre(genreId)
                    MediaItemsWithStartPosition(
                        songsByGenre.map { it.toPlayableMediaItem() },
                        songsByGenre.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.TOP_TRACKS -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val playCountSongs = repository.playCountSongs()
                    MediaItemsWithStartPosition(
                        playCountSongs.map { it.toPlayableMediaItem() },
                        playCountSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.RECENT_SONGS -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val historySongs = repository.historySongs()
                    MediaItemsWithStartPosition(
                        historySongs.map { it.toPlayableMediaItem() },
                        historySongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.FAVORITES -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val favoriteSongs = repository.favoriteSongs()
                    MediaItemsWithStartPosition(
                        favoriteSongs.map { it.toPlayableMediaItem() },
                        favoriteSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                else -> null
            }
        } catch (e: Exception) {
            Log.e("LibraryProvider", "Could not resolve media items", e)
        }
        return null
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
                    buildBrowsableMediaItem(
                        type = MediaMetadata.MEDIA_TYPE_ALBUM,
                        id = MediaIDs.getPathId(parentId, album.id),
                        title = album.name,
                        subtitle = album.albumInfo(),
                        artworkUri = getImageUri(ALBUM_COVER_PATH, album.id)
                    )
                }
            }

            MediaIDs.ALBUM_ARTISTS -> {
                repository.allAlbumArtists().map { albumArtist ->
                    buildBrowsableMediaItem(
                        type = MediaMetadata.MEDIA_TYPE_ARTIST,
                        id = MediaIDs.getPathId(parentId, albumArtist.name),
                        title = albumArtist.name,
                        subtitle = albumArtist.artistInfo(context),
                        artworkUri = getImageUri(ALBUM_ARTIST_COVER_PATH, albumArtist.name)
                    )
                }
            }

            MediaIDs.ARTISTS -> {
                repository.allArtists().map { artist ->
                    buildBrowsableMediaItem(
                        type = MediaMetadata.MEDIA_TYPE_ARTIST,
                        id = MediaIDs.getPathId(parentId, artist.id),
                        title = artist.name,
                        subtitle = artist.artistInfo(context),
                        artworkUri = getImageUri(ARTIST_COVER_PATH, artist.id)
                    )
                }
            }

            MediaIDs.PLAYLISTS -> {
                repository.playlistsWithSongs(sorted = true).map { playlistWithSongs ->
                    buildBrowsableMediaItem(
                        type = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        id = MediaIDs.getPathId(parentId, playlistWithSongs.playlistEntity.playListId),
                        title = playlistWithSongs.playlistEntity.playlistName,
                        subtitle = playlistWithSongs.songCount.asNumberOfSongs(context),
                        artworkUri = getImageUri(PLAYLIST_COVER_PATH, playlistWithSongs.playlistEntity.playListId)
                    )
                }
            }

            MediaIDs.GENRES -> {
                repository.allGenres().map { genre ->
                    buildBrowsableMediaItem(
                        type = MediaMetadata.MEDIA_TYPE_GENRE,
                        id = MediaIDs.getPathId(parentId, genre.id),
                        title = genre.name,
                        subtitle = genre.songCount.asNumberOfSongs(context),
                        artworkUri = getImageUri(GENRE_COVER_PATH, genre.id)
                    )
                }
            }

            // SONGS, TOP_TRACKS, RECENT_SONGS
            else -> getPlayableMediaItems(parentId)
        }
    }

    fun getItem(itemId: String): MediaItem {
        val songId = itemId.toLongOrNull() ?: return MediaItem.EMPTY
        return repository.songById(songId).toPlayableMediaItem()
    }

    suspend fun search(callerUid: Int, query: String): List<MediaItem> {
        val result = repository.searchSongs(query).map { it.toPlayableMediaItem(SEARCH) }
        lastSearch = callerUid to result
        return result
    }

    @OptIn(UnstableApi::class)
    private suspend fun getRootChildren(context: Context): List<MediaItem> {
        val resources = context.resources
        val mediaItems = arrayListOf<MediaItem>()
        Preferences.libraryCategories.forEach { categoryInfo ->
            if (categoryInfo.visible) {
                val mediaItem = when (categoryInfo.category) {
                    CategoryInfo.Category.Songs -> {
                        buildBrowsableMediaItem(
                            type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            id = MediaIDs.SONGS,
                            title = resources.getString(categoryInfo.category.titleRes)
                        )
                    }

                    CategoryInfo.Category.Albums -> {
                        buildBrowsableMediaItem(
                            type = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                            id = MediaIDs.ALBUMS,
                            title = resources.getString(categoryInfo.category.titleRes),
                            showAsGrid = true
                        )
                    }

                    CategoryInfo.Category.Artists -> {
                        if (Preferences.onlyAlbumArtists) {
                            buildBrowsableMediaItem(
                                type = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                                id = MediaIDs.ALBUM_ARTISTS,
                                title = resources.getString(categoryInfo.category.titleRes),
                                showAsGrid = true
                            )
                        } else {
                            buildBrowsableMediaItem(
                                type = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                                id = MediaIDs.ARTISTS,
                                title = resources.getString(categoryInfo.category.titleRes),
                                showAsGrid = true
                            )
                        }
                    }

                    CategoryInfo.Category.Genres -> {
                        buildBrowsableMediaItem(
                            type = MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                            id = MediaIDs.GENRES,
                            title = resources.getString(categoryInfo.category.titleRes),
                            showAsGrid = true
                        )
                    }

                    CategoryInfo.Category.Playlists -> {
                        buildBrowsableMediaItem(
                            type = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                            id = MediaIDs.PLAYLISTS,
                            title = resources.getString(categoryInfo.category.titleRes),
                            showAsGrid = true
                        )
                    }

                    else -> { MediaItem.EMPTY }
                }
                if (mediaItem != MediaItem.EMPTY) mediaItems.add(mediaItem)
            }
        }

        mediaItems.add(
            buildBrowsableMediaItem(
                type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                id = MediaIDs.TOP_TRACKS,
                title = resources.getString(R.string.top_tracks_label),
                subtitle = repository.playCountSongs().songCountStr(context)
            )
        )

        return mediaItems
    }

    // Use id-only DAO queries where possible to avoid loading artwork and metadata for widgets.
    internal suspend fun getPlayableSongIds(parentId: String, limit: Int): List<Long> =
        when (parentId) {
            MediaIDs.RECENT_SONGS -> repository.historySongIds(limit)
            MediaIDs.TOP_TRACKS -> repository.playCountSongIds(limit)
            else -> getPlayableSongs(parentId).asSequence().take(limit).map { it.id }.toList()
        }

    internal suspend fun getPlayableSongs(
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
                song.toPlayableMediaItem(
                    if (childId.isNullOrEmpty()) parentId else MediaIDs.getPathId(parentId, childId)
                )
            }

    private fun Song.toPlayableMediaItem(parent: String? = null) =
        buildPlayableMediaItem(
            song = this,
            id = if (parent.isNullOrEmpty()) this.id.toString() else MediaIDs.getPathId(parent, this.id)
        )

    companion object {
        // Internal ID for search requests
        private const val SEARCH = "SEARCH"
    }
}
