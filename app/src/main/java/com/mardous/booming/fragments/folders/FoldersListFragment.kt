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

package com.mardous.booming.fragments.folders

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
import com.mardous.booming.adapters.FileAdapter
import com.mardous.booming.extensions.files.getCanonicalPathSafe
import com.mardous.booming.extensions.navigation.folderDetailArgs
import com.mardous.booming.extensions.showToast
import com.mardous.booming.fragments.base.AbsRecyclerViewCustomGridSizeFragment
import com.mardous.booming.helper.menu.onSongMenu
import com.mardous.booming.helper.menu.onSongsMenu
import com.mardous.booming.interfaces.IBackConsumer
import com.mardous.booming.interfaces.IFileCallback
import com.mardous.booming.model.Folder
import com.mardous.booming.model.GridViewType
import com.mardous.booming.model.Song
import com.mardous.booming.model.filesystem.FileSystemItem
import com.mardous.booming.model.filesystem.FileSystemQuery
import com.mardous.booming.model.isPresent
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.sort.SortOrder
import com.mardous.booming.util.sort.prepareSortOrder
import com.mardous.booming.util.sort.selectedSortOrder
import com.mardous.booming.viewmodels.library.ReloadType
import java.io.File

class FoldersListFragment : AbsRecyclerViewCustomGridSizeFragment<FileAdapter, GridLayoutManager>(),
    IFileCallback, IBackConsumer {

    override val titleRes: Int = R.string.folders_label
    override val isShuffleVisible: Boolean = false

    override val defaultGridSize: Int
        get() = if (isLandscape) resources.getInteger(R.integer.default_list_columns_land)
        else resources.getInteger(R.integer.default_list_columns)

    override val maxGridSize: Int
        get() = if (isLandscape) resources.getInteger(R.integer.default_grid_columns_land)
        else resources.getInteger(R.integer.default_grid_columns)

    override val maxGridSizeForList: Int
        get() = gridSize

    override val itemLayoutRes: Int
        get() = R.layout.item_list

    private val fileSystem: FileSystemQuery?
        get() = libraryViewModel.getFileSystem().value

    private val isFlatView: Boolean
        get() = fileSystem?.isFlatView == true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getFileSystem().observe(viewLifecycleOwner) { fileSystem ->
            showFolders(fileSystem)
        }
    }

    override fun handleBackPress(): Boolean {
        val handledBackPress = fileSystem?.let {
            if (it.canGoUp) {
                libraryViewModel.navigateToPath(it.parentPath)
                true
            } else false
        }
        return handledBackPress == true
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Folders)
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireActivity(), gridSize)
    }

    override fun createAdapter(): FileAdapter {
        notifyLayoutResChanged(itemLayoutRes)
        val dataSet = adapter?.files ?: ArrayList()
        return FileAdapter(mainActivity, dataSet, itemLayoutRes, this)
    }

    override fun fileClick(file: FileSystemItem) {
        when (file) {
            is Song -> {
                libraryViewModel.listSongsFromFiles(file).observe(viewLifecycleOwner) { (songs, position) ->
                    playerViewModel.openQueue(songs, position)
                }
            }
            else -> {
                if (Preferences.hierarchyFolderView) {
                    libraryViewModel.navigateToPath(file.filePath)
                } else if (file is Folder) {
                    findNavController().navigate(R.id.nav_folder_detail, folderDetailArgs(file))
                }
            }
        }
    }

    override fun fileMenuItemClick(file: FileSystemItem, menuItem: MenuItem): Boolean {
        val recursiveActions = Preferences.recursiveFolderActions
        return when (file) {
            is Folder -> {
                when (menuItem.itemId) {
                    R.id.action_blacklist -> {
                        libraryViewModel.blacklistPath(File(file.filePath))
                    }
                    R.id.action_set_as_start_directory -> {
                        Preferences.startDirectory = File(file.filePath)
                    }
                    R.id.action_scan -> {
                        libraryViewModel.scanPaths(requireContext(), arrayOf(file.filePath))
                            .observe(viewLifecycleOwner) {
                                showToast(R.string.scan_finished)
                            }
                    }
                    else -> {
                        if (isFlatView) {
                            file.songs.onSongsMenu(this, menuItem)
                        } else {
                            val isRecursive = recursiveActions.isPresent(menuItem.itemId)
                            libraryViewModel.songs(
                                file.musicFiles,
                                includeFolders = isRecursive,
                                deepListing = isRecursive
                            ).observe(viewLifecycleOwner) {
                                it.onSongsMenu(this, menuItem)
                            }
                        }
                    }
                }
                true
            }
            is Song -> file.onSongMenu(this, menuItem)
            else -> false
        }
    }

    override fun filesMenuItemClick(selection: List<FileSystemItem>, menuItem: MenuItem): Boolean {
        if (isFlatView) {
            libraryViewModel.songs(selection).observe(viewLifecycleOwner) {
                it.onSongsMenu(this, menuItem)
            }
        } else {
            libraryViewModel.songs(
                selection,
                includeFolders = true,
                deepListing = Preferences.recursiveFolderActions.isPresent(menuItem.itemId)
            ).observe(viewLifecycleOwner) { songs ->
                songs.onSongsMenu(this, menuItem)
            }
        }
        return true
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        inflater.inflate(R.menu.menu_folders, menu)
        menu.removeItem(R.id.action_view_type)
        menu.findItem(R.id.action_hierarchy_view)?.isChecked = Preferences.hierarchyFolderView
        val sortOrderSubmenu = menu.findItem(R.id.action_sort_order)?.subMenu
        if (sortOrderSubmenu != null) {
            sortOrderSubmenu.clear()
            sortOrderSubmenu.add(0, R.id.action_sort_order_az, 0, R.string.sort_order_az)
            sortOrderSubmenu.add(0, R.id.action_sort_order_number_of_songs, 1, R.string.sort_order_number_of_songs)
            sortOrderSubmenu.add(0, R.id.action_sort_order_date_added, 2, R.string.sort_order_date_added)
            sortOrderSubmenu.add(0, R.id.action_sort_order_date_modified, 3, R.string.sort_order_date_modified)
            sortOrderSubmenu.add(1, R.id.action_sort_order_descending, 4, R.string.sort_order_descending)
            sortOrderSubmenu.setGroupCheckable(0, true, true)
            sortOrderSubmenu.prepareSortOrder(SortOrder.folderSortOrder)
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (item.selectedSortOrder(SortOrder.folderSortOrder)) {
            libraryViewModel.forceReload(ReloadType.Folders)
            return true
        }
        when (item.itemId) {
            R.id.action_hierarchy_view -> {
                val isChecked = !item.isChecked
                Preferences.hierarchyFolderView = isChecked
                item.isChecked = isChecked
                libraryViewModel.forceReload(ReloadType.Folders)
                return true
            }
            R.id.action_go_to_start_directory -> {
                libraryViewModel.navigateToPath(Preferences.startDirectory.getCanonicalPathSafe())
                return true
            }
            else -> return super.onMenuItemSelected(item)
        }
    }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        libraryViewModel.forceReload(ReloadType.Folders)
    }

    private fun showFolders(fileSystem: FileSystemQuery) {
        toolbar.menu.let {
            it.findItem(R.id.action_sort_order)?.isVisible = fileSystem.isFlatView
            it.findItem(R.id.action_go_to_start_directory)?.isVisible = !fileSystem.isFlatView
        }
        adapter?.submitList(fileSystem.getNavigableChildren())
    }

    override fun getSavedViewType(): GridViewType = GridViewType.Normal

    override fun saveViewType(viewType: GridViewType) {}

    override fun getSavedGridSize(): Int {
        return sharedPreferences.getInt(FOLDERS_GRID_SIZE_KEY, defaultGridSize)
    }

    override fun saveGridSize(newGridSize: Int) {
        sharedPreferences.edit {
            putInt(FOLDERS_GRID_SIZE_KEY, newGridSize)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onGridSizeChanged(isLand: Boolean, gridColumns: Int) {
        layoutManager?.spanCount = gridColumns
        adapter?.notifyDataSetChanged()
    }

    companion object {
        private const val FOLDERS_GRID_SIZE_KEY = "folders_grid_size"
    }
}