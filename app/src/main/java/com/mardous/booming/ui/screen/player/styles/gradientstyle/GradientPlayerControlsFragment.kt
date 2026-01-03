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

package com.mardous.booming.ui.screen.player.styles.gradientstyle

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.mardous.booming.R
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.core.model.player.PlayerColorScheme
import com.mardous.booming.core.model.player.PlayerTintTarget
import com.mardous.booming.core.model.player.iconButtonTintTarget
import com.mardous.booming.core.model.player.tintTarget
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentGradientPlayerPlaybackControlsBinding
import com.mardous.booming.extensions.isLandscape
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_NEXT
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_PREVIOUS
import com.mardous.booming.ui.component.views.MusicSlider

class GradientPlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_gradient_player_playback_controls) {

    private var _binding: FragmentGradientPlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val musicSlider: MusicSlider?
        get() = binding.progressSlider

    override val repeatButton: MaterialButton
        get() = binding.repeatButton

    override val shuffleButton: MaterialButton
        get() = binding.shuffleButton

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    override val songTitleView: TextView?
        get() = binding.title

    override val songArtistView: TextView?
        get() = binding.text

    override val songInfoView: TextView
        get() = binding.songInfo

    private var isFavorite: Boolean = false
    private var popupMenu: PopupMenu? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGradientPlayerPlaybackControlsBinding.bind(view)
        setupListeners()
        setViewAction(binding.favorite, NowPlayingAction.ToggleFavoriteState)
        popupMenu = playerFragment?.inflateMenuInView(binding.menu)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            if (view.resources.isLandscape) {
                val systemBars = insets.getInsets(Type.systemBars())
                v.updatePadding(top = systemBars.top)
            }
            insets
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.playPauseButton.setOnClickListener(this)
        binding.shuffleButton.setOnClickListener(this)
        binding.repeatButton.setOnClickListener(this)
        binding.nextButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_NEXT))
        binding.previousButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_PREVIOUS))
    }

    override fun onClick(view: View) {
        super.onClick(view)
        when (view) {
            binding.shuffleButton -> playerViewModel.toggleShuffleMode()
            binding.repeatButton -> playerViewModel.cycleRepeatMode()
            binding.playPauseButton -> playerViewModel.togglePlayPause()
        }
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldControlColor = binding.nextButton.iconTint.defaultColor
        val oldSliderColor = binding.progressSlider.currentColor
        val oldPrimaryTextColor = binding.title.currentTextColor
        val oldSecondaryTextColor = binding.text.currentTextColor

        val oldShuffleColor = getPlaybackControlsColor(isShuffleModeOn)
        val newShuffleColor = getPlaybackControlsColor(
            isShuffleModeOn,
            scheme.onSurfaceColor,
            scheme.onSurfaceVariantColor
        )
        val oldRepeatColor = getPlaybackControlsColor(isRepeatModeOn)
        val newRepeatColor = getPlaybackControlsColor(
            isRepeatModeOn,
            scheme.onSurfaceColor,
            scheme.onSurfaceVariantColor
        )

        return listOfNotNull(
            binding.progressSlider.progressView?.tintTarget(oldSliderColor, scheme.onSurfaceColor),
            binding.menu.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.favorite.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.playPauseButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.nextButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.previousButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.shuffleButton.iconButtonTintTarget(oldShuffleColor, newShuffleColor),
            binding.repeatButton.iconButtonTintTarget(oldRepeatColor, newRepeatColor),
            binding.title.tintTarget(oldPrimaryTextColor, scheme.onSurfaceColor),
            binding.text.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor),
            binding.songInfo.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor),
            binding.songCurrentProgress.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor),
            binding.songTotalTime.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor)
        )
    }

    override fun onSongInfoChanged(currentSong: Song, nextSong: Song) {
        _binding?.let { nonNullBinding ->
            nonNullBinding.title.text = currentSong.title
            nonNullBinding.text.text = getSongArtist(currentSong)
        }
    }

    override fun onExtraInfoChanged(extraInfo: String?) {
        _binding?.let { nonNullBinding ->
            if (isExtraInfoEnabled()) {
                nonNullBinding.songInfo.text = extraInfo
                nonNullBinding.songInfo.isVisible = true
            } else {
                nonNullBinding.songInfo.isVisible = false
            }
        }
    }

    override fun onUpdatePlayPause(isPlaying: Boolean) {
        if (isPlaying) {
            _binding?.playPauseButton?.setIconResource(R.drawable.ic_pause_24dp)
        } else {
            _binding?.playPauseButton?.setIconResource(R.drawable.ic_play_24dp)
        }
    }

    internal fun setFavorite(isFavorite: Boolean, withAnimation: Boolean) {
        if (this.isFavorite != isFavorite) {
            this.isFavorite = isFavorite
            playerFragment?.let { nonNullPlayerFragment ->
                with(nonNullPlayerFragment) {
                    binding.favorite.setIsFavorite(isFavorite, withAnimation)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}