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
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.data.local.room.SongEntity
import com.mardous.booming.extensions.EXTRA_SONGS
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.extensions.toHtml
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.screen.library.LibraryViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class RemoveFromPlaylistDialog : DialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val songs: List<SongEntity> by extraNotNull(EXTRA_SONGS)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title: Int
        val content: CharSequence
        if (songs.size > 1) {
            title = R.string.remove_songs_from_playlist_title
            content = getString(R.string.remove_x_songs_from_playlist, songs.size).toHtml()
        } else {
            title = R.string.remove_song_from_playlist_title
            content = getString(R.string.remove_song_x_from_playlist, songs[0].title).toHtml()
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton(R.string.remove_action) { _: DialogInterface, _: Int ->
                libraryViewModel.deleteSongsInPlaylist(songs)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    companion object {
        fun create(song: SongEntity) = create(listOf(song))

        fun create(songs: List<SongEntity>) = RemoveFromPlaylistDialog().withArgs {
            putParcelableArrayList(EXTRA_SONGS, ArrayList(songs))
        }
    }
}