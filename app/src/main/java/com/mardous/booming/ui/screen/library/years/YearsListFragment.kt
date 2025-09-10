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

package com.mardous.booming.ui.screen.library.years

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
import com.mardous.booming.data.model.ReleaseYear
import com.mardous.booming.ui.IYearCallback
import com.mardous.booming.ui.adapters.YearAdapter
import com.mardous.booming.ui.component.base.AbsRecyclerViewCustomGridSizeFragment
import com.mardous.booming.ui.component.menu.onSongsMenu
import com.mardous.booming.ui.screen.library.ReloadType
import com.mardous.booming.util.sort.SortOrder
import com.mardous.booming.util.sort.prepareSortOrder
import com.mardous.booming.util.sort.selectedSortOrder

class YearsListFragment : AbsRecyclerViewCustomGridSizeFragment<YearAdapter, GridLayoutManager>(),
    IYearCallback {

    override val titleRes: Int = R.string.release_years_label
    override val isShuffleVisible: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getYears().observe(viewLifecycleOwner) { releaseYears ->
            adapter?.dataSet = releaseYears
        }
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Years)
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireActivity(), gridSize)
    }

    override fun createAdapter(): YearAdapter {
        notifyLayoutResChanged(itemLayoutRes)
        val dataSet = adapter?.dataSet ?: ArrayList()
        return YearAdapter(mainActivity, dataSet, itemLayoutRes, this)
    }

    override fun yearClick(year: ReleaseYear) {
        findNavController().navigate(R.id.nav_year_detail,
            YearDetailFragmentArgs.Builder(year.year)
                .build()
                .toBundle()
        )
    }

    override fun yearMenuItemClick(year: ReleaseYear, menuItem: MenuItem): Boolean {
        return year.songs.onSongsMenu(this, menuItem)
    }

    override fun yearsMenuItemClick(selection: List<ReleaseYear>, menuItem: MenuItem): Boolean {
        libraryViewModel.songs(selection).observe(viewLifecycleOwner) { songs ->
            songs.onSongsMenu(this@YearsListFragment, menuItem)
        }
        return true
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        val sortOrderSubmenu = menu.findItem(R.id.action_sort_order)?.subMenu
        if (sortOrderSubmenu != null) {
            sortOrderSubmenu.clear()
            sortOrderSubmenu.add(0, R.id.action_sort_order_year, 0, R.string.sort_order_year)
            sortOrderSubmenu.add(0, R.id.action_sort_order_number_of_songs, 1, R.string.sort_order_number_of_songs)
            sortOrderSubmenu.add(1, R.id.action_sort_order_descending, 2, R.string.sort_order_descending)
            sortOrderSubmenu.setGroupCheckable(0, true, true)
            sortOrderSubmenu.prepareSortOrder(SortOrder.yearSortOrder)
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (item.selectedSortOrder(SortOrder.yearSortOrder)) {
            libraryViewModel.forceReload(ReloadType.Years)
            return true
        }
        return super.onMenuItemSelected(item)
    }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        libraryViewModel.forceReload(ReloadType.Years)
    }

    override fun onPause() {
        super.onPause()
        adapter?.actionMode?.finish()
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
        sharedPreferences.edit { putInt(GRID_SIZE, newGridSize) }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onGridSizeChanged(isLand: Boolean, gridColumns: Int) {
        layoutManager?.spanCount = gridColumns
        adapter?.notifyDataSetChanged()
    }

    companion object {
        private const val VIEW_TYPE = "years_view_type"
        private const val GRID_SIZE = "years_grid_size"
    }
}