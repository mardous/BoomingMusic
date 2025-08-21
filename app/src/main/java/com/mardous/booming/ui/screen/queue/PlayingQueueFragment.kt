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

package com.mardous.booming.ui.screen.queue

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.mardous.booming.R
import com.mardous.booming.core.model.action.QueueQuickAction
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentQueueBinding
import com.mardous.booming.extensions.applyBottomWindowInsets
import com.mardous.booming.extensions.applyScrollableContentInsets
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.media.songCountStr
import com.mardous.booming.extensions.resources.createFastScroller
import com.mardous.booming.extensions.resources.inflateMenu
import com.mardous.booming.extensions.resources.onVerticalScroll
import com.mardous.booming.extensions.showToast
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.ui.adapters.song.PlayingQueueSongAdapter
import com.mardous.booming.ui.component.menu.onSongMenu
import com.mardous.booming.ui.dialogs.playlists.CreatePlaylistDialog
import com.mardous.booming.ui.screen.MainActivity
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.util.Preferences
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * @author Christians M. A. (mardous)
 */
class PlayingQueueFragment : Fragment(R.layout.fragment_queue),
    Toolbar.OnMenuItemClickListener,
    View.OnClickListener,
    PlayingQueueSongAdapter.Callback {

    private val playerViewModel: PlayerViewModel by activityViewModel()
    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!

    private val toolbar get() = binding.appBarLayout.toolbar

    private var playingQueueAdapter: PlayingQueueSongAdapter? = null
    private var dragDropManager: RecyclerViewDragDropManager? = null
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
    private var linearLayoutManager: LinearLayoutManager? = null

    private var lastRemovedSong: Song? = null
    private var snackbar: Snackbar? = null

    private val allQuickActions by lazy { QueueQuickAction.entries }
    private var selectedQuickAction: QueueQuickAction by Delegates.observable(Preferences.queueQuickAction) { _: KProperty<*>, old: QueueQuickAction?, new: QueueQuickAction ->
        Preferences.queueQuickAction = new
        updateQuickAction(old, new)
    }

    private val queueInfo: CharSequence
        get() = buildInfoString(
            playingQueue.songCountStr(requireContext()),
            playerViewModel.queueDurationAsString
        )

    private val playingQueue: List<Song>
        get() = playerViewModel.playingQueue

    private val queuePosition: Int
        get() = playerViewModel.currentPosition

    private val mainActivity: MainActivity
        get() = activity as MainActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQueueBinding.bind(view)
        binding.quickActionButton.setOnClickListener(this@PlayingQueueFragment)
        binding.quickActionButton.setOnLongClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setItems(allQuickActions.map { getString(it.titleRes) }.toTypedArray()) { _, position: Int ->
                    selectedQuickAction = allQuickActions[position]
                }
                .show()
            true
        }

        enterTransition = Fade()
        exitTransition = Fade()
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        applyWindowInsets(view)

        playingQueueAdapter = PlayingQueueSongAdapter(
            requireActivity(),
            arrayListOf(),
            this,
            queuePosition
        ).also { adapter ->
            dragDropManager = RecyclerViewDragDropManager().also { manager ->
                wrappedAdapter = manager.createWrappedAdapter(adapter)
            }
        }

        linearLayoutManager = LinearLayoutManager(requireContext())

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = wrappedAdapter
        binding.recyclerView.itemAnimator = RefactoredDefaultItemAnimator()

        dragDropManager!!.attachRecyclerView(_binding!!.recyclerView)
        linearLayoutManager!!.scrollToPositionWithOffset(queuePosition + 1, 0)

        binding.recyclerView.onVerticalScroll(
            viewLifecycleOwner,
            onScrollDown = { binding.quickActionButton.hide() },
            onScrollUp = { binding.quickActionButton.show() }
        )
        binding.recyclerView.createFastScroller()

        toolbar.isTitleCentered = false
        toolbar.setNavigationIcon(R.drawable.ic_back_24dp)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        toolbar.setTitle(R.string.playing_queue_label)
        toolbar.inflateMenu(R.menu.menu_playing_queue, this) { menu ->
            if (Preferences.isQueueLocked) {
                menu.findItem(R.id.action_lock)
                    .icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_lock_24dp)
            }else {
                menu.findItem(R.id.action_lock)
                    .icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_lock_open_24dp)
            }
        }
        updateQuickAction(null, selectedQuickAction)

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.currentPositionFlow.collect { position ->
                playingQueueAdapter?.setPosition(position)
                toolbar.subtitle = queueInfo
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.playingQueueFlow.collect { playingQueue ->
                if (playingQueue.isEmpty()) {
                    findNavController().navigateUp()
                } else {
                    playingQueueAdapter?.swapDataSet(playingQueue)
                    toolbar.subtitle = queueInfo
                }
            }
        }
    }

    private fun applyWindowInsets(view: View) {
        view.applyScrollableContentInsets(binding.recyclerView)
        binding.quickActionButton.applyBottomWindowInsets()
    }

    override fun onClick(view: View) {
        if (view == binding.quickActionButton) {
            val menuItem = toolbar.menu.findItem(selectedQuickAction.menuItemId)
            if (menuItem != null) {
                onMenuItemClick(menuItem)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save_playing_queue -> {
                CreatePlaylistDialog.create(playingQueue)
                    .show(childFragmentManager, "CREATE_PLAYLIST")
                true
            }

            R.id.action_clear_playing_queue -> {
                playerViewModel.clearQueue()
                true
            }

            R.id.action_shuffle_queue -> {
                playerViewModel.shuffleQueue()
                true
            }

            R.id.action_move_to_current_track -> {
                resetToCurrentPosition()
                true
            }

            R.id.action_lock -> {
                Preferences.isQueueLocked = !Preferences.isQueueLocked
                if (Preferences.isQueueLocked) {
                    item.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_lock_24dp)
                    showToast(ContextCompat.getString(requireContext(), R.string.queue_locked))
                }else {
                    item.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_lock_open_24dp)
                    showToast(ContextCompat.getString(requireContext(), R.string.queue_unlocked))
                }
                playingQueueAdapter?.notifyDataSetChanged()
                true
            }

            else -> false
        }
    }

    override fun onRemoveSong(song: Song, fromPosition: Int) {
        lastRemovedSong = song
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }
        snackbar = Snackbar.make(
            binding.root.context,
            binding.root,
            getString(R.string.x_removed_from_playing_queue, lastRemovedSong!!.title),
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.undo_action) {
                playerViewModel.enqueue(song, fromPosition).observe(viewLifecycleOwner) {
                    showToast(R.string.added_title_to_playing_queue)
                }
            }
            .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(snackbar: Snackbar, event: Int) {
                    snackbar.removeCallback(this)
                }

                override fun onShown(snackbar: Snackbar) {
                    playerViewModel.removePosition(fromPosition)
                }
            }).also { newSnackbar ->
                newSnackbar.show()
            }
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean {
        return song.onSongMenu(this, menuItem)
    }

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {}

    private fun resetToCurrentPosition() {
        binding.recyclerView.stopScroll()
        linearLayoutManager?.scrollToPositionWithOffset(queuePosition, 0)
    }

    private fun updateQuickAction(oldAction: QueueQuickAction?, newAction: QueueQuickAction) {
        if (oldAction != null) {
            toolbar.menu.findItem(oldAction.menuItemId).isVisible = true
        }
        toolbar.menu.findItem(newAction.menuItemId).isVisible = false
        binding.quickActionButton.setText(newAction.titleRes)
        binding.quickActionButton.setIconResource(newAction.iconRes)
    }

    override fun onPause() {
        dragDropManager?.cancelDrag()
        super.onPause()
    }

    override fun onDestroyView() {
        dragDropManager?.release()
        dragDropManager = null

        WrapperAdapterUtils.releaseAll(wrappedAdapter)
        wrappedAdapter = null
        playingQueueAdapter = null

        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = null
        binding.recyclerView.layoutManager = null

        linearLayoutManager = null

        super.onDestroyView()
        if (playerViewModel.playingQueue.isNotEmpty())
            mainActivity.expandPanel()
    }
}