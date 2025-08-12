/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.ui.adapters.album

import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.mardous.booming.R
import com.mardous.booming.extensions.glide.albumOptions
import com.mardous.booming.extensions.glide.getAlbumGlideModel
import com.mardous.booming.extensions.glide.getDefaultGlideTransition
import com.mardous.booming.ui.IAlbumCallback
import com.mardous.booming.data.model.Album

/**
 * @author Christians M. A. (mardous)
 */
class SimpleAlbumAdapter(
    activity: FragmentActivity,
    dataSet: List<Album>,
    itemLayoutRes: Int,
    callback: IAlbumCallback? = null
) : AlbumAdapter(activity, dataSet, itemLayoutRes, callback = callback) {

    override fun loadAlbumCover(album: Album, holder: ViewHolder) {
        if (holder.image != null) {
            Glide.with(holder.image)
                .asBitmap()
                .load(album.getAlbumGlideModel())
                .transition(getDefaultGlideTransition())
                .albumOptions(album)
                .into(holder.image)
        }
    }

    override fun getAlbumText(holder: ViewHolder, album: Album): String? {
        return if (album.year > 0) {
            return album.year.toString()
        } else {
            return holder.itemView.resources.getString(R.string.unknown_year)
        }
    }
}