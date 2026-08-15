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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.EXTRA_SONGS
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.presentation.screens.playlists.AddToPlaylistScreen
import com.mardous.booming.presentation.theme.BoomingMusicTheme

class AddToPlaylistDialog : BottomSheetDialogFragment() {

    private val songs by extraNotNull<List<Song>>(EXTRA_SONGS)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.let {
            it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                BoomingMusicTheme {
                    AddToPlaylistScreen(
                        onDismiss = { dismiss() },
                        onCreatePlaylistClick = {
                            val dialog = CreatePlaylistDialog.create(songs)
                            dialog.callback(object : CreatePlaylistDialog.PlaylistCreatedCallback {
                                override fun playlistCreated() {
                                    dismiss()
                                }
                            })
                            dialog.show(childFragmentManager, "CREATE_PLAYLIST")
                        },
                        songs = songs
                    )
                }
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

    companion object {
        fun create(song: Song) = create(listOf(song))

        fun create(songs: List<Song>): AddToPlaylistDialog {
            return AddToPlaylistDialog().withArgs {
                putParcelableArrayList(EXTRA_SONGS, ArrayList(songs))
            }
        }
    }
}