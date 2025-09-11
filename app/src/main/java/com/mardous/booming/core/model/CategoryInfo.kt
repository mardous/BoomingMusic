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

package com.mardous.booming.core.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.mardous.booming.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Keep
@Parcelize
@Serializable
class CategoryInfo(val category: Category, var visible: Boolean) : Parcelable {

    @Serializable
    enum class Category(
        @IdRes val id: Int,
        @StringRes val titleRes: Int,
        @DrawableRes val iconRes: Int
    ) {
        Home(R.id.nav_home, R.string.for_you_label, R.drawable.icon_home),
        Songs(R.id.nav_songs, R.string.songs_label, R.drawable.icon_music),
        Albums(R.id.nav_albums, R.string.albums_label, R.drawable.icon_album),
        Artists(R.id.nav_artists, R.string.artists_label, R.drawable.icon_artist),
        Playlists(R.id.nav_playlists, R.string.playlists_label, R.drawable.icon_playlist),
        Genres(R.id.nav_genres, R.string.genres_label, R.drawable.icon_genre),
        Years(R.id.nav_years, R.string.release_years_label, R.drawable.icon_year),
        Folders(R.id.nav_folders, R.string.folders_label, R.drawable.icon_folder)
    }

    companion object {
        const val MAX_VISIBLE_CATEGORIES = 5
    }
}