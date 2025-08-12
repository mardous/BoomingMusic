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

package com.mardous.booming.ui.dialogs.playlists

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.ui.adapters.AddToPlaylistAdapter
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.databinding.DialogProgressRecyclerViewBinding
import com.mardous.booming.extensions.EXTRA_SONGS
import com.mardous.booming.extensions.create
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.extensions.showToast
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.data.model.Song
import com.mardous.booming.ui.screen.library.LibraryViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians M. A. (mardous)
 */
class AddToPlaylistDialog : DialogFragment(), AddToPlaylistAdapter.IAddToPlaylistCallback {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val songs by extraNotNull<List<Song>>(EXTRA_SONGS)

    private lateinit var adapter: AddToPlaylistAdapter
    private lateinit var binding: DialogProgressRecyclerViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = AddToPlaylistAdapter(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogProgressRecyclerViewBinding.inflate(layoutInflater)
        binding.recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_playlist_title)
            .setView(binding.root)
            .setPositiveButton(R.string.close_action, null)
            .create {
                libraryViewModel.playlistsAsync().observe(this) { playlists ->
                    binding.progressIndicator.hide()
                    adapter.data(playlists)
                    binding.recyclerView.scheduleLayoutAnimation()
                }
            }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val fragment = childFragmentManager.findFragmentByTag("CREATE_PLAYLIST")
        if (fragment is DialogFragment) {
            fragment.dismiss()
        }
    }

    override fun newPlaylistClick() {
        val dialog = CreatePlaylistDialog.create(songs)
        dialog.callback(object : CreatePlaylistDialog.PlaylistCreatedCallback {
            override fun playlistCreated() {
                dismiss()
            }
        })
        dialog.show(childFragmentManager, "CREATE_PLAYLIST")
    }

    override fun playlistClick(playlist: PlaylistWithSongs) {
        val playlistName = playlist.playlistEntity.playlistName
        libraryViewModel.addToPlaylist(playlistName, songs).observe(this) {
            if (it.isWorking) {
                adapter.adding(playlist.playlistEntity.playListId)
            } else {
                if (it.insertedSongs > 1) {
                    showToast(
                        getString(
                            R.string.inserted_x_songs_into_playlist_x,
                            it.insertedSongs,
                            it.playlistName
                        )
                    )
                } else if (it.insertedSongs == 1) {
                    showToast(
                        getString(
                            R.string.inserted_one_song_into_playlist_x,
                            it.playlistName
                        )
                    )
                }
                adapter.adding(-1)
            }
        }
    }

    companion object {
        fun create(song: Song) = create(listOf(song))

        fun create(songs: List<Song>): AddToPlaylistDialog {
            return AddToPlaylistDialog().withArgs {
                putParcelableArrayList(EXTRA_SONGS, ArrayList(songs))
            }
        }
    }
}