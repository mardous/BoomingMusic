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

package com.mardous.booming.core.androidauto

import android.content.Context
import android.content.res.Resources
import android.support.v4.media.MediaBrowserCompat
import com.mardous.booming.R
import com.mardous.booming.core.model.CategoryInfo
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.mapper.fromHistoryToSongs
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.media.*
import com.mardous.booming.service.queue.QueueManager
import com.mardous.booming.util.Preferences

/**
 * Created by Beesham Sarendranauth (Beesham)
 */
class AutoMusicProvider(
    private val mContext: Context,
    private val repository: Repository,
    private val queueManager: QueueManager
) {
    suspend fun getChildren(mediaId: String?, resources: Resources): List<MediaBrowserCompat.MediaItem> {
        val mediaItems: MutableList<MediaBrowserCompat.MediaItem> = ArrayList()
        when (mediaId) {
            AutoMediaIDHelper.MEDIA_ID_ROOT -> {
                mediaItems.addAll(getRootChildren(resources))
            }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_PLAYLIST ->
                for (playlist in repository.playlistsWithSongs(sorted = true)) {
                    mediaItems.add(
                        AutoMediaItem.with(mContext)
                            .path(mediaId, playlist.playlistEntity.playListId)
                            .icon(R.drawable.ic_play_24dp)
                            .title(playlist.playlistEntity.playlistName)
                            .subTitle(playlist.songCount.songsStr(mContext))
                            .asPlayable()
                            .build()
                    )
                }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM ->
                for (album in repository.allAlbums()) {
                    mediaItems.add(
                        AutoMediaItem.with(mContext)
                            .path(mediaId, album.id)
                            .title(album.name)
                            .subTitle(album.displayArtistName())
                            .icon(album.id.albumCoverUri())
                            .asPlayable()
                            .build()
                    )
                }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST ->
                for (artist in repository.allArtists()) {
                    mediaItems.add(
                        AutoMediaItem.with(mContext)
                            .asPlayable()
                            .path(mediaId, artist.id)
                            .title(artist.displayName())
                            .build()
                    )
                }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM_ARTIST ->
                for (artist in repository.allAlbumArtists()) {
                    mediaItems.add(
                        AutoMediaItem.with(mContext)
                            .asPlayable()
                            // we just pass album id here as we don't have album artist id's
                            .path(mediaId, artist.safeGetFirstAlbum().id)
                            .title(artist.name)
                            .build()
                    )
                }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE ->
                for (genre in repository.allGenres()) {
                    mediaItems.add(
                        AutoMediaItem.with(mContext)
                            .asPlayable()
                            .path(mediaId, genre.id)
                            .title(genre.name)
                            .build()
                    )
                }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_QUEUE ->
                for (song in queueManager.playingQueue) {
                    mediaItems.add(
                        AutoMediaItem.with(mContext)
                            .asPlayable()
                            .path(mediaId, song.id)
                            .title(song.title)
                            .subTitle(song.displayArtistName())
                            .icon(song.id.albumCoverUri())
                            .build()
                    )
                }

            else -> {
                getPlaylistChildren(mediaId, mediaItems)
            }
        }
        return mediaItems
    }

    private suspend fun getPlaylistChildren(mediaId: String?, mediaItems: MutableList<MediaBrowserCompat.MediaItem>) {
        val songs = when (mediaId) {
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS -> repository.topPlayedSongs()
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY -> repository.historySongs().fromHistoryToSongs()
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_NOT_RECENTLY_PLAYED -> repository.notRecentlyPlayedSongs()
                .take(8)

            else -> {
                emptyList()
            }
        }
        songs.forEach { song ->
            mediaItems.add(getPlayableSong(mediaId, song))
        }
    }

    private suspend fun getRootChildren(resources: Resources): List<MediaBrowserCompat.MediaItem> {
        val mediaItems: MutableList<MediaBrowserCompat.MediaItem> = ArrayList()
        val libraryCategories = Preferences.libraryCategories
        libraryCategories.forEach {
            if (it.visible) {
                when (it.category) {
                    CategoryInfo.Category.Albums -> {
                        mediaItems.add(
                            AutoMediaItem.with(mContext)
                                .asBrowsable()
                                .path(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM)
                                .gridLayout(true)
                                .icon(R.drawable.ic_album_24dp)
                                .title(resources.getString(R.string.albums_label)).build()
                        )
                    }

                    CategoryInfo.Category.Artists -> {
                        if (Preferences.onlyAlbumArtists) {
                            mediaItems.add(
                                AutoMediaItem.with(mContext)
                                    .asBrowsable()
                                    .path(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM_ARTIST)
                                    .icon(R.drawable.ic_artist_24dp)
                                    .title(resources.getString(R.string.album_artist)).build()
                            )
                        } else {
                            mediaItems.add(
                                AutoMediaItem.with(mContext)
                                    .asBrowsable()
                                    .path(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST)
                                    .icon(R.drawable.ic_artist_24dp)
                                    .title(resources.getString(R.string.artists_label)).build()
                            )
                        }
                    }

                    CategoryInfo.Category.Genres -> {
                        mediaItems.add(
                            AutoMediaItem.with(mContext)
                                .asBrowsable()
                                .path(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE)
                                .icon(R.drawable.ic_radio_24dp)
                                .title(resources.getString(R.string.genres_label)).build()
                        )
                    }

                    CategoryInfo.Category.Playlists -> {
                        mediaItems.add(
                            AutoMediaItem.with(mContext)
                                .asBrowsable()
                                .path(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_PLAYLIST)
                                .icon(R.drawable.ic_playlist_play_24dp)
                                .title(resources.getString(R.string.playlists_label)).build()
                        )
                    }

                    else -> {
                    }
                }
            }
        }
        mediaItems.add(
            AutoMediaItem.with(mContext)
                .asPlayable()
                .path(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SHUFFLE)
                .icon(R.drawable.ic_shuffle_24dp)
                .title(resources.getString(R.string.shuffle_all_label))
                .subTitle(repository.allSongs().playlistInfo(mContext))
                .build()
        )
        mediaItems.add(
            AutoMediaItem.with(mContext)
                .asBrowsable()
                .path(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_QUEUE)
                .icon(R.drawable.ic_queue_music_24dp)
                .title(resources.getString(R.string.playing_queue_label))
                .subTitle(queueManager.playingQueue.playlistInfo(mContext))
                .asBrowsable().build()
        )
        mediaItems.add(
            AutoMediaItem.with(mContext)
                .asBrowsable()
                .path(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS)
                .icon(R.drawable.ic_trending_up_24dp)
                .title(resources.getString(R.string.top_tracks_label))
                .subTitle(repository.topPlayedSongs().playlistInfo(mContext))
                .asBrowsable().build()
        )
        mediaItems.add(
            AutoMediaItem.with(mContext)
                .asBrowsable()
                .path(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_NOT_RECENTLY_PLAYED)
                .icon(R.drawable.ic_trending_down_24dp)
                .title(resources.getString(R.string.not_recently_played))
                .subTitle(
                    (repository.notRecentlyPlayedSongs().takeIf { it.size > 9 }
                        ?: emptyList()).playlistInfo(mContext)
                )
                .asBrowsable().build()
        )
        mediaItems.add(
            AutoMediaItem.with(mContext)
                .asBrowsable()
                .path(AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY)
                .icon(R.drawable.ic_history_24dp)
                .title(resources.getString(R.string.history_label))
                .subTitle(repository.historySongs().fromHistoryToSongs().playlistInfo(mContext))
                .asBrowsable().build()
        )
        return mediaItems
    }

    private fun getPlayableSong(mediaId: String?, song: Song): MediaBrowserCompat.MediaItem {
        return AutoMediaItem.with(mContext)
            .asPlayable()
            .path(mediaId, song.id)
            .title(song.title)
            .subTitle(song.displayArtistName())
            .icon(song.albumId.albumCoverUri())
            .build()
    }
}