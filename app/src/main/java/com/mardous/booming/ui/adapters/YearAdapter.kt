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
import android.view.*
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.widget.TextViewCompat
import com.bumptech.glide.Glide
import com.mardous.booming.R
import com.mardous.booming.ui.component.base.AbsMultiSelectAdapter
import com.mardous.booming.ui.component.base.MediaEntryViewHolder
import com.mardous.booming.extensions.isActivated
import com.mardous.booming.extensions.setColors
import com.mardous.booming.extensions.glide.asBitmapPalette
import com.mardous.booming.extensions.glide.getSongGlideModel
import com.mardous.booming.extensions.glide.songOptions
import com.mardous.booming.extensions.media.songsStr
import com.mardous.booming.extensions.resources.toColorStateList
import com.mardous.booming.extensions.resources.useAsIcon
import com.mardous.booming.glide.BoomingColoredTarget
import com.mardous.booming.util.color.MediaNotificationProcessor
import com.mardous.booming.ui.component.menu.OnClickMenu
import com.mardous.booming.ui.IYearCallback
import com.mardous.booming.data.model.ReleaseYear
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

@SuppressLint("NotifyDataSetChanged")
class YearAdapter(
    activity: AppCompatActivity,
    dataSet: List<ReleaseYear>,
    @LayoutRes
    private val itemLayoutRes: Int,
    private val callback: IYearCallback?,
) : AbsMultiSelectAdapter<YearAdapter.ViewHolder, ReleaseYear>(activity, R.menu.menu_media_selection) {

    var dataSet: List<ReleaseYear> by Delegates.observable(dataSet) { _: KProperty<*>, _: List<ReleaseYear>, _: List<ReleaseYear> ->
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
        val year = dataSet[position]
        val isChecked = isChecked(year)
        holder.isActivated = isChecked
        holder.menu?.isGone = isChecked
        holder.title?.text = year.name
        if (itemLayoutRes == R.layout.item_grid_gradient) {
            holder.text?.text = year.songCount.toString()
            loadImage(holder, year)
        } else {
            holder.text?.text = year.songCount.songsStr(holder.itemView.context)
            holder.image?.setImageResource(R.drawable.ic_event_24dp)
        }
    }

    private fun loadImage(holder: ViewHolder, year: ReleaseYear) {
        if (holder.image != null) {
            Glide.with(holder.image)
                .asBitmapPalette()
                .load(year.safeGetFirstSong().getSongGlideModel())
                .songOptions(year.safeGetFirstSong())
                .into(object : BoomingColoredTarget(holder.image) {
                    override fun onColorReady(colors: MediaNotificationProcessor) {
                        holder.setColors(colors)
                        if (holder.text != null) {
                            TextViewCompat.setCompoundDrawableTintList(
                                holder.text, colors.secondaryTextColor.toColorStateList()
                            )
                        }
                    }
                })
        }
    }

    override fun getItemCount(): Int = dataSet.size

    override fun getItemId(position: Int): Long {
        return dataSet[position].year.toLong()
    }

    override fun getIdentifier(position: Int): ReleaseYear {
        return dataSet[position]
    }

    override fun getName(item: ReleaseYear): String {
        return item.name
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<ReleaseYear>) {
        callback?.yearsMenuItemClick(selection, menuItem)
    }

    inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {

        private val currentYear: ReleaseYear
            get() = dataSet[layoutPosition]

        override fun onClick(view: View) {
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
            } else {
                callback?.yearClick(currentYear)
            }
        }

        override fun onLongClick(view: View): Boolean {
            toggleChecked(layoutPosition)
            return true
        }

        init {
            if (itemLayoutRes == R.layout.item_list) {
                image?.useAsIcon()
            }

            // We could create a new menu for this, but I prefer to reuse the
            // Artist model menu, which includes the basic elements needed for
            // this case. We just need to remove the action_tag_editor item.
            menu?.setOnClickListener(object : OnClickMenu() {
                override val popupMenuRes: Int
                    get() = R.menu.menu_item_artist

                override fun onPreparePopup(menu: Menu) {
                    menu.removeItem(R.id.action_tag_editor)
                }

                override fun onMenuItemClick(item: MenuItem): Boolean {
                    return callback?.yearMenuItemClick(currentYear, item) ?: false
                }
            })
        }
    }
}
