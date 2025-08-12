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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.mardous.booming.R
import com.mardous.booming.databinding.FragmentFullCoverPlayerBinding
import com.mardous.booming.extensions.getOnBackPressedDispatcher
import com.mardous.booming.extensions.glide.DEFAULT_SONG_IMAGE
import com.mardous.booming.extensions.glide.getSongGlideModel
import com.mardous.booming.extensions.glide.songOptions
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.resources.applyColor
import com.mardous.booming.extensions.resources.getPrimaryTextColor
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.ui.screen.player.PlayerColorScheme
import com.mardous.booming.ui.screen.player.PlayerColorSchemeMode
import com.mardous.booming.ui.screen.player.PlayerTintTarget
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.AbsPlayerFragment
import com.mardous.booming.ui.screen.player.tintTarget
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.data.model.Song
import com.mardous.booming.core.model.theme.NowPlayingScreen
import com.mardous.booming.util.Preferences

/**
 * @author Christians M. A. (mardous)
 */
class FullCoverPlayerFragment : AbsPlayerFragment(R.layout.fragment_full_cover_player),
    View.OnClickListener {

    private var _binding: FragmentFullCoverPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var controlsFragment: FullCoverPlayerControlsFragment

    private var playbackControlsColor = 0
    private var disabledPlaybackControlsColor = 0

    private var target: Target<Bitmap>? = null

    override val colorSchemeMode: PlayerColorSchemeMode
        get() = Preferences.getNowPlayingColorSchemeMode(NowPlayingScreen.FullCover)

    override val playerControlsFragment: AbsPlayerControlsFragment
        get() = controlsFragment

    override fun onAttach(context: Context) {
        super.onAttach(context)
        playbackControlsColor = getPrimaryTextColor(context, isDark = false)
        disabledPlaybackControlsColor = getPrimaryTextColor(context, isDark = false, isDisabled = true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFullCoverPlayerBinding.bind(view)
        setupColors()
        setupListeners()
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarContainer) { v: View, insets: WindowInsetsCompat ->
            val statusBar = insets.getInsets(Type.systemBars())
            v.updatePadding(left = statusBar.left, top = statusBar.top, right = statusBar.right)
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            insets
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.nextSongFlow.collect {
                updateNextSongInfo(it)
            }
        }
    }

    override fun onPrepareViewGestures(view: View) {}

    private fun setupColors() {
        binding.nextSongLabel.setTextColor(disabledPlaybackControlsColor)
        binding.nextSongText.setTextColor(playbackControlsColor)
        binding.close.applyColor(playbackControlsColor, isIconButton = true)
    }

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

    private fun updateNextSongInfo(nextSong: Song) {
        Glide.with(this).clear(target)
        if (nextSong != Song.emptySong) {
            _binding?.nextSongText?.text = nextSong.title
            target = _binding?.nextSongAlbumArt?.let {
                Glide.with(this)
                    .asBitmap()
                    .load(nextSong.getSongGlideModel())
                    .songOptions(nextSong)
                    .into(it)
            }
        } else {
            _binding?.nextSongText?.setText(R.string.now_playing)
            _binding?.nextSongAlbumArt?.setImageResource(DEFAULT_SONG_IMAGE)
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