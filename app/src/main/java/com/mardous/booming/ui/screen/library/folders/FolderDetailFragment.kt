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

package com.mardous.booming.ui.screen.library.folders

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.mardous.booming.R
import com.mardous.booming.core.sort.SongSortMode
import com.mardous.booming.data.mapper.searchFilter
import com.mardous.booming.data.model.Folder
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentDetailListBinding
import com.mardous.booming.extensions.applyHorizontalWindowInsets
import com.mardous.booming.extensions.materialSharedAxis
import com.mardous.booming.extensions.media.songCountStr
import com.mardous.booming.extensions.media.songsDurationStr
import com.mardous.booming.extensions.navigation.searchArgs
import com.mardous.booming.extensions.setSupportActionBar
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.service.playback.Playback
import com.mardous.booming.ui.ISongCallback
import com.mardous.booming.ui.adapters.song.SongAdapter
import com.mardous.booming.ui.component.base.AbsMainActivityFragment
import com.mardous.booming.ui.component.menu.onSongMenu
import com.mardous.booming.ui.component.menu.onSongsMenu
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class FolderDetailFragment : AbsMainActivityFragment(R.layout.fragment_detail_list), ISongCallback {

    private val arguments by navArgs<FolderDetailFragmentArgs>()
    private val detailViewModel: FolderDetailViewModel by viewModel {
        parametersOf(arguments.extraFolderPath)
    }

    private var _binding: FragmentDetailListBinding? = null
    private val binding get() = _binding!!

    private lateinit var songAdapter: SongAdapter

    private val folder: Folder
        get() = detailViewModel.getFolder().value ?: Folder.empty

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailListBinding.bind(view)
        materialSharedAxis(view)
        setSupportActionBar(binding.toolbar)

        view.applyHorizontalWindowInsets()

        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            binding.recyclerView.updatePadding(bottom = it.getWithSpace())
        }

        setupButtons()
        setupRecyclerView()
        detailViewModel.getFolder().observe(viewLifecycleOwner) {
            binding.collapsingAppBarLayout.title = it.fileName
            binding.title.text = it.fileName
            songs(it.songs)
        }
    }

    private fun setupButtons() {
        binding.playAction.setOnClickListener {
            playerViewModel.openQueue(songAdapter.dataSet, shuffleMode = Playback.ShuffleMode.Off)
        }
        binding.shuffleAction.setOnClickListener {
            playerViewModel.openQueue(songAdapter.dataSet, shuffleMode = Playback.ShuffleMode.On)
        }
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(
            activity = requireActivity(),
            dataSet = ArrayList(),
            itemLayoutRes = R.layout.item_list,
            sortMode = SongSortMode.FolderSongs,
            callback = this
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
        }
    }

    fun songs(songs: List<Song>) {
        if (songs.isEmpty()) {
            findNavController().popBackStack()
            return
        }
        binding.progressIndicator.hide()
        binding.subtitle.text =
            buildInfoString(songs.songCountStr(requireContext()), songs.songsDurationStr())
        songAdapter.dataSet = songs
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean = song.onSongMenu(this, menuItem)

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {
        songs.onSongsMenu(this, menuItem)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_folder_detail, menu)
        SongSortMode.FolderSongs.createMenu(menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when {
            SongSortMode.FolderSongs.sortItemSelected(item) -> {
                detailViewModel.loadDetail()
                true
            }

            item.itemId == R.id.action_search -> {
                findNavController().navigate(
                    R.id.nav_search,
                    searchArgs(folder.searchFilter(requireContext()))
                )
                true
            }

            item.itemId == android.R.id.home -> {
                findNavController().navigateUp()
                true
            }

            else -> songAdapter.dataSet.onSongsMenu(this, item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}