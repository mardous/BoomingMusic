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

package com.mardous.booming.ui.screen.player.styles.peekplayerstyle

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.color.MaterialColors
import com.mardous.booming.R
import com.mardous.booming.core.model.player.*
import com.mardous.booming.core.model.theme.NowPlayingScreen
import com.mardous.booming.databinding.FragmentPeekPlayerBinding
import com.mardous.booming.extensions.getOnBackPressedDispatcher
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.AbsPlayerFragment
import com.mardous.booming.util.Preferences

/**
 * @author Christians M. A. (mardous)
 */
class PeekPlayerFragment : AbsPlayerFragment(R.layout.fragment_peek_player) {

    private var _binding: FragmentPeekPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var controlsFragment: PeekPlayerControlsFragment

    override val playerControlsFragment: AbsPlayerControlsFragment
        get() = controlsFragment

    override val colorSchemeMode: PlayerColorSchemeMode
        get() = Preferences.getNowPlayingColorSchemeMode(NowPlayingScreen.Peek)

    override val playerToolbar: Toolbar
        get() = binding.playerToolbar

    private var primaryControlColor: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPeekPlayerBinding.bind(view)
        setupToolbar()
        inflateMenuInView(playerToolbar)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.songInfo) { v: View, insets: WindowInsetsCompat ->
            val navigationBar = insets.getInsets(Type.systemBars())
            v.updatePadding(bottom = navigationBar.bottom)
            insets
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.currentSongFlow.collect { currentSong ->
                _binding?.let { nonNullBinding ->
                    nonNullBinding.title.text = currentSong.title
                    nonNullBinding.text.text = getSongArtist(currentSong)
                }
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.extraInfoFlow.collect { extraInfo ->
                _binding?.let { nonNullBinding ->
                    if (isExtraInfoEnabled()) {
                        nonNullBinding.songInfo.text = extraInfo
                        nonNullBinding.songInfo.isVisible = true
                    } else {
                        nonNullBinding.songInfo.isVisible = false
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        playerToolbar.setNavigationOnClickListener {
            getOnBackPressedDispatcher().onBackPressed()
        }
    }

    override fun onMenuInflated(menu: Menu) {
        super.onMenuInflated(menu)
        menu.setShowAsAction(R.id.action_favorite)
        menu.setShowAsAction(R.id.action_playing_queue)
        menu.setShowAsAction(R.id.action_show_lyrics)
    }

    override fun onCreateChildFragments() {
        super.onCreateChildFragments()
        controlsFragment = whichFragment(R.id.playbackControlsFragment)
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldPrimaryControlColor = this.primaryControlColor
        primaryControlColor = scheme.onSurfaceColor
        val oldPrimaryTextColor = binding.title.currentTextColor
        val oldSecondaryTextColor = binding.text.currentTextColor
        val newSurfaceColor = if (scheme.mode == PlayerColorSchemeMode.AppTheme) {
            MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceContainerLow)
        } else {
            scheme.surfaceColor
        }
        return mutableListOf(
            binding.root.surfaceTintTarget(newSurfaceColor),
            binding.playerToolbar.tintTarget(oldPrimaryControlColor, primaryControlColor),
            binding.title.tintTarget(oldPrimaryTextColor, scheme.onSurfaceColor),
            binding.text.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor),
            binding.songInfo.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor)
        ).also {
            it.addAll(playerControlsFragment.getTintTargets(scheme))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}