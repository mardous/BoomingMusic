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

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.mardous.booming.R
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.mapper.searchFilter
import com.mardous.booming.data.mapper.toSongEntity
import com.mardous.booming.data.mapper.toSongs
import com.mardous.booming.data.mapper.toSongsEntity
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentPlaylistDetailBinding
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.glide.playlistOptions
import com.mardous.booming.extensions.media.isFavorites
import com.mardous.booming.extensions.media.playlistInfo
import com.mardous.booming.extensions.navigation.searchArgs
import com.mardous.booming.extensions.resources.createFastScroller
import com.mardous.booming.extensions.resources.surfaceColor
import com.mardous.booming.glide.playlistPreview.PlaylistPreview
import com.mardous.booming.service.playback.Playback
import com.mardous.booming.ui.ISongCallback
import com.mardous.booming.ui.adapters.song.PlaylistSongAdapter
import com.mardous.booming.ui.component.base.AbsMainActivityFragment
import com.mardous.booming.ui.component.menu.onPlaylistMenu
import com.mardous.booming.ui.component.menu.onSongMenu
import com.mardous.booming.ui.component.menu.onSongsMenu
import com.mardous.booming.ui.dialogs.playlists.RemoveFromPlaylistDialog
import com.mardous.booming.util.Preferences
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Christians M. A. (mardous)
 */
class PlaylistDetailFragment : AbsMainActivityFragment(R.layout.fragment_playlist_detail),
    ISongCallback {

    private val arguments by navArgs<PlaylistDetailFragmentArgs>()
    private val detailViewModel by viewModel<PlaylistDetailViewModel> {
        parametersOf(arguments.playlistId)
    }

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!

    private var playlist: PlaylistWithSongs = PlaylistWithSongs.Empty

    private var playlistSongAdapter: PlaylistSongAdapter? = null
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            drawingViewId = R.id.fragment_container
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(surfaceColor())
            setPathMotion(MaterialArcMotion())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaylistDetailBinding.bind(view)

        setupButtons()
        setupRecyclerView()

        materialSharedAxis(view)
        view.applyHorizontalWindowInsets()

        setSupportActionBar(binding.toolbar)
        //binding.collapsingAppBarLayout.setupStatusBarScrim(requireContext())

        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            binding.recyclerView.updatePadding(bottom = it.getWithSpace())
        }

        detailViewModel.getPlaylist().observe(viewLifecycleOwner) { playlistWithSongs ->
            playlist = playlistWithSongs
            binding.title.text = playlist.playlistEntity.playlistName
            val description = playlist.playlistEntity.description
            if (!description.isNullOrEmpty()) {
                binding.description.text = description
                binding.description.isVisible = true
            } else {
                binding.description.text = null
                binding.description.isVisible = false
            }
            binding.subtitle.text = playlist.songs.toSongs().playlistInfo(requireContext())
            binding.collapsingAppBarLayout.title = playlist.playlistEntity.playlistName
            binding.image.let { image ->
                Glide.with(this)
                    .asBitmap()
                    .load(PlaylistPreview(playlistWithSongs))
                    .playlistOptions()
                    .into(image)
            }
        }
        detailViewModel.getSongs().observe(viewLifecycleOwner) {
            binding.progressIndicator.hide()
            playlistSongAdapter?.dataSet = it.toSongs()
        }
        detailViewModel.playlistExists().observe(viewLifecycleOwner) {
            if (!it) {
                findNavController().navigateUp()
            }
        }
    }

    private fun checkIsEmpty() {
        binding.empty.isVisible = playlistSongAdapter?.isNullOrEmpty == true
    }

    private fun setupButtons() {
        binding.playAction.setOnClickListener {
            playlistSongAdapter?.dataSet?.let {
                playerViewModel.openQueue(it, shuffleMode = Playback.ShuffleMode.Off)
            }
        }
        binding.shuffleAction.setOnClickListener {
            playlistSongAdapter?.dataSet?.let {
                playerViewModel.openQueue(it, shuffleMode = Playback.ShuffleMode.On)
            }
        }
        binding.searchAction?.setOnClickListener {
            findNavController().navigate(
                R.id.nav_search,
                searchArgs(playlist.playlistEntity.searchFilter(requireContext()))
            )
        }
    }

    private fun setupRecyclerView() {
        playlistSongAdapter = PlaylistSongAdapter(
            activity = mainActivity,
            dataSet = emptyList(),
            itemLayoutRes = R.layout.item_list_draggable,
            isLockDrag = Preferences.lockedPlaylists,
            callback = this
        )
        recyclerViewDragDropManager = RecyclerViewDragDropManager().also { dragDropManager ->
            wrappedAdapter = dragDropManager.createWrappedAdapter(playlistSongAdapter!!)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = wrappedAdapter
        binding.recyclerView.itemAnimator = RefactoredDefaultItemAnimator()
        binding.recyclerView.createFastScroller()
        recyclerViewDragDropManager?.attachRecyclerView(binding.recyclerView)
        playlistSongAdapter!!.registerAdapterDataObserver(adapterDataObserver)
    }

    private val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            checkIsEmpty()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_playlist_detail, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        playlist.let {
            if (it.playlistEntity.isFavorites(requireContext())) {
                menu.removeItem(R.id.action_edit_playlist)
                menu.removeItem(R.id.action_delete_playlist)
            }
        }
        if (playlistSongAdapter?.isLockDrag == true) {
            menu.findItem(R.id.action_lock)
                ?.setIcon(R.drawable.ic_lock_24dp)
        } else {
            menu.findItem(R.id.action_lock)
                ?.setIcon(R.drawable.ic_lock_open_24dp)
        }
        if (!isLandscape()) {
            menu.removeItem(R.id.action_search)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }

            R.id.action_search -> {
                findNavController().navigate(
                    R.id.nav_search,
                    searchArgs(playlist.playlistEntity.searchFilter(requireContext()))
                )
                true
            }

            R.id.action_lock -> {
                val lockedPlaylists = !Preferences.lockedPlaylists
                Preferences.lockedPlaylists = lockedPlaylists
                if (lockedPlaylists) {
                    menuItem.setIcon(R.drawable.ic_lock_24dp)
                    showToast(R.string.playlist_locked)
                } else {
                    menuItem.setIcon(R.drawable.ic_lock_open_24dp)
                    showToast(R.string.playlist_unlocked)
                }
                playlistSongAdapter?.setLockDrag(lockedPlaylists)
                true
            }

            else -> playlist.onPlaylistMenu(this, menuItem)
        }
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean {
        return when (menuItem.itemId) {
            R.id.action_remove_from_playlist -> {
                RemoveFromPlaylistDialog.create(song.toSongEntity(playlist.playlistEntity.playListId))
                    .show(childFragmentManager, "REMOVE_FROM_PLAYLIST")
                true
            }

            else -> song.onSongMenu(this, menuItem)
        }
    }

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_remove_from_playlist -> {
                RemoveFromPlaylistDialog.create(songs.toSongsEntity(playlist.playlistEntity))
                    .show(childFragmentManager, "REMOVE_FROM_PLAYLIST")
            }

            else -> songs.onSongsMenu(this, menuItem)
        }
    }

    override fun onPause() {
        recyclerViewDragDropManager?.cancelDrag()
        playlistSongAdapter?.saveSongs(playlist.playlistEntity)
        super.onPause()
    }

    override fun onDestroyView() {
        playlistSongAdapter?.unregisterAdapterDataObserver(adapterDataObserver)

        recyclerViewDragDropManager?.release()
        recyclerViewDragDropManager = null

        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = null

        WrapperAdapterUtils.releaseAll(wrappedAdapter)
        wrappedAdapter = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "PlaylistDetail"
    }
}