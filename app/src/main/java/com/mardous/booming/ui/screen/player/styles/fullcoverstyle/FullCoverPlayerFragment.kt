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

package com.mardous.booming.ui.screen.player.styles.fullcoverstyle

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updatePadding
import com.mardous.booming.R
import com.mardous.booming.coil.DEFAULT_SONG_IMAGE
import com.mardous.booming.coil.songImage
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.core.model.theme.NowPlayingScreen
import com.mardous.booming.databinding.FragmentFullCoverPlayerBinding
import com.mardous.booming.extensions.getOnBackPressedDispatcher
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.AbsPlayerFragment
import com.mardous.booming.ui.component.views.getPlaceholderDrawable
import com.mardous.booming.ui.screen.player.PlayerColorScheme
import com.mardous.booming.ui.screen.player.PlayerColorSchemeMode
import com.mardous.booming.ui.screen.player.PlayerTintTarget
import com.mardous.booming.ui.screen.player.tintTarget
import com.mardous.booming.util.Preferences

/**
 * @author Christians M. A. (mardous)
 */
class FullCoverPlayerFragment : AbsPlayerFragment(R.layout.fragment_full_cover_player),
    View.OnClickListener {

    private var _binding: FragmentFullCoverPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var controlsFragment: FullCoverPlayerControlsFragment

    private var errorDrawable: Drawable? = null

    override val colorSchemeMode: PlayerColorSchemeMode
        get() = Preferences.getNowPlayingColorSchemeMode(NowPlayingScreen.FullCover)

    override val playerControlsFragment: AbsPlayerControlsFragment
        get() = controlsFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFullCoverPlayerBinding.bind(view)
        errorDrawable = view.context.getPlaceholderDrawable(DEFAULT_SONG_IMAGE)
        setupListeners()
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarContainer) { v: View, insets: WindowInsetsCompat ->
            val statusBar = insets.getInsets(Type.systemBars())
            v.updatePadding(left = statusBar.left, top = statusBar.top, right = statusBar.right)
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            insets
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.queueFlow.collect {
                if (it.nextSong != null) {
                    _binding?.nextSongAlbumArt?.songImage(it.nextSong)
                    _binding?.nextSongText?.text = it.nextSong.title
                } else {
                    _binding?.nextSongText?.setText(R.string.list_end)
                    _binding?.nextSongAlbumArt?.setImageDrawable(errorDrawable)
                }
            }
        }
    }

    override fun onPrepareViewGestures(view: View) {}

    private fun setupListeners() {
        binding.nextSongText.setOnClickListener(this)
        binding.nextSongAlbumArt.setOnClickListener(this)
        binding.close.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view) {
            binding.nextSongText, binding.nextSongAlbumArt -> onQuickActionEvent(NowPlayingAction.OpenPlayQueue)
            binding.close -> getOnBackPressedDispatcher().onBackPressed()
        }
    }

    override fun onMenuInflated(menu: Menu) {
        super.onMenuInflated(menu)
        menu.removeItem(R.id.action_favorite)
        menu.removeItem(R.id.action_playing_queue)
    }

    override fun onCreateChildFragments() {
        super.onCreateChildFragments()
        controlsFragment = whichFragment(R.id.playbackControlsFragment)
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldMaskColor = binding.mask.backgroundTintList?.defaultColor
            ?: Color.TRANSPARENT
        return mutableListOf(
            binding.mask.tintTarget(oldMaskColor, scheme.surfaceColor)
        ).also {
            it.addAll(playerControlsFragment.getTintTargets(scheme))
        }
    }

    override fun onIsFavoriteChanged(isFavorite: Boolean, withAnimation: Boolean) {
        controlsFragment.setFavorite(isFavorite, withAnimation)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}