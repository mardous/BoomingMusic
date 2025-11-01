package com.mardous.booming.playback.library

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mardous.booming.R
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

    suspend fun getChildren(
        context: Context,
        parentId: String
    ): List<MediaItem> {
        return if (MediaIDs.isPath(parentId)) {
            getMediaItemsFromPath(parentId)
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
                                .setArtworkUri(album.albumCover)
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
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(genre.name)
                                .setSubtitle(genre.songCount.asNumberOfSongs(context))
                                .build()
                        )
                        .build()
                }
            }

            else -> getPlayableMediaItems(parentId)
        }
    }

    private suspend fun getRootChildren(context: Context): List<MediaItem> {
        val resources = context.resources
        val mediaItems: MutableList<MediaItem> = ArrayList()
        val libraryCategories = Preferences.libraryCategories
        libraryCategories.forEach { categoryInfo ->
            if (categoryInfo.visible) {
                when (categoryInfo.category) {
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

    private suspend fun getMediaItemsFromPath(path: String): List<MediaItem> {
        val pathParentId = MediaIDs.getParentId(path)
        val pathChildId = MediaIDs.getChildId(path)
        return if (pathParentId == null || pathChildId == null) {
            listOf(MediaItem.EMPTY)
        } else {
            getPlayableMediaItems(pathParentId, pathChildId)
        }
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
                MediaItem.Builder()
                    .setUri(song.uri)
                    .setMediaId(song.id.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsPlayable(true)
                            .setArtworkUri(song.albumCoverUri) // IMPORTANT: must use the actual album cover Uri
                            .setTitle(song.title)
                            .setArtist(song.artistName)
                            .setAlbumTitle(song.albumName)
                            .setAlbumArtist(song.albumArtistName)
                            .setGenre(song.genreName)
                            .setTrackNumber(song.trackNumber)
                            .setReleaseYear(song.year)
                            .setDurationMs(song.duration.coerceAtLeast(0))
                            .build()
                    )
                    .build()
            }
}