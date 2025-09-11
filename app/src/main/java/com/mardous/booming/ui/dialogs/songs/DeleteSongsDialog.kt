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

package com.mardous.booming.ui.dialogs.songs

import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.DialogInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.DialogDeleteSongsBinding
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.files.isSAFAccessGranted
import com.mardous.booming.extensions.files.isSAFRequiredForSongs
import com.mardous.booming.ui.dialogs.SAFDialog
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.util.MusicUtil
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians M. A. (mardous)
 */
class DeleteSongsDialog : DialogFragment(), SAFDialog.SAFResultListener {

    private val libraryViewModel: LibraryViewModel by activityViewModel()

    private lateinit var songs: MutableList<Song>

    private var binding: DialogDeleteSongsBinding? = null
    private var songsToDelete: List<Song>? = null

    override fun onSAFResult(treeUri: Uri?) {
        if (treeUri != null) {
            onDeleteSongs(songsToDelete)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun createDeleteRequest(uris: List<Uri>): PendingIntent {
        return if (Preferences.trashMusicFiles) {
            MediaStore.createTrashRequest(requireActivity().contentResolver, uris, true)
        } else {
            MediaStore.createDeleteRequest(requireActivity().contentResolver, uris)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        songs = BundleCompat.getParcelableArrayList(requireArguments(), EXTRA_SONGS, Song::class.java)!!
            .distinct()
            .toMutableList()

        if (hasR()) {
            val deleteResultLauncher =
                registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        libraryViewModel.deleteSongs(songs)
                        dismiss()
                    } else {
                        dismiss()
                    }
                }

            val pendingIntent = createDeleteRequest(songs.map { it.mediaStoreUri })
            deleteResultLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            return MaterialAlertDialogBuilder(requireContext())
                .setView(R.layout.dialog_deleting_songs)
                .create()
        } else {
            val titleRes: Int
            val message: CharSequence
            if (songs.size == 1) {
                titleRes = R.string.delete_song_title
                message = getString(R.string.delete_the_song_x, songs[0].title)
            } else {
                titleRes = R.string.delete_songs_title
                message = getString(R.string.delete_x_songs, songs.size)
            }
            return MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes).also {
                    binding = DialogDeleteSongsBinding.inflate(layoutInflater.cloneInContext(it.context))
                    binding!!.message.text = message
                }
                .setView(binding!!.root)
                .setPositiveButton(R.string.delete_action, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .create { dialog ->
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                        onStartDeletion(songs)
                    }
                }
        }
    }

    private fun onStartDeletion(songsToDelete: List<Song>) {
        this.songsToDelete = ArrayList(songsToDelete)
        if (!songsToDelete.isSAFRequiredForSongs()) {
            onDeleteSongs(songsToDelete)
        } else {
            if (requireContext().isSAFAccessGranted()) {
                onDeleteSongs(songsToDelete)
            } else {
                SAFDialog.show(this)
            }
        }
    }

    private fun onDeleteSongs(songs: List<Song>?) {
        if (songs.isNullOrEmpty())
            return

        lifecycleScope.launch {
            requireAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = false
            requireAlertDialog().getButton(DialogInterface.BUTTON_NEGATIVE)?.isEnabled = false

            binding?.let {
                it.message.setText(R.string.deleting_songs)
                it.progress.isVisible = true
                it.progressText.isVisible = true
            }

            withContext(IO) {
                try {
                    MusicUtil.deleteTracks(
                        requireContext(), songs,
                        onProgress = { song: Song, progress: Int, max: Int ->
                            requestContext { context ->
                                binding?.let { nonNullBinding ->
                                    nonNullBinding.progressText.text =
                                        context.getString(
                                            R.string.song_x_of_x,
                                            progress,
                                            max,
                                            song.title
                                        )
                                    nonNullBinding.progress.isIndeterminate = false
                                    nonNullBinding.progress.progress = progress
                                    nonNullBinding.progress.max = max
                                }
                            }
                        },
                        onCompleted = { deleted ->
                            requestContext {
                                it.showToast(
                                    if (deleted == 1) it.getString(R.string.deleted_one_song)
                                    else it.getString(R.string.deleted_x_songs, deleted)
                                )
                                dismissAllowingStateLoss()
                            }
                        })
                } catch (t: Throwable) {
                    Log.e("DeleteSongsDialog", "${songs.size} song deletion failed", t)
                }
            }
        }
    }

    companion object {
        fun create(song: Song): DeleteSongsDialog {
            return create(listOf(song))
        }

        fun create(songs: List<Song>): DeleteSongsDialog {
            return DeleteSongsDialog().withArgs {
                putParcelableArrayList(EXTRA_SONGS, ArrayList(songs))
            }
        }
    }
}