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
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import coil3.load
import coil3.size.Precision
import coil3.size.Scale
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.coil.DEFAULT_PLAYLIST_IMAGE
import com.mardous.booming.coil.placeholderDrawableRes
import com.mardous.booming.data.local.room.PlaylistEntity
import com.mardous.booming.databinding.DialogCreatePlaylistBinding
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.extensions.showToast
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.library.ReloadType
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author SifouByte
 */
class EditPlaylistDialog : DialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val playlistEntity: PlaylistEntity by extraNotNull(EXTRA_PLAYLIST)

    private var _binding: DialogCreatePlaylistBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: String? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            selectedImageUri = it.toString()
            loadImage(it)
        }
    }

    private fun loadImage(uri: Uri?) {
        binding.playlistCoverImage.load(uri) {
            placeholderDrawableRes(binding.playlistCoverImage.context, DEFAULT_PLAYLIST_IMAGE)
            precision(Precision.INEXACT)
            scale(Scale.FILL)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreatePlaylistBinding.inflate(layoutInflater)

        // Pre-fill existing data
        binding.playlistNameEditText.setText(playlistEntity.playlistName)
        binding.playlistDescriptionEditText.setText(playlistEntity.description ?: "")

        // Set initial image
        selectedImageUri = playlistEntity.customCoverUri
        if (!playlistEntity.customCoverUri.isNullOrEmpty()) {
            loadImage(playlistEntity.customCoverUri?.toUri())
        } else {
            // Load default icon for playlists without custom covers
            loadImage(null)
        }

        binding.selectCoverFab.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_edit_playlist)
            .setView(binding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val playlistName = binding.playlistNameEditText.text?.toString()?.trim()
                val description = binding.playlistDescriptionEditText.text?.toString()?.trim()

                if (playlistName.isNullOrEmpty()) {
                    showToast(R.string.playlist_name_cannot_be_empty)
                    return@setPositiveButton
                }

                libraryViewModel.updatePlaylist(
                    playlistId = playlistEntity.playListId,
                    newName = playlistName,
                    customCoverUri = selectedImageUri,
                    description = description?.ifEmpty { null }
                )
                
                libraryViewModel.forceReload(ReloadType.Playlists)
                showToast(R.string.playlist_updated)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val EXTRA_PLAYLIST = "extra_playlist"

        fun create(playlist: PlaylistEntity) =
            EditPlaylistDialog().withArgs { putParcelable(EXTRA_PLAYLIST, playlist) }
    }
}
