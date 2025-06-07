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

package com.mardous.booming.search

import androidx.annotation.IdRes
import com.mardous.booming.R

/**
 * @author Christians M. A. (mardous)
 */
data class SearchQuery(
    val filterMode: FilterMode? = null,
    val searched: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {

    enum class FilterMode(@IdRes val chipId: Int) {
        Songs(R.id.chip_songs),
        Albums(R.id.chip_albums),
        Artists(R.id.chip_artists),
        Genres(R.id.chip_genres),
        Playlists(R.id.chip_playlists)
    }
}