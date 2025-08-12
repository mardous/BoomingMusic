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

package com.mardous.booming.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.mardous.booming.R
import com.mardous.booming.ui.component.base.AbsMultiSelectAdapter
import com.mardous.booming.ui.component.base.MediaEntryViewHolder
import com.mardous.booming.extensions.isActivated
import com.mardous.booming.extensions.glide.getSongGlideModel
import com.mardous.booming.extensions.glide.songOptions
import com.mardous.booming.extensions.resources.useAsIcon
import com.mardous.booming.ui.component.menu.OnClickMenu
import com.mardous.booming.ui.IFileCallback
import com.mardous.booming.data.model.Folder
import com.mardous.booming.data.model.Song
import com.mardous.booming.core.model.filesystem.FileSystemItem

class FileAdapter(
    activity: FragmentActivity,
    files: List<FileSystemItem>,
    private val itemLayoutRes: Int,
    private val callback: IFileCallback?,
) : AbsMultiSelectAdapter<FileAdapter.ViewHolder, FileSystemItem>(activity, R.menu.menu_media_selection) {

    var files: List<FileSystemItem> = files
        private set

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = if (viewType == VIEW_TYPE_FOLDER || viewType == VIEW_TYPE_SONG) {
            LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_list_single_row, parent, false)
        }
        return ViewHolder(itemView, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        val isChecked = isChecked(file)
        holder.isActivated = isChecked
        holder.menu?.isGone = isChecked || getItemViewType(position) == VIEW_TYPE_OTHER
        holder.title?.text = file.fileName
        holder.text?.text = file.getFileDescription(holder.itemView.context)
        if (getItemViewType(position) == VIEW_TYPE_SONG) {
            if (holder.image == null) return
            val song = file as? Song ?: return
            Glide.with(holder.image)
                .asBitmap()
                .load(song.getSongGlideModel())
                .songOptions(song)
                .into(holder.image)
        } else {
            holder.image?.setImageDrawable(file.getFileIcon(holder.itemView.context))
        }
    }

    override fun getItemCount(): Int = files.size

    override fun getItemId(position: Int): Long = files[position].fileId

    override fun getItemViewType(position: Int): Int {
        return when(files[position]) {
            is Song -> VIEW_TYPE_SONG
            is Folder -> VIEW_TYPE_FOLDER
            else -> VIEW_TYPE_OTHER
        }
    }

    override fun getIdentifier(position: Int): FileSystemItem? {
        if (getItemViewType(position) == VIEW_TYPE_OTHER) {
            return null
        }
        return files[position]
    }

    override fun getName(item: FileSystemItem): String? = item.fileName

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<FileSystemItem>) {
        callback?.filesMenuItemClick(selection, menuItem)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(files: List<FileSystemItem>) {
        this.files = files
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View, itemViewType: Int) : MediaEntryViewHolder(itemView) {
        private val currentFile: FileSystemItem
            get() = files[layoutPosition]

        private val filePopupMenuResource: Int
            get() = when (itemViewType) {
                VIEW_TYPE_SONG -> R.menu.menu_item_song
                VIEW_TYPE_FOLDER -> R.menu.menu_item_directory
                else -> 0
            }

        override fun onClick(view: View) {
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
            } else {
                callback?.fileClick(currentFile)
            }
        }

        override fun onLongClick(view: View): Boolean {
            toggleChecked(layoutPosition)
            return true
        }

        init {
            if (itemViewType != VIEW_TYPE_SONG) {
                image?.useAsIcon()
            }
            if (itemViewType != VIEW_TYPE_OTHER) {
                menu?.setOnClickListener(object : OnClickMenu() {
                    override val popupMenuRes: Int
                        get() = filePopupMenuResource

                    override fun onMenuItemClick(item: MenuItem): Boolean {
                        return callback?.fileMenuItemClick(currentFile, item) == true
                    }
                })
            }
        }
    }

    companion object {
        const val VIEW_TYPE_OTHER = 0
        const val VIEW_TYPE_FOLDER = 1
        const val VIEW_TYPE_SONG = 2
    }
}