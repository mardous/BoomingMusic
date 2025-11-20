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

package com.mardous.booming.ui.screen.player.styles.defaultstyle

import android.animation.Animator
import android.animation.TimeInterpolator
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mardous.booming.R
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.core.model.player.PlayerColorScheme
import com.mardous.booming.core.model.player.PlayerTintTarget
import com.mardous.booming.core.model.player.iconButtonTintTarget
import com.mardous.booming.core.model.player.tintTarget
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentDefaultPlayerPlaybackControlsBinding
import com.mardous.booming.extensions.resources.centerPivot
import com.mardous.booming.extensions.resources.showBounceAnimation
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_NEXT
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_PREVIOUS
import com.mardous.booming.ui.component.views.MusicSlider
import com.mardous.booming.ui.screen.player.PlayerAnimator
import com.mardous.booming.util.DISPLAY_NEXT_SONG
import com.mardous.booming.util.Preferences
import java.util.LinkedList

/**
 * @author Christians M. A. (mardous)
 */
class DefaultPlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_default_player_playback_controls) {

    private var _binding: FragmentDefaultPlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val playPauseFab: FloatingActionButton
        get() = binding.playPauseButton

    override val repeatButton: MaterialButton?
        get() = binding.repeatButton

    override val shuffleButton: MaterialButton?
        get() = binding.shuffleButton

    override val musicSlider: MusicSlider?
        get() = binding.progressSlider

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    override val songTitleView: TextView?
        get() = binding.title

    override val songArtistView: TextView?
        get() = binding.text

    override val songInfoView: TextView?
        get() = binding.songInfo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDefaultPlayerPlaybackControlsBinding.bind(view)
        binding.playPauseButton.doOnLayout { it.centerPivot() }
        binding.playPauseButton.setOnClickListener(this)
        binding.shuffleButton.setOnClickListener(this)
        binding.repeatButton.setOnClickListener(this)
        binding.nextButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_NEXT))
        binding.previousButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_PREVIOUS))

        setupQueueInfoView()
    }

    override fun onCreatePlayerAnimator(): PlayerAnimator {
        return DefaultPlayerAnimator(binding, Preferences.animateControls)
    }

    override fun onSongInfoChanged(currentSong: Song, nextSong: Song) {
        _binding?.let { nonNullBinding ->
            nonNullBinding.title.text = currentSong.title
            nonNullBinding.text.text = getSongArtist(currentSong)
            nonNullBinding.queueInfo.text = getNextSongInfo(nextSong)
        }
    }

    override fun onExtraInfoChanged(extraInfo: String?) {
        _binding?.let { nonNullBinding ->
            if (isExtraInfoEnabled()) {
                nonNullBinding.songInfo?.text = extraInfo
                nonNullBinding.songInfo?.isVisible = true
            } else {
                nonNullBinding.songInfo?.isVisible = false
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
            .rotation(360f)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun onHide() {
        super.onHide()
        binding.playPauseButton.apply {
            scaleX = 0f
            scaleY = 0f
            rotation = 0f
        }
    }

    override fun onClick(view: View) {
        super.onClick(view)
        when (view) {
            binding.repeatButton -> playerViewModel.cycleRepeatMode()
            binding.shuffleButton -> playerViewModel.toggleShuffleMode()
            binding.playPauseButton -> {
                playerViewModel.togglePlayPause()
                view.showBounceAnimation()
            }
        }
    }

    private fun setupQueueInfoView() {
        _binding?.let { binding ->
            if (Preferences.isShowNextSong) {
                binding.queueInfo.visibility = View.VISIBLE
                setViewAction(binding.queueInfo, NowPlayingAction.OpenPlayQueue)
            } else {
                binding.queueInfo.visibility = View.GONE
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        when(key) {
            DISPLAY_NEXT_SONG -> {
                setupQueueInfoView()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            binding.songInfo?.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor),
            binding.songCurrentProgress.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor),
            binding.songTotalTime.tintTarget(oldSecondaryTextColor, scheme.secondaryTextColor)
        )
    }

    private class DefaultPlayerAnimator(
        private val binding: FragmentDefaultPlayerPlaybackControlsBinding,
        isEnabled: Boolean
    ) : PlayerAnimator(isEnabled) {
        override fun onAddAnimation(animators: LinkedList<Animator>, interpolator: TimeInterpolator) {
            addScaleAnimation(animators, binding.shuffleButton, interpolator, 100)
            addScaleAnimation(animators, binding.repeatButton, interpolator, 100)
            addScaleAnimation(animators, binding.previousButton, interpolator, 200)
            addScaleAnimation(animators, binding.nextButton, interpolator, 200)
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
