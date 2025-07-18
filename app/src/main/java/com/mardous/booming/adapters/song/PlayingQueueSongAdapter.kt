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

package com.mardous.booming.adapters.song

import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemState
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.mardous.booming.R
import com.mardous.booming.extensions.resources.hitTest
import com.mardous.booming.extensions.showToast
import com.mardous.booming.interfaces.ISongCallback
import com.mardous.booming.model.Song
import com.mardous.booming.viewmodels.player.PlayerViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel

class PlayingQueueSongAdapter(
    activity: FragmentActivity,
    dataSet: List<Song>,
    callback: Callback? = null,
    current: Int
) : SongAdapter(activity, dataSet, R.layout.item_list_draggable, callback = callback),
    DraggableItemAdapter<PlayingQueueSongAdapter.ViewHolder> {

    interface Callback : ISongCallback {
        fun onRemoveSong(song: Song, fromPosition: Int)
    }

    var currentPosition: Int = current

    override fun createViewHolder(view: View, viewType: Int): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongAdapter.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder.itemViewType == HISTORY || holder.itemViewType == CURRENT) {
            setAlpha(holder, 0.5f)
        }
    }

    fun setPosition(newPosition: Int) {
        if (newPosition > dataSet.lastIndex || newPosition < 0)
            return

        if (newPosition > this.currentPosition) {
            notifyItemRangeChanged(currentPosition, (newPosition - currentPosition) + 1)
        } else if (newPosition < this.currentPosition) {
            notifyItemRangeChanged(newPosition, (currentPosition - newPosition) + 1)
        } else {
            notifyItemRangeChanged(0, newPosition)
        }
        this.currentPosition = newPosition
    }

    fun swapDataSet(dataSet: List<Song>) {
        this.dataSet = dataSet
    }

    private fun setAlpha(holder: SongAdapter.ViewHolder, alpha: Float) {
        holder.image?.alpha = alpha
        holder.title?.alpha = alpha
        holder.text?.alpha = alpha
        holder.paletteColorContainer?.alpha = alpha
        holder.dragView?.alpha = alpha
        holder.menu?.alpha = alpha
    }

    override fun getPopupText(view: View, position: Int): CharSequence {
        return ""
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < currentPosition -> HISTORY
            position > currentPosition -> UP_NEXT
            else -> CURRENT
        }
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean {
        return holder.dragView?.hitTest(x, y) ?: false
    }

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange? {
        return null
    }

    override fun onMoveItem(from: Int, to: Int) {
        val playerViewModel = activity.getViewModel<PlayerViewModel>()
        playerViewModel.moveSong(from, to)
    }

    override fun onCheckCanDrop(p1: Int, p2: Int): Boolean {
        return true
    }

    override fun onItemDragStarted(position: Int) {
        notifyDataSetChanged()
    }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        notifyDataSetChanged()
    }

    companion object {
        private const val HISTORY = 0
        private const val CURRENT = 1
        private const val UP_NEXT = 2
    }

    inner class ViewHolder internal constructor(itemView: View) : SongAdapter.ViewHolder(itemView),
        DraggableItemViewHolder {

        private val mDraggableItemState = DraggableItemState()

        override fun onClick(view: View) {
            val songPosition = layoutPosition
            val playerViewModel = activity.getViewModel<PlayerViewModel>()
            playerViewModel.playSongAt(songPosition)
        }

        override fun onLongClick(view: View): Boolean {
            return false
        }

        override fun onPrepareSongMenu(menu: Menu) {
            super.onPrepareSongMenu(menu)
            menu.findItem(R.id.action_put_after_current_track)?.let { menuItem ->
                menuItem.isEnabled = layoutPosition > currentPosition + 1
            }
            menu.findItem(R.id.action_stop_after_track)?.let { menuItem ->
                menuItem.isEnabled = layoutPosition >= currentPosition
            }
        }

        override fun onSongMenuItemClick(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_remove_from_playing_queue -> {
                    if (callback is Callback) {
                        callback.onRemoveSong(song, layoutPosition)
                    } else {
                        val playerViewModel = activity.getViewModel<PlayerViewModel>()
                        playerViewModel.removePosition(layoutPosition)
                    }
                    true
                }

                R.id.action_stop_after_track -> {
                    val playerViewModel = activity.getViewModel<PlayerViewModel>()
                    val stopAtResult = playerViewModel.stopAt(layoutPosition)
                    if (stopAtResult.first != Song.emptySong) {
                        if (stopAtResult.second) {
                            activity.showToast(
                                activity.getString(
                                    R.string.sleep_timer_stop_after_x,
                                    stopAtResult.first.title
                                )
                            )
                        } else {
                            activity.showToast(
                                activity.getString(
                                    R.string.sleep_timer_stop_after_x_canceled,
                                    stopAtResult.first.title
                                )
                            )
                        }
                    }
                    true
                }

                R.id.action_put_after_current_track -> {
                    val playerViewModel = activity.getViewModel<PlayerViewModel>()
                    playerViewModel.moveToNextPosition(layoutPosition)
                    true
                }

                else -> super.onSongMenuItemClick(item)
            }
        }

        override fun getDragState(): DraggableItemState {
            return mDraggableItemState
        }

        override fun getDragStateFlags(): Int {
            return mDraggableItemState.flags
        }

        override fun setDragStateFlags(flags: Int) {
            mDraggableItemState.flags = flags
        }

        override val songMenuRes: Int
            get() = R.menu.menu_item_playing_queue_song

        init {
            dragView?.visibility = View.VISIBLE
        }
    }

}