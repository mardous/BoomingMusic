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

package com.mardous.booming.util.sort

interface SortKeys {
    companion object {
        const val AZ = "az_key"
        const val ARTIST = "artist_key"
        const val ALBUM = "album_key"
        const val TRACK_NUMBER = "track_key"
        const val DURATION = "duration_key"
        const val YEAR = "year_key"
        const val DATE_ADDED = "added_key"
        const val DATE_MODIFIED = "modified_key"
        const val NUMBER_OF_SONGS = "songs_key"
        const val NUMBER_OF_ALBUMS = "albums_key"
    }
}