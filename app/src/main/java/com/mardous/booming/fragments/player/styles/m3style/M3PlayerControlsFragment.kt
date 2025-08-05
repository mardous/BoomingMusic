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

package com.mardous.booming.fragments.player.styles.m3style

import android.animation.Animator
import android.animation.TimeInterpolator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mardous.booming.R
import com.mardous.booming.databinding.FragmentM3PlayerPlaybackControlsBinding
import com.mardous.booming.fragments.player.*
import com.mardous.booming.fragments.player.base.AbsPlayerControlsFragment
import com.mardous.booming.fragments.player.base.SkipButtonTouchHandler.Companion.DIRECTION_NEXT
import com.mardous.booming.fragments.player.base.SkipButtonTouchHandler.Companion.DIRECTION_PREVIOUS
import com.mardous.booming.model.Song
import com.mardous.booming.util.Preferences
import com.mardous.booming.views.MusicSlider
import java.util.LinkedList

/**
 * @author Christians M. A. (mardous)
 */
class M3PlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_m3_player_playback_controls) {

    private var _binding: FragmentM3PlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val playPauseFab: FloatingActionButton
        get() = binding.playPauseButton

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

    override val songInfoView: TextView?
        get() = binding.songInfo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentM3PlayerPlaybackControlsBinding.bind(view)
        binding.title.setOnClickListener(this)
        binding.text.setOnClickListener(this)
        binding.playPauseButton.setOnClickListener(this)
        binding.shuffleButton.setOnClickListener(this)
        binding.repeatButton.setOnClickListener(this)
        binding.nextButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_NEXT))
        binding.previousButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_PREVIOUS))
    }

    override fun onCreatePlayerAnimator(): PlayerAnimator {
        return M3PlayerAnimator(binding, Preferences.animateControls)
    }

    override fun onSongInfoChanged(song: Song) {
        _binding?.let { nonNullBinding ->
            nonNullBinding.title.text = song.title
            nonNullBinding.text.text = getSongArtist(song)
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

    override fun onQueueInfoChanged(newInfo: String?) {}

    override fun onUpdatePlayPause(isPlaying: Boolean) {
        if (isPlaying) {
            _binding?.playPauseButton?.setImageResource(R.drawable.ic_pause_m3_24dp)
        } else {
            _binding?.playPauseButton?.setImageResource(R.drawable.ic_play_m3_24dp)
        }
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldPlayPauseColor = binding.playPauseButton.backgroundTintList?.defaultColor
            ?: Color.TRANSPARENT

        val oldControlColor = binding.nextButton.iconTint.defaultColor
        val oldSliderColor = binding.progressSlider.currentColor
        val oldPrimaryTextColor = binding.title.currentTextColor
        val oldSecondaryTextColor = binding.text.currentTextColor

        val oldShuffleColor = getPlaybackControlsColor(isShuffleModeOn)
        val newShuffleColor = getPlaybackControlsColor(
            isShuffleModeOn,
            scheme.primaryControlColor,
            scheme.secondaryControlColor
        )
        val oldRepeatColor = getPlaybackControlsColor(isRepeatModeOn)
        val newRepeatColor = getPlaybackControlsColor(
            isRepeatModeOn,
            scheme.primaryControlColor,
            scheme.secondaryControlColor
        )

        return listOfNotNull(
            binding.playPauseButton.tintTarget(oldPlayPauseColor, scheme.emphasisColor),
            binding.progressSlider.progressView?.tintTarget(oldSliderColor, scheme.emphasisColor),
            binding.nextButton.iconButtonTintTarget(oldControlColor, scheme.primaryControlColor),
            binding.previousButton.iconButtonTintTarget(oldControlColor, scheme.primaryControlColor),
            binding.shuffleButton.iconButtonTintTarget(oldShuffleColor, newShuffleColor),
            binding.repeatButton.iconButtonTintTarget(oldRepeatColor, newRepeatColor),
            binding.title.tintTarget(oldPrimaryTextColor, scheme.primaryTextColor),
            binding.text.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor),
            binding.songInfo.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor),
            binding.songCurrentProgress.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor),
            binding.songTotalTime.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor)
        )
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

    private class M3PlayerAnimator(
        private val binding: FragmentM3PlayerPlaybackControlsBinding,
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