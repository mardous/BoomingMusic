/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.ui.screen.player.styles.plainstyle

import android.animation.Animator
import android.animation.TimeInterpolator
import android.os.Bundle
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mardous.booming.R
import com.mardous.booming.core.model.player.*
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentPlainPlayerPlaybackControlsBinding
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_NEXT
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_PREVIOUS
import com.mardous.booming.ui.component.views.MusicSlider
import com.mardous.booming.ui.screen.player.PlayerAnimator
import com.mardous.booming.util.Preferences
import java.util.LinkedList

/**
 * @author Christians M. A. (mardous)
 */
class PlainPlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_plain_player_playback_controls) {

    private var _binding: FragmentPlainPlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val playPauseFab: FloatingActionButton
        get() = binding.playPauseButton

    override val repeatButton: MaterialButton
        get() = binding.repeatButton

    override val shuffleButton: MaterialButton
        get() = binding.shuffleButton

    override val musicSlider: MusicSlider?
        get() = binding.progressSlider

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    override val songInfoView: TextView?
        get() = binding.songInfo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlainPlayerPlaybackControlsBinding.bind(view)
        binding.playPauseButton.setOnClickListener(this)
        binding.shuffleButton.setOnClickListener(this)
        binding.repeatButton.setOnClickListener(this)
        binding.nextButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_NEXT))
        binding.previousButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_PREVIOUS))
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldControlColor = binding.nextButton.iconTint.defaultColor
        val oldSliderColor = binding.progressSlider.currentColor
        val oldSecondaryTextColor = binding.songCurrentProgress.currentTextColor
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
        val oldPlayPauseColor = binding.playPauseButton.backgroundTintList?.defaultColor ?: oldControlColor
        val newEmphasisColor = if (scheme.mode == PlayerColorSchemeMode.VibrantColor) {
            scheme.onSurfaceColor
        } else {
            scheme.primaryColor
        }
        return listOfNotNull(
            binding.progressSlider.progressView?.tintTarget(oldSliderColor, newEmphasisColor),
            binding.songCurrentProgress.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor),
            binding.songTotalTime.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor),
            binding.songInfo.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor),
            binding.playPauseButton.iconButtonTintTarget(oldPlayPauseColor, newEmphasisColor),
            binding.nextButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.previousButton.iconButtonTintTarget(oldControlColor, scheme.onSurfaceColor),
            binding.shuffleButton.iconButtonTintTarget(oldShuffleColor, newShuffleColor),
            binding.repeatButton.iconButtonTintTarget(oldRepeatColor, newRepeatColor)
        )
    }

    override fun onCreatePlayerAnimator(): PlayerAnimator {
        return PlainPlayerAnimator(binding, Preferences.animateControls)
    }

    override fun onSongInfoChanged(currentSong: Song, nextSong: Song) {}

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
            _binding?.playPauseButton?.setImageResource(R.drawable.ic_pause_24dp)
        } else {
            _binding?.playPauseButton?.setImageResource(R.drawable.ic_play_24dp)
        }
    }

    override fun onShow() {
        super.onShow()
        binding.playPauseButton.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setInterpolator(BounceInterpolator())
            .start()
    }

    override fun onHide() {
        super.onHide()
        binding.playPauseButton.apply {
            scaleX = 0f
            scaleY = 0f
        }
    }

    override fun onClick(view: View) {
        super.onClick(view)
        when (view) {
            binding.repeatButton -> playerViewModel.cycleRepeatMode()
            binding.shuffleButton -> playerViewModel.toggleShuffleMode()
            binding.playPauseButton -> playerViewModel.togglePlayPause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class PlainPlayerAnimator(
        private val binding: FragmentPlainPlayerPlaybackControlsBinding,
        isEnabled: Boolean
    ) : PlayerAnimator(isEnabled) {
        override fun onAddAnimation(animators: LinkedList<Animator>, interpolator: TimeInterpolator) {
            addScaleAnimation(animators, binding.shuffleButton, interpolator, 100)
            addScaleAnimation(animators, binding.repeatButton, interpolator, 100)
            addScaleAnimation(animators, binding.previousButton, interpolator, 100)
            addScaleAnimation(animators, binding.nextButton, interpolator, 100)
            addScaleAnimation(animators, binding.songCurrentProgress, interpolator, 200)
            addScaleAnimation(animators, binding.songTotalTime, interpolator, 200)
        }

        override fun onPrepareForAnimation() {
            prepareForScaleAnimation(binding.previousButton)
            prepareForScaleAnimation(binding.nextButton)
            prepareForScaleAnimation(binding.shuffleButton)
            prepareForScaleAnimation(binding.repeatButton)
            prepareForScaleAnimation(binding.songCurrentProgress)
            prepareForScaleAnimation(binding.songTotalTime)
        }
    }
}
