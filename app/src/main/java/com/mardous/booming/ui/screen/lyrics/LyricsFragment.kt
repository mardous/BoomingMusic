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
package com.mardous.booming.ui.screen.lyrics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mardous.booming.R
import com.mardous.booming.extensions.keepScreenOn
import com.mardous.booming.extensions.materialSharedAxis
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians M. A. (mardous)
 */
class LyricsFragment : Fragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private val playerViewModel: PlayerViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BoomingMusicTheme {
                    LyricsScreen(
                        libraryViewModel = libraryViewModel,
                        lyricsViewModel = lyricsViewModel,
                        playerViewModel = playerViewModel,
                        onEditClick = {
                            val currentSong = playerViewModel.currentSong
                            findNavController().navigate(
                                R.id.nav_lyrics_editor,
                                LyricsEditorFragmentArgs.Builder(currentSong)
                                    .build()
                                    .toBundle()
                            )
                        }
                    )
                }
            }
        }.also {
            materialSharedAxis(it)
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().keepScreenOn(true)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().keepScreenOn(false)
    }
}