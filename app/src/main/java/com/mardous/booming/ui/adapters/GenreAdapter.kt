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

package com.mardous.booming.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.mardous.booming.coil.DEFAULT_GENRE_IMAGE
import com.mardous.booming.data.model.Genre
import com.mardous.booming.extensions.loadPaletteImage
import com.mardous.booming.extensions.media.asNumberOfSongs
import com.mardous.booming.extensions.media.asSectionName
import com.mardous.booming.extensions.resources.hide
import com.mardous.booming.ui.IGenreCallback
import com.mardous.booming.ui.component.base.MediaEntryViewHolder
import me.zhanghai.android.fastscroll.PopupTextProvider
import org.koin.core.component.KoinComponent
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * @author Christians M. A. (mardous)
 */
class GenreAdapter(
    dataSet: List<Genre>,
    @LayoutRes
    private val itemLayoutRes: Int,
    private val callback: IGenreCallback?,
) : RecyclerView.Adapter<GenreAdapter.ViewHolder>(), PopupTextProvider, KoinComponent {

    var dataSet by Delegates.observable(dataSet) { _: KProperty<*>, _: List<Genre>, _: List<Genre> ->
        notifyDataSetChanged()
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genre = dataSet[position]
        holder.title?.text = genre.name
        holder.text?.text = genre.songCount.asNumberOfSongs(holder.itemView.context)
        holder.loadPaletteImage(genre, DEFAULT_GENRE_IMAGE)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun getPopupText(view: View, position: Int): CharSequence {
        val genre = dataSet.getOrNull(position) ?: return ""
        return if (genre.id != -1L) genre.name.asSectionName() else ""
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {

        override fun onClick(view: View) {
            callback?.genreClick(dataSet[layoutPosition])
        }

        init {
            menu?.hide()
        }
    }
}