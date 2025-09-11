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

package com.mardous.booming.data.model

import androidx.annotation.StringRes
import com.mardous.booming.R

enum class ContentType(@StringRes internal val titleRes: Int) {
    TopArtists(R.string.top_artists),
    RecentArtists(R.string.recent_artists),
    TopAlbums(R.string.top_albums),
    RecentAlbums(R.string.recent_albums),
    TopTracks(R.string.top_tracks_label),
    History(R.string.history_label),
    RecentSongs(R.string.last_added_label),
    Favorites(R.string.favorites_label),
    NotRecentlyPlayed(R.string.not_recently_played);

    val isPlayableContent: Boolean
        get() = this == Favorites || this == History || this == TopTracks || this == RecentSongs || this == NotRecentlyPlayed

    val isHistoryContent: Boolean
        get() = this == History

    val isFavoriteContent: Boolean
        get() = this == Favorites

    val isRecentContent: Boolean
        get() = this == RecentSongs || this == RecentAlbums || this == RecentArtists

    val isSearchableContent: Boolean
        get() = isFavoriteContent || isRecentContent
}