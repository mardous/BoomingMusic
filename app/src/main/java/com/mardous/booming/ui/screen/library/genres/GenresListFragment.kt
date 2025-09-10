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

package com.mardous.booming.ui.screen.library.genres

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
import com.mardous.booming.data.model.Genre
import com.mardous.booming.extensions.navigation.genreDetailArgs
import com.mardous.booming.ui.IGenreCallback
import com.mardous.booming.ui.adapters.GenreAdapter
import com.mardous.booming.ui.component.base.AbsRecyclerViewCustomGridSizeFragment
import com.mardous.booming.ui.screen.library.ReloadType
import com.mardous.booming.util.sort.SortOrder
import com.mardous.booming.util.sort.prepareSortOrder
import com.mardous.booming.util.sort.selectedSortOrder

class GenresListFragment : AbsRecyclerViewCustomGridSizeFragment<GenreAdapter, GridLayoutManager>(),
    IGenreCallback {

    override val titleRes: Int = R.string.genres_label
    override val isShuffleVisible: Boolean = false
    override val emptyMessageRes: Int = R.string.no_genres_label

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getGenres().observe(viewLifecycleOwner) { genres ->
            adapter?.dataSet = genres
        }
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Genres)
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireActivity(), gridSize)
    }

    override fun createAdapter(): GenreAdapter {
        val itemLayoutRes = itemLayoutRes
        notifyLayoutResChanged(itemLayoutRes)
        val dataSet = adapter?.dataSet ?: ArrayList()
        return GenreAdapter(dataSet, itemLayoutRes, this)
    }

    override fun genreClick(genre: Genre) {
        findNavController().navigate(R.id.nav_genre_detail, genreDetailArgs(genre))
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        val sortOrderSubmenu = menu.findItem(R.id.action_sort_order)?.subMenu
        if (sortOrderSubmenu != null) {
            sortOrderSubmenu.clear()
            sortOrderSubmenu.add(0, R.id.action_sort_order_az, 0, R.string.sort_order_az)
            sortOrderSubmenu.add(0, R.id.action_sort_order_number_of_songs, 1, R.string.sort_order_number_of_songs)
            sortOrderSubmenu.add(1, R.id.action_sort_order_descending, 2, R.string.sort_order_descending)
            sortOrderSubmenu.setGroupCheckable(0, true, true)
            sortOrderSubmenu.prepareSortOrder(SortOrder.genreSortOrder)
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (item.selectedSortOrder(SortOrder.genreSortOrder)) {
            libraryViewModel.forceReload(ReloadType.Genres)
            return true
        }
        return super.onMenuItemSelected(item)
    }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        libraryViewModel.forceReload(ReloadType.Genres)
    }

    override fun getSavedViewType(): GridViewType {
        return GridViewType.entries.firstOrNull {
            it.name == sharedPreferences.getString(VIEW_TYPE, null)
        } ?: GridViewType.Image
    }

    override fun saveViewType(viewType: GridViewType) {
        sharedPreferences.edit { putString(VIEW_TYPE, viewType.name) }
    }

    override fun getSavedGridSize(): Int {
        return sharedPreferences.getInt(GRID_SIZE, defaultGridSize)
    }

    override fun saveGridSize(newGridSize: Int) {
        sharedPreferences.edit {
            putInt(GRID_SIZE, newGridSize)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onGridSizeChanged(isLand: Boolean, gridColumns: Int) {
        layoutManager?.spanCount = gridColumns
        adapter?.notifyDataSetChanged()
    }

    companion object {
        private const val VIEW_TYPE = "genres_view_type"
        private const val GRID_SIZE = "genres_grid_size"
    }
}