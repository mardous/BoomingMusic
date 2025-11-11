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
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import coil3.load
import coil3.size.Precision
import coil3.size.Scale
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.coil.DEFAULT_PLAYLIST_IMAGE
import com.mardous.booming.coil.placeholderDrawableRes
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.DialogCreatePlaylistBinding
import com.mardous.booming.extensions.EXTRA_SONGS
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.extensions.showToast
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.screen.library.LibraryViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class CreatePlaylistDialog : DialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val songs: List<Song> by extraNotNull(EXTRA_SONGS, arrayListOf())

    private var _binding: DialogCreatePlaylistBinding? = null
    private val binding get() = _binding!!

    private var callback: PlaylistCreatedCallback? = null
    private var selectedCoverUri: Uri? = null

    private val imagePickerLauncher = 
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedCoverUri = uri
                loadCoverImage(uri)
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreatePlaylistBinding.inflate(layoutInflater)
        
        val message = if (songs.isEmpty()) {
            getString(R.string.create_a_playlist)
        } else if (songs.size == 1) {
            getString(R.string.create_a_playlist_with_one_song)
        } else {
            getString(R.string.create_a_playlist_with_x_songs, songs.size)
        }

        setupViews()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_playlist_title)
            .setMessage(message)
            .setView(binding.root)
            .setPositiveButton(R.string.create_action, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create().apply {
                setOnShowListener { 
                    getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                        createPlaylist()
                    }
                }
            }
    }

    private fun setupViews() {
        // Set up cover image selection
        binding.selectCoverFab.setOnClickListener {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        // Load default playlist image
        loadCoverImage(null)
    }

    private fun loadCoverImage(uri: Uri?) {
        binding.playlistCoverImage.load(uri) {
            placeholderDrawableRes(binding.playlistCoverImage.context, DEFAULT_PLAYLIST_IMAGE)
            precision(Precision.INEXACT)
            scale(Scale.FILL)
        }
    }

    private fun createPlaylist() {
        val playlistName = binding.playlistNameEditText.text?.toString()?.trim()
        if (playlistName.isNullOrBlank()) {
            binding.playlistNameLayout.error = getString(R.string.playlist_name_cannot_be_empty)
            return
        }

        val description = binding.playlistDescriptionEditText.text?.toString()?.trim()
        val customCoverUri = selectedCoverUri?.toString()

        // Use the new createCustomPlaylist method
        libraryViewModel.createCustomPlaylist(
            playlistName = playlistName,
            customCoverUri = customCoverUri,
            description = if (description.isNullOrEmpty()) null else description,
            songs = songs
        ).observe(this) { result ->
            if (result.isWorking) {
                return@observe
            }

            if (result.playlistCreated) {
                showToast(getString(R.string.created_playlist_x, result.playlistName))
                callback?.playlistCreated()
                dismiss()
            } else {
                showToast(getString(R.string.playlist_exists, result.playlistName))
            }
        }
    }

    fun callback(playlistCreatedCallback: PlaylistCreatedCallback) =
        apply { this.callback = playlistCreatedCallback }

    interface PlaylistCreatedCallback {
        fun playlistCreated()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun create(song: Song) = create(listOf(song))

        fun create(songs: List<Song>? = null): CreatePlaylistDialog {
            if (songs.isNullOrEmpty()) {
                return CreatePlaylistDialog()
            }
            return CreatePlaylistDialog().withArgs {
                putParcelableArrayList(EXTRA_SONGS, ArrayList(songs))
            }
        }
    }
}
