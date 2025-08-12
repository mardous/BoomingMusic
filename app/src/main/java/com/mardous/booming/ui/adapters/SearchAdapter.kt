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
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mardous.booming.R
import com.mardous.booming.ui.component.base.AbsMultiSelectAdapter
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.extensions.glide.*
import com.mardous.booming.extensions.media.*
import com.mardous.booming.glide.playlistPreview.PlaylistPreview
import com.mardous.booming.ui.component.menu.OnClickMenu
import com.mardous.booming.ui.ISearchCallback
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.Genre
import com.mardous.booming.data.model.Song
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class SearchAdapter(
    private val activity: FragmentActivity,
    dataSet: List<Any>,
    private val callback: ISearchCallback? = null
) : AbsMultiSelectAdapter<SearchAdapter.ViewHolder, Song>(activity, R.menu.menu_media_selection) {

    var dataSet by Delegates.observable(dataSet) { _: KProperty<*>, _: List<Any>, _: List<Any> ->
        notifyDataSetChanged()
    }

    override fun getIdentifier(position: Int): Song? {
        val item = dataSet[position]
        return if (item is Song) item else null
    }

    override fun getName(item: Song): String {
        return item.title
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        callback?.onMultipleItemAction(menuItem, selection)
    }

    override fun getItemViewType(position: Int): Int {
        if (dataSet[position] is Album) return ALBUM
        if (dataSet[position] is Artist) return ARTIST
        if (dataSet[position] is Song) return SONG
        if (dataSet[position] is PlaylistWithSongs) return PLAYLIST
        return if (dataSet[position] is Genre) GENRE else HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == HEADER) {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.sub_header, parent, false), viewType
            )
        }
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false), viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ALBUM -> {
                val album = dataSet[position] as Album
                holder.title?.text = album.name.displayArtistName()
                holder.text?.text = album.albumInfo()
                holder.image?.let {
                    it.transitionName = album.id.toString()
                    Glide.with(it)
                        .asDrawable()
                        .load(album.getAlbumGlideModel())
                        .transition(getDefaultGlideTransition())
                        .albumOptions(album)
                        .into(it)
                }
            }

            ARTIST -> {
                val artist = dataSet[position] as Artist
                holder.title?.text = artist.displayName()
                holder.text?.text = artist.artistInfo(holder.itemView.context)
                holder.image?.let {
                    it.transitionName = if (artist.isAlbumArtist) artist.name else artist.id.toString()
                    Glide.with(it)
                        .asBitmap()
                        .load(artist.getArtistGlideModel())
                        .transition(getDefaultGlideTransition())
                        .artistOptions(artist)
                        .into(it)
                }
            }

            SONG -> {
                val song = dataSet[position] as Song
                val isChecked = isChecked(song)
                holder.isActivated = isChecked
                holder.menu?.isGone = isChecked
                holder.title?.text = song.title
                holder.text?.text = song.songInfo()
                holder.image?.let {
                    Glide.with(it)
                        .asBitmap()
                        .load(song.getSongGlideModel())
                        .transition(getDefaultGlideTransition())
                        .songOptions(song)
                        .into(it)
                }
            }

            PLAYLIST -> {
                val playlist = dataSet[position] as PlaylistWithSongs
                holder.title?.text = playlist.playlistEntity.playlistName
                holder.text?.text = playlist.songCount.songsStr(holder.itemView.context)
                holder.image?.let {
                    Glide.with(it)
                        .asBitmap()
                        .load(PlaylistPreview(playlist))
                        .transition(getDefaultGlideTransition())
                        .playlistOptions()
                        .into(it)
                }
            }

            GENRE -> {
                val genre = dataSet[position] as Genre
                holder.title?.text = genre.name
                holder.text?.text = genre.songCount.songsStr(holder.itemView.context)
            }

            else -> holder.title?.text = dataSet[position].toString()
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    inner class ViewHolder(itemView: View, private val itemViewType: Int) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        
        val image: ImageView? = itemView.findViewById(R.id.image)
        val title: TextView? = itemView.findViewById(R.id.title)
        val text: TextView? = itemView.findViewById(R.id.text)
        val menu: View? = itemView.findViewById(R.id.menu)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            // Ensure long click is enabled
            itemView.isLongClickable = true
        }

        var isActivated: Boolean
            get() = itemView.isActivated
            set(activated) {
                itemView.isActivated = activated
            }

        private val sharedElements: Array<Pair<View, String>>
            get() = arrayOf(image!! to image.transitionName)

        override fun onClick(view: View) {
            val item = dataSet[layoutPosition]
            when (itemViewType) {
                ALBUM -> callback?.albumClick(item as Album, sharedElements)
                ARTIST -> callback?.artistClick(item as Artist, sharedElements)
                SONG -> {
                    val song = item as Song
                    if (isInQuickSelectMode) {
                        toggleChecked(layoutPosition)
                    } else {
                        callback?.songClick(song, dataSet)
                    }
                }
                GENRE -> callback?.genreClick(item as Genre)
                PLAYLIST -> callback?.playlistClick(item as PlaylistWithSongs)
            }
        }

        override fun onLongClick(view: View): Boolean {
            val item = dataSet[layoutPosition]
            return when (itemViewType) {
                SONG -> {
                    // Force toggle checked for songs
                    val result = toggleChecked(layoutPosition)
                    true // Always return true to consume the long click
                }
                else -> menu?.let {
                    it.callOnClick()
                    true
                } ?: false
            }
        }

        private val menuRes = when (itemViewType) {
            SONG -> R.menu.menu_item_song
            ALBUM -> R.menu.menu_item_album
            ARTIST -> R.menu.menu_item_artist
            PLAYLIST -> R.menu.menu_item_playlist
            else -> 0
        }

        init {
            menu?.apply {
                visibility = if (menuRes != 0) {
                    setOnClickListener(object : OnClickMenu() {
                        override val popupMenuRes: Int
                            get() = menuRes

                        override fun onMenuItemClick(item: MenuItem): Boolean {
                            when (val data = dataSet[layoutPosition]) {
                                is Song -> return callback?.songMenuItemClick(data, item) ?: false
                                is Album -> return callback?.albumMenuItemClick(data, item, sharedElements) ?: false
                                is Artist -> return callback?.artistMenuItemClick(data, item, sharedElements) ?: false
                                is PlaylistWithSongs -> return callback?.playlistMenuItemClick(data, item) ?: false
                            }
                            return false
                        }
                    })
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            if (itemViewType == GENRE) {
                image?.isVisible = false
            }
        }
    }

    companion object {
        private const val HEADER = 0
        private const val ALBUM = 1
        private const val ARTIST = 2
        private const val SONG = 3
        private const val PLAYLIST = 4
        private const val GENRE = 5
    }
}