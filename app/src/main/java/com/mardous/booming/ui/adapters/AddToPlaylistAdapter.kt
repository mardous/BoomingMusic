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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.mardous.booming.R
import com.mardous.booming.ui.component.base.MediaEntryViewHolder
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.extensions.glide.getDefaultGlideTransition
import com.mardous.booming.extensions.glide.playlistOptions
import com.mardous.booming.extensions.resources.hide
import com.mardous.booming.extensions.resources.show
import com.mardous.booming.glide.playlistPreview.PlaylistPreview
import com.mardous.booming.ui.IPlaylistCallback

/**
 * @author Christians M. A. (mardous)
 */
@SuppressLint("NotifyDataSetChanged")
class AddToPlaylistAdapter(
    private val callback: IAddToPlaylistCallback? = null
) : RecyclerView.Adapter<AddToPlaylistAdapter.ViewHolder>() {

    interface IAddToPlaylistCallback : IPlaylistCallback {
        fun newPlaylistClick()

        override fun playlistMenuItemClick(
            playlist: PlaylistWithSongs,
            menuItem: MenuItem
        ): Boolean = false

        override fun playlistsMenuItemClick(
            playlists: List<PlaylistWithSongs>,
            menuItem: MenuItem
        ) {
        }
    }

    private var addingToPlaylistId: Long = -1
    private var dataSet: List<PlaylistWithSongs> = arrayListOf()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.item_dialog_playlist, parent, false).let { itemView ->
            ViewHolder(itemView, viewType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_PLAYLIST) {
            val playlist = dataSet[position - 1]
            holder.title?.text = playlist.playlistEntity.playlistName
            if (holder.image != null) {
                Glide.with(holder.image)
                    .asBitmap()
                    .load(PlaylistPreview(playlist))
                    .playlistOptions()
                    .transition(getDefaultGlideTransition())
                    .into(holder.image)
            }
            if (playlist.playlistEntity.playListId == addingToPlaylistId)
                holder.progressLayout.show(true)
            else holder.progressLayout.hide(true)
        } else {
            holder.title?.setText(R.string.new_playlist_title)
            holder.image?.setImageResource(R.drawable.ic_playlist_add_24dp)
        }
    }

    override fun getItemId(position: Int): Long {
        if (getItemViewType(position) == TYPE_PLAYLIST) {
            return dataSet[position - 1].playlistEntity.playListId
        }
        return -1
    }

    override fun getItemCount(): Int {
        return dataSet.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_OFFSET else TYPE_PLAYLIST
    }

    fun adding(playlistId: Long) {
        this.addingToPlaylistId = playlistId
        notifyDataSetChanged()
    }

    fun data(dataSet: List<PlaylistWithSongs>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View, viewType: Int) : MediaEntryViewHolder(itemView) {
        val progressLayout: FrameLayout = itemView.findViewById(R.id.progressLayout)
        val progressIndicator: CircularProgressIndicator = itemView.findViewById(R.id.progressIndicator)

        private val playlist: PlaylistWithSongs
            get() = dataSet[layoutPosition - 1]

        init {
            if (viewType == TYPE_OFFSET) {
                image?.scaleType = ImageView.ScaleType.CENTER
            }
        }

        override fun onClick(view: View) {
            if (addingToPlaylistId != -1L)
                return

            if (itemViewType == TYPE_PLAYLIST) {
                callback?.playlistClick(playlist)
            } else {
                callback?.newPlaylistClick()
            }
        }
    }

    companion object {
        private const val TYPE_OFFSET = 0
        private const val TYPE_PLAYLIST = 1
    }
}