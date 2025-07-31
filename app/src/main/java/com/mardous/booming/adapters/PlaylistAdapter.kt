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

package com.mardous.booming.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import com.bumptech.glide.Glide
import com.mardous.booming.R
import com.mardous.booming.adapters.base.AbsMultiSelectAdapter
import com.mardous.booming.adapters.extension.isValidPosition
import com.mardous.booming.database.PlaylistWithSongs
import com.mardous.booming.extensions.getTintedDrawable
import com.mardous.booming.extensions.glide.getDefaultGlideTransition
import com.mardous.booming.extensions.glide.playlistOptions
import com.mardous.booming.extensions.media.isFavorites
import com.mardous.booming.extensions.media.songsStr
import com.mardous.booming.extensions.resources.controlColorNormal
import com.mardous.booming.extensions.resources.useAsIcon
import com.mardous.booming.glide.playlistPreview.PlaylistPreview
import com.mardous.booming.helper.menu.OnClickMenu
import com.mardous.booming.interfaces.IPlaylistCallback
import kotlin.properties.Delegates
import kotlin.reflect.KProperty


/**
 * @author Christians M. A. (mardous)
 */
class PlaylistAdapter(
    activity: AppCompatActivity,
    dataSet: List<PlaylistWithSongs>,
    @LayoutRes
    private val itemLayoutRes: Int,
    private val callback: IPlaylistCallback? = null
) : AbsMultiSelectAdapter<PlaylistAdapter.ViewHolder, PlaylistWithSongs>(activity, R.menu.menu_playlists_selection) {

    var dataSet by Delegates.observable(dataSet) { _: KProperty<*>, _: List<Any>, _: List<Any> ->
        notifyDataSetChanged()
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false).let { itemView ->
            createViewHolder(itemView)
        }
    }

    private fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = dataSet[position]
        val isChecked = isChecked(playlist)
        holder.itemView.isActivated = isChecked
        holder.menu?.isGone = isChecked
        holder.title?.text = playlist.playlistEntity.playlistName
        if (holder.text != null) {
            holder.text.text = playlist.songCount.songsStr(holder.itemView.context)
        }
        if (holder.imageContainer != null) {
            holder.imageContainer.transitionName = playlist.playlistEntity.playlistName
        } else {
            holder.image?.transitionName = playlist.playlistEntity.playlistName
        }
        if (holder.image != null) {
            if (itemLayoutRes == R.layout.item_playlist) {
                Glide.with(holder.image)
                    .asBitmap()
                    .load(PlaylistPreview(playlist))
                    .playlistOptions()
                    .transition(getDefaultGlideTransition())
                    .into(holder.image)
            } else {
                holder.image.setImageDrawable(getIconRes(holder))
            }
        }
    }

    private fun getIconRes(holder: ViewHolder): Drawable {
        return holder.itemView.context.getTintedDrawable(
            R.drawable.ic_playlist_play_24dp,
            holder.itemView.context.controlColorNormal()
        )!!
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].playlistEntity.playListId
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): PlaylistWithSongs {
        return dataSet[position]
    }

    override fun getName(item: PlaylistWithSongs): String {
        return item.playlistEntity.playlistName
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<PlaylistWithSongs>) {
        callback?.playlistsMenuItemClick(selection, menuItem)
    }

    inner class ViewHolder(itemView: View) : com.mardous.booming.adapters.base.MediaEntryViewHolder(itemView) {

        init {
            if (itemLayoutRes == R.layout.item_list)
                image?.useAsIcon()

            menu?.setOnClickListener(object : OnClickMenu() {
                override val popupMenuRes: Int
                    get() = R.menu.menu_item_playlist

                override fun onPreparePopup(menu: Menu) {
                    super.onPreparePopup(menu)
                    if (playlist.playlistEntity.isFavorites(itemView.context)) {
                        menu.removeItem(R.id.action_edit_playlist)
                    }
                }

                override fun onMenuItemClick(item: MenuItem): Boolean =
                    callback?.playlistMenuItemClick(playlist, item) ?: false
            })
        }

        private val playlist: PlaylistWithSongs
            get() = dataSet[layoutPosition]

        override fun onClick(view: View) {
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
            } else {
                callback?.playlistClick(playlist)
            }
        }

        override fun onLongClick(view: View): Boolean {
            return isValidPosition && toggleChecked(layoutPosition)
        }
    }
}