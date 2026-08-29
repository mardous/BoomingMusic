package com.mardous.booming.playback.library

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
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
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.mapper.toSongs
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.Genre
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.model.search.SearchQuery
import com.mardous.booming.data.repository.Repository
import com.mardous.booming.extensions.media.albumInfo
import com.mardous.booming.extensions.media.artistInfo
import com.mardous.booming.extensions.media.asNumberOfSongs
import com.mardous.booming.extensions.media.songCountStr
import com.mardous.booming.playback.buildBrowsableMediaItem
import com.mardous.booming.playback.buildPlayableMediaItem
import com.mardous.booming.util.Preferences

class LibraryProvider(private val repository: Repository) {

    suspend fun resolveMediaItems(mediaItems: List<MediaItem>): List<MediaItem> {
        try {
            val resolvedMediaItems = mediaItems
                .filterTo(arrayListOf()) { item -> item.localConfiguration != null }

            // All the MediaItems had their playback settings configured; we can return them as is
            if (resolvedMediaItems.size == mediaItems.size) return resolvedMediaItems

            fun List<Song>.toPlayableMediaItems() = map { buildPlayableMediaItem(it) }

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
                                playableSongs.map { song -> buildPlayableMediaItem(song) }
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
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): MediaItemsWithStartPosition? {
        try {
            val mediaItem = mediaItems.single()
            if (!mediaItem.requestMetadata.searchQuery.isNullOrEmpty()) {
                val songs = searchWithRequestMetadata(mediaItem.requestMetadata)
                if (songs.isNotEmpty()) {
                    return MediaItemsWithStartPosition(
                        songs.map { buildPlayableMediaItem(it) },
                        startIndex,
                        startPositionMs
                    )
                }
                return null
            }
            val path = MediaIDs.splitPath(mediaItem.mediaId)
            return when (path.firstOrNull()) {
                MediaIDs.SONGS -> {
                    val id = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val allSongs = repository.allSongs()
                    MediaItemsWithStartPosition(
                        allSongs.map { buildPlayableMediaItem(it) },
                        allSongs.indexOfFirst { it.id == id }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.ALBUMS -> {
                    val albumId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val album = repository.albumById(albumId)
                    MediaItemsWithStartPosition(
                        album.songs.map { buildPlayableMediaItem(it) },
                        album.songs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.ARTISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val artistId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val artistSongs = repository.artistById(artistId).sortedSongs
                    MediaItemsWithStartPosition(
                        artistSongs.map { buildPlayableMediaItem(it) },
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
                        albumArtistSongs.map { buildPlayableMediaItem(it) },
                        albumArtistSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.PLAYLISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val playlistId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val playlist = repository.playlistWithSongs(playlistId)
                    MediaItemsWithStartPosition(
                        playlist.songs.toSongs().map { buildPlayableMediaItem(it) },
                        playlist.songs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.GENRES -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val genreId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val songsByGenre = repository.songsByGenre(genreId)
                    MediaItemsWithStartPosition(
                        songsByGenre.map { buildPlayableMediaItem(it) },
                        songsByGenre.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.TOP_TRACKS -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val playCountSongs = repository.playCountSongs()
                    MediaItemsWithStartPosition(
                        playCountSongs.map { buildPlayableMediaItem(it) },
                        playCountSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.RECENT_SONGS -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val historySongs = repository.historySongs()
                    MediaItemsWithStartPosition(
                        historySongs.map { buildPlayableMediaItem(it) },
                        historySongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                        C.TIME_UNSET
                    )
                }

                MediaIDs.FAVORITES -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val favoriteSongs = repository.favoriteSongs()
                    MediaItemsWithStartPosition(
                        favoriteSongs.map { buildPlayableMediaItem(it) },
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
                        id = MediaIDs.getPathId(
                            parentId,
                            playlistWithSongs.playlistEntity.playListId
                        ),
                        title = playlistWithSongs.playlistEntity.playlistName,
                        subtitle = playlistWithSongs.songCount.asNumberOfSongs(context),
                        artworkUri = getImageUri(
                            PLAYLIST_COVER_PATH,
                            playlistWithSongs.playlistEntity.playListId
                        )
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

    suspend fun getItem(itemId: String): MediaItem {
        val songId = itemId.toLongOrNull() ?: return MediaItem.EMPTY
        val resolvedSong = repository.songById(songId)
        return if (resolvedSong != Song.emptySong) {
            buildPlayableMediaItem(resolvedSong)
        } else MediaItem.EMPTY
    }

    suspend fun getSearchResult(query: String, page: Int, pageSize: Int): List<MediaItem> {
        val songs = repository.searchSongs(query)
        val maxPageSize = pageSize.coerceAtMost(200)
        return if (songs.isEmpty()) emptyList() else
            songs.asSequence().drop(page * maxPageSize).take(maxPageSize)
                .mapTo(mutableListOf()) { buildPlayableMediaItem(it) }
    }

    @Suppress("DEPRECATION")
    private suspend fun searchWithRequestMetadata(
        requestMetadata: MediaItem.RequestMetadata
    ): List<Song> {
        val query = requestMetadata.searchQuery ?: return emptyList()
        val extras = requestMetadata.extras ?: Bundle.EMPTY
        return when (extras.getString(MediaStore.EXTRA_MEDIA_FOCUS)) {
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                val artistQuery = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST) ?: query
                repository.search(SearchQuery(SearchQuery.FilterMode.Artists, artistQuery), null)
                    .filterIsInstance<Artist>()
                    .firstOrNull()?.sortedSongs ?: repository.searchSongs(query)
            }

            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                val albumQuery = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM) ?: query
                repository.search(SearchQuery(SearchQuery.FilterMode.Albums, albumQuery), null)
                    .filterIsInstance<Album>()
                    .firstOrNull()?.songs ?: repository.searchSongs(query)
            }

            MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE -> {
                val genreQuery = extras.getString(MediaStore.EXTRA_MEDIA_GENRE) ?: query
                repository.search(SearchQuery(SearchQuery.FilterMode.Genres, genreQuery), null)
                    .filterIsInstance<Genre>()
                    .firstOrNull()?.let { repository.songsByGenre(it.id) }
                    ?: repository.searchSongs(query)
            }

            MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE -> {
                val playlistQuery = extras.getString(MediaStore.EXTRA_MEDIA_PLAYLIST) ?: query
                repository.search(SearchQuery(SearchQuery.FilterMode.Playlists, playlistQuery), null)
                    .filterIsInstance<PlaylistWithSongs>()
                    .firstOrNull()?.songs?.toSongs() ?: repository.searchSongs(query)
            }

            MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> {
                val title = extras.getString(MediaStore.EXTRA_MEDIA_TITLE)
                val artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                if (!title.isNullOrEmpty() && !artist.isNullOrEmpty()) {
                    repository.searchSongs(title).filter {
                        it.artistName.contains(artist, ignoreCase = true) ||
                                it.albumArtistName?.contains(artist, ignoreCase = true) == true
                    }.ifEmpty { repository.searchSongs("$title $artist") }
                } else {
                    repository.searchSongs(query)
                }
            }

            else -> repository.searchSongs(query)
        }
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

                    else -> {
                        MediaItem.EMPTY
                    }
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
                val parentPath = if (childId.isNullOrEmpty()) parentId else MediaIDs.getPathId(parentId, childId)
                buildPlayableMediaItem(song, MediaIDs.getPathId(parentPath, song.id))
            }
}
