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

package com.mardous.booming.fragments.player.styles.peek2playerstyle

import android.animation.Animator
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.mardous.booming.R
import com.mardous.booming.databinding.FragmentPeek2PlayerPlaybackControlsBinding
import com.mardous.booming.databinding.FragmentPeekPlayerPlaybackControlsBinding
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.fragments.player.*
import com.mardous.booming.fragments.player.base.AbsPlayerControlsFragment
import com.mardous.booming.model.Song
import com.mardous.booming.util.Preferences
import com.mardous.booming.views.MusicSlider
import java.util.LinkedList

/**
 * @author Christians M. A. (mardous)
 */
class Peek2PlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_peek2_player_playback_controls) {

    private var _binding: FragmentPeek2PlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val repeatButton: MaterialButton?
        get() = _binding?.repeatButton

    override val shuffleButton: MaterialButton?
        get() = _binding?.shuffleButton

    override val musicSlider: MusicSlider
        get() = binding.progressSlider

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPeek2PlayerPlaybackControlsBinding.bind(view)
        binding.playPauseButton.setOnClickListener(this)
        binding.nextButton.setOnClickListener(this)
        binding.previousButton.setOnClickListener(this)
        binding.shuffleButton.setOnClickListener(this)
        binding.repeatButton.setOnClickListener(this)

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.currentSongFlow.collect { song ->
                _binding?.let { nonNullBinding ->
                    nonNullBinding.title.text = song.title
                    nonNullBinding.text.text = getSongArtist(song)
                }
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.extraInfoFlow.collect {
                _binding?.let { nonNullBinding ->
                    if (isExtraInfoEnabled()) {
                        nonNullBinding.songInfo.text = it
                        nonNullBinding.songInfo.isVisible = true
                    } else {
                        nonNullBinding.songInfo.isVisible = false
                    }
                }
            }
        }
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldPrimaryTextColor = binding.title.currentTextColor
        val oldSecondaryTextColor = binding.text.currentTextColor
        val oldControlColor = binding.nextButton.iconTint.defaultColor
        val oldSliderColor = binding.progressSlider.currentColor
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
            binding.progressSlider.progressView?.tintTarget(oldSliderColor, scheme.emphasisColor),
            binding.songCurrentProgress.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor),
            binding.songTotalTime.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor),
            binding.playPauseButton.iconButtonTintTarget(oldControlColor, scheme.primaryControlColor),
            binding.nextButton.iconButtonTintTarget(oldControlColor, scheme.primaryControlColor),
            binding.previousButton.iconButtonTintTarget(oldControlColor, scheme.primaryControlColor),
            binding.shuffleButton.iconButtonTintTarget(oldShuffleColor, newShuffleColor),
            binding.repeatButton.iconButtonTintTarget(oldRepeatColor, newRepeatColor),
            binding.title.tintTarget(oldPrimaryTextColor, scheme.primaryTextColor),
            binding.text.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor),
            binding.songInfo.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor),
        )
    }

    override fun onCreatePlayerAnimator(): PlayerAnimator {
        return Peek2PlayerAnimator(binding, Preferences.animateControls)
    }

    override fun onSongInfoChanged(song: Song) {}

    override fun onExtraInfoChanged(extraInfo: String?) {}

    override fun onQueueInfoChanged(newInfo: String?) {}

    override fun onUpdatePlayPause(isPlaying: Boolean) {
        if (isPlaying) {
            _binding?.playPauseButton?.setIconResource(R.drawable.ic_pause_24dp)
        } else {
            _binding?.playPauseButton?.setIconResource(R.drawable.ic_play_24dp)
        }
    }

    override fun onClick(view: View) {
        super.onClick(view)
        when (view) {
            binding.nextButton -> playerViewModel.playNext()
            binding.previousButton -> playerViewModel.playPrevious()
            binding.repeatButton -> playerViewModel.cycleRepeatMode()
            binding.shuffleButton -> playerViewModel.toggleShuffleMode()
            binding.playPauseButton -> playerViewModel.togglePlayPause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class Peek2PlayerAnimator(
        private val binding: FragmentPeek2PlayerPlaybackControlsBinding,
        isEnabled: Boolean
    ) : PlayerAnimator(isEnabled) {
        override fun onAddAnimation(animators: LinkedList<Animator>, interpolator: TimeInterpolator) {
            addScaleAnimation(animators, binding.playPauseButton, interpolator, 100)
            addScaleAnimation(animators, binding.previousButton, interpolator, 200)
            addScaleAnimation(animators, binding.nextButton, interpolator, 200)
            addScaleAnimation(animators, binding.shuffleButton, interpolator, 300)
            addScaleAnimation(animators, binding.repeatButton, interpolator, 300)
            addScaleAnimation(animators, binding.songCurrentProgress, interpolator, 400)
            addScaleAnimation(animators, binding.songTotalTime, interpolator, 400)
        }

        override fun onPrepareForAnimation() {
            prepareForScaleAnimation(binding.playPauseButton)
            prepareForScaleAnimation(binding.previousButton)
            prepareForScaleAnimation(binding.nextButton)
            prepareForScaleAnimation(binding.shuffleButton)
            prepareForScaleAnimation(binding.repeatButton)
            prepareForScaleAnimation(binding.songCurrentProgress)
            prepareForScaleAnimation(binding.songTotalTime)
        }
    }
}