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

package com.mardous.booming.ui.screen.library.playlists

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.mardous.booming.R
import com.mardous.booming.core.model.GridViewType
import com.mardous.booming.core.sort.PlaylistSortMode
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.extensions.navigation.playlistDetailArgs
import com.mardous.booming.ui.IPlaylistCallback
import com.mardous.booming.ui.adapters.PlaylistAdapter
import com.mardous.booming.ui.component.base.AbsRecyclerViewCustomGridSizeFragment
import com.mardous.booming.ui.component.menu.onPlaylistMenu
import com.mardous.booming.ui.component.menu.onPlaylistsMenu
import com.mardous.booming.ui.dialogs.playlists.CreatePlaylistDialog
import com.mardous.booming.ui.dialogs.playlists.ImportPlaylistDialog
import com.mardous.booming.ui.screen.library.ReloadType

/**
 * @author Christians M. A. (mardous)
 */
class PlaylistListFragment : AbsRecyclerViewCustomGridSizeFragment<PlaylistAdapter, GridLayoutManager>(),
    IPlaylistCallback {

    override val titleRes: Int = R.string.playlists_label
    override val isShuffleVisible: Boolean = false
    override val emptyMessageRes: Int
        get() = R.string.no_device_playlists

    override val maxGridSize: Int
        get() = if (isLandscape) resources.getInteger(R.integer.max_playlist_columns_land)
        else resources.getInteger(R.integer.max_playlist_columns)

    override val itemLayoutRes: Int
        get() = if (isGridMode) R.layout.item_playlist
        else R.layout.item_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getPlaylists().observe(viewLifecycleOwner) { playlists ->
            adapter?.dataSet = playlists
        }
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Playlists)
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireContext(), gridSize)
    }

    override fun createAdapter(): PlaylistAdapter {
        notifyLayoutResChanged(itemLayoutRes)
        val dataSet = adapter?.dataSet ?: ArrayList()
        return PlaylistAdapter(mainActivity, dataSet, itemLayoutRes, this)
    }

    override fun playlistClick(playlist: PlaylistWithSongs) {
        findNavController().navigate(R.id.nav_playlist_detail, playlistDetailArgs(playlist.playlistEntity.playListId))
    }

    override fun playlistMenuItemClick(playlist: PlaylistWithSongs, menuItem: MenuItem): Boolean {
        return playlist.onPlaylistMenu(this, menuItem)
    }

    override fun playlistsMenuItemClick(playlists: List<PlaylistWithSongs>, menuItem: MenuItem) {
        playlists.onPlaylistsMenu(this, menuItem)
    }

    override fun onMediaContentChanged() {
        libraryViewModel.forceReload(ReloadType.Playlists)
    }

    override fun onFavoriteContentChanged() {
        libraryViewModel.forceReload(ReloadType.Playlists)
    }

    override fun onPause() {
        super.onPause()
        adapter?.actionMode?.finish()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        menu.removeItem(R.id.action_view_type)
        menu.add(0, R.id.action_new_playlist, 0, R.string.new_playlist_title)
        menu.add(0, R.id.action_import_playlist, 0, R.string.action_import_playlist)
        PlaylistSortMode.AllPlaylists.createMenu(menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (PlaylistSortMode.AllPlaylists.sortItemSelected(item)) {
            libraryViewModel.forceReload(ReloadType.Playlists)
            return true
        }
        return when (item.itemId) {
            R.id.action_new_playlist -> {
                CreatePlaylistDialog()
                    .show(childFragmentManager, "NEW_PLAYLIST")
                true
            }
            R.id.action_import_playlist -> {
                ImportPlaylistDialog().show(childFragmentManager, "IMPORT_PLAYLIST")
                true
            }
            else -> super.onMenuItemSelected(item)
        }
    }

    override fun getSavedViewType(): GridViewType {
        return GridViewType.Normal
    }

    override fun saveViewType(viewType: GridViewType) {}

    override fun getSavedGridSize(): Int {
        return sharedPreferences.getInt(GRID_SIZE, defaultGridSize)
    }

    override fun saveGridSize(newGridSize: Int) {
        sharedPreferences.edit { putInt(GRID_SIZE, newGridSize) }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onGridSizeChanged(isLand: Boolean, gridColumns: Int) {
        layoutManager?.spanCount = gridColumns
        adapter?.notifyDataSetChanged()
    }

    companion object {
        private const val GRID_SIZE = "playlists_grid_size"
    }
}