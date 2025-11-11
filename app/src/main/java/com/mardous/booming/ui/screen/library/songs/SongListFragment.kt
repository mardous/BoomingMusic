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

package com.mardous.booming.ui.screen.library.songs

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.edit
import androidx.recyclerview.widget.GridLayoutManager
import com.mardous.booming.R
import com.mardous.booming.core.model.GridViewType
import com.mardous.booming.core.sort.SongSortMode
import com.mardous.booming.data.model.Song
import com.mardous.booming.ui.ISongCallback
import com.mardous.booming.ui.adapters.song.SongAdapter
import com.mardous.booming.ui.component.base.AbsRecyclerViewCustomGridSizeFragment
import com.mardous.booming.ui.component.menu.onSongMenu
import com.mardous.booming.ui.component.menu.onSongsMenu
import com.mardous.booming.ui.screen.library.ReloadType

/**
 * @author Christians M. A. (mardous)
 */
class SongListFragment : AbsRecyclerViewCustomGridSizeFragment<SongAdapter, GridLayoutManager>(), ISongCallback {

    override val titleRes: Int = R.string.songs_label
    override val isShuffleVisible: Boolean = true
    override val emptyMessageRes: Int
        get() = R.string.no_songs_label

    override val defaultGridSize: Int
        get() = if (isLandscape) resources.getInteger(R.integer.default_list_columns_land)
        else resources.getInteger(R.integer.default_list_columns)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getSongs().observe(viewLifecycleOwner) { songs ->
            adapter?.dataSet = songs
        }
    }

    override fun onShuffleClicked() {
        super.onShuffleClicked()
        adapter?.dataSet?.let { songs ->
            playerViewModel.openAndShuffleQueue(songs)
        }
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Songs)
    }

    override fun onPause() {
        super.onPause()
        adapter?.actionMode?.finish()
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(activity, gridSize)
    }

    override fun createAdapter(): SongAdapter {
        notifyLayoutResChanged(itemLayoutRes)
        val dataSet = adapter?.dataSet ?: ArrayList()
        return SongAdapter(
            activity = mainActivity,
            dataSet = dataSet,
            itemLayoutRes = itemLayoutRes,
            sortMode = SongSortMode.AllSongs,
            callback = this
        )
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean {
        return song.onSongMenu(this, menuItem, sharedElements)
    }

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {
        songs.onSongsMenu(this, menuItem)
    }

    override fun getSavedViewType(): GridViewType {
        return GridViewType.entries.firstOrNull {
            it.name == sharedPreferences.getString(VIEW_TYPE, null)
        } ?: GridViewType.Normal
    }

    override fun saveViewType(viewType: GridViewType) {
        sharedPreferences.edit { putString(VIEW_TYPE, viewType.name) }
    }

    override fun getSavedGridSize(): Int {
        return sharedPreferences.getInt(GRID_SIZE, defaultGridSize)
    }

    override fun saveGridSize(newGridSize: Int) {
        sharedPreferences.edit { putInt(GRID_SIZE, newGridSize) }
    }

    override fun onGridSizeChanged(isLand: Boolean, gridColumns: Int) {
        layoutManager?.spanCount = gridColumns
        adapter?.notifyDataSetChanged()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        SongSortMode.AllSongs.createMenu(menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (SongSortMode.AllSongs.sortItemSelected(item)) {
            libraryViewModel.forceReload(ReloadType.Songs)
            return true
        }
        return super.onMenuItemSelected(item)
    }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        libraryViewModel.forceReload(ReloadType.Songs)
    }

    companion object {
        private const val VIEW_TYPE = "songs_view_type"
        private const val GRID_SIZE = "songs_grid_size"
    }
}