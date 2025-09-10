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

package com.mardous.booming.ui.adapters.song

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.media.durationStr
import com.mardous.booming.extensions.media.trackNumber
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.ui.ISongCallback
import com.mardous.booming.util.sort.SortKeys
import com.mardous.booming.util.sort.SortOrder

class SimpleSongAdapter(
    context: FragmentActivity,
    songs: List<Song>,
    layoutRes: Int,
    sortOrder: SortOrder,
    callback: ISongCallback
) : SongAdapter(context, songs, layoutRes, sortOrder, callback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val fixedTrackNumber = dataSet[position].trackNumber.trackNumber()

        holder.imageText?.text = if (fixedTrackNumber > 0) fixedTrackNumber.toString() else "-"
        holder.time?.text = dataSet[position].duration.durationStr()
    }

    override fun getSongText(song: Song): String {
        when (sortOrder?.value) {
            SortKeys.TRACK_NUMBER -> {
                return buildInfoString(getTrackNumberString(song), song.displayArtistName())
            }
            SortKeys.YEAR -> {
                if (song.year > 0) {
                    return buildInfoString(song.year.toString(), song.displayArtistName())
                }
            }
            SortKeys.ALBUM -> {
                return buildInfoString(getTrackNumberString(song), song.albumName)
            }
        }
        return song.displayArtistName()
    }

    private fun getTrackNumberString(song: Song) =
        song.trackNumber.takeIf { it > 0 }?.trackNumber()?.toString() ?: "-"

    override fun getItemCount(): Int {
        return dataSet.size
    }
}