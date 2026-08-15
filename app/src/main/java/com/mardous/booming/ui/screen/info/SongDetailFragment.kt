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
package com.mardous.booming.ui.screen.info

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.data.local.EditTarget
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.isLandscape
import com.mardous.booming.presentation.screens.SongInfoScreen
import com.mardous.booming.presentation.theme.BoomingMusicTheme
import com.mardous.booming.ui.component.base.AbsTagEditorActivity
import com.mardous.booming.ui.component.base.goToDestination
import com.mardous.booming.ui.screen.lyrics.LyricsEditorFragmentArgs
import com.mardous.booming.ui.screen.tageditor.SongTagEditorActivity

class SongDetailFragment : BottomSheetDialogFragment() {

    private val navArgs: SongDetailFragmentArgs by navArgs()
    private val song: Song get() = navArgs.extraSong

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (isLandscape()) {
            (dialog as? BottomSheetDialog)?.let {
                it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
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
                    SongInfoScreen(
                        song = song,
                        onLyricsEditorClick = {
                            goToDestination(
                                requireActivity(),
                                R.id.nav_lyrics_editor,
                                LyricsEditorFragmentArgs.Builder(song)
                                    .build()
                                    .toBundle()
                            )
                        },
                        onTagEditorClick = {
                            val tagEditor =
                                Intent(requireContext(), SongTagEditorActivity::class.java)
                            tagEditor.putExtra(
                                AbsTagEditorActivity.EXTRA_TARGET,
                                EditTarget.song(song)
                            )
                            startActivity(tagEditor)
                        }
                    )
                }
            }
        }
    }
}