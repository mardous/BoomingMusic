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
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mardous.booming.R
import com.mardous.booming.core.model.player.*
import com.mardous.booming.core.model.player.PlayerColorScheme.Mode
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentPlainPlayerPlaybackControlsBinding
import com.mardous.booming.extensions.resources.withAlpha
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
            if (playerViewModel.colorScheme.mode == Mode.VibrantGradient) scheme.toolbarColor else scheme.onSurfaceColor,
            if (playerViewModel.colorScheme.mode == Mode.VibrantGradient) scheme.toolbarColor.withAlpha(0.45f) else scheme.onSurfaceVariantColor
        )
        val oldRepeatColor = getPlaybackControlsColor(isRepeatModeOn)
        val newRepeatColor = getPlaybackControlsColor(
            isRepeatModeOn,
            if (playerViewModel.colorScheme.mode == Mode.VibrantGradient) scheme.toolbarColor else scheme.onSurfaceColor,
            if (playerViewModel.colorScheme.mode == Mode.VibrantGradient) scheme.toolbarColor.withAlpha(0.45f) else scheme.onSurfaceVariantColor
        )
        val oldPlayPauseColor = binding.playPauseButton.backgroundTintList?.defaultColor ?: oldControlColor
        val newEmphasisColor = if (scheme.mode == PlayerColorSchemeMode.VibrantColor) {
            scheme.onSurfaceColor
        } else {
            if (scheme.mode == PlayerColorSchemeMode.VibrantGradient) {
                scheme.toolbarColor
            }else {
                scheme.primaryColor
            }
        }

        if (scheme.mode == Mode.VibrantGradient) {
            binding.playPauseButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.vibrant_shadow_black))
        }else {
            binding.playPauseButton.setColorFilter(scheme.surfaceColor)
        }

        return listOfNotNull(
            binding.progressSlider.progressView?.tintTarget(oldSliderColor, newEmphasisColor),
            binding.songCurrentProgress.tintTarget(oldSecondaryTextColor, newEmphasisColor),
            binding.songTotalTime.tintTarget(oldSecondaryTextColor, newEmphasisColor),
            binding.songInfo.tintTarget(oldSecondaryTextColor, scheme.toolbarColor),
            binding.playPauseButton.iconButtonTintTarget(oldPlayPauseColor, newEmphasisColor),
            binding.nextButton.iconButtonTintTarget(oldControlColor, scheme.toolbarColor),
            binding.previousButton.iconButtonTintTarget(oldControlColor, scheme.toolbarColor),
            binding.shuffleButton.iconButtonTintTarget(oldShuffleColor, newShuffleColor),
            binding.repeatButton.iconButtonTintTarget(oldRepeatColor, newRepeatColor),
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

        if (playerViewModel.colorScheme.mode == PlayerColorScheme.Mode.VibrantGradient) {
            binding.playPauseButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.vibrant_shadow_black))
        }else {
            binding.playPauseButton.setColorFilter(playerViewModel.colorScheme.surfaceColor)
        }
    }

    override fun onShow() {
        super.onShow()
        if (Preferences.animateControls) {
            binding.playPauseButton.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(BounceInterpolator())
                .start()
        }
    }

    override fun onHide() {
        super.onHide()
        binding.playPauseButton.apply {
            scaleX = if (Preferences.animateControls) 0f else 1f
            scaleY = if (Preferences.animateControls) 0f else 1f
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
