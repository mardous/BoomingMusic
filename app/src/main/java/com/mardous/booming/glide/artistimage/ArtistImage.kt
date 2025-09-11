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

package com.mardous.booming.glide.artistimage

import com.mardous.booming.data.model.Artist

class ArtistImage(artist: Artist) {

    val name = artist.name
    val imageId = artist.safeGetFirstAlbum().id

    override fun toString(): String {
        return "ArtistImage(name='$name', imageId=$imageId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArtistImage) return false

        if (name != other.name) return false
        if (imageId != other.imageId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + imageId.hashCode()
        return result
    }
}