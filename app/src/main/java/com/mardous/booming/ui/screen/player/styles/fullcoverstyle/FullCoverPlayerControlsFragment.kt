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

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mardous.booming.R
import com.mardous.booming.databinding.FragmentFullCoverPlayerPlaybackControlsBinding
import com.mardous.booming.extensions.resources.showBounceAnimation
import com.mardous.booming.ui.screen.player.*
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_NEXT
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_PREVIOUS
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.data.model.Song
import com.mardous.booming.util.Preferences
import com.mardous.booming.ui.component.views.MusicSlider
import java.util.LinkedList

/**
 * @author Christians M. A. (mardous)
 */
class FullCoverPlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_full_cover_player_playback_controls) {

    private var _binding: FragmentFullCoverPlayerPlaybackControlsBinding? = null
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

    override val songInfoView: TextView
        get() = binding.songInfo

    private var isFavorite: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFullCoverPlayerPlaybackControlsBinding.bind(view)

        binding.title.setOnClickListener(this)
        binding.text.setOnClickListener(this)
        binding.playPauseButton.setOnClickListener(this)
        binding.shuffleButton.setOnClickListener(this)
        binding.repeatButton.setOnClickListener(this)
        binding.nextButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_NEXT))
        binding.previousButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_PREVIOUS))

        setViewAction(binding.favorite, NowPlayingAction.ToggleFavoriteState)
        playerFragment?.inflateMenuInView(binding.menu)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
            val navigationBar = insets.getInsets(Type.systemBars())
            v.updatePadding(bottom = navigationBar.bottom)
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            insets
        }
    }

    override fun onClick(view: View) {
        super.onClick(view)
        when (view) {
            binding.shuffleButton -> playerViewModel.toggleShuffleMode()
            binding.repeatButton -> playerViewModel.cycleRepeatMode()
            binding.playPauseButton -> {
                playerViewModel.togglePlayPause()
                view.showBounceAnimation()
            }
        }
    }

    override fun onCreatePlayerAnimator(): PlayerAnimator {
        return FullCoverPlayerAnimator(binding, Preferences.animateControls)
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
            binding.playPauseButton.tintTarget(oldPlayPauseColor, scheme.primaryControlColor),
            binding.progressSlider.progressView?.tintTarget(oldSliderColor, scheme.primaryControlColor),
            binding.menu.iconButtonTintTarget(oldControlColor, scheme.primaryControlColor),
            binding.favorite.iconButtonTintTarget(oldControlColor, scheme.primaryControlColor),
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
            _binding?.playPauseButton?.setImageResource(R.drawable.ic_pause_24dp)
        } else {
            _binding?.playPauseButton?.setImageResource(R.drawable.ic_play_24dp)
        }
    }

    internal fun setFavorite(isFavorite: Boolean, withAnimation: Boolean) {
        if (this.isFavorite != isFavorite) {
            this.isFavorite = isFavorite
            val iconRes = if (withAnimation) {
                if (isFavorite) R.drawable.avd_favorite else R.drawable.avd_unfavorite
            } else {
                if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
            }
            binding.favorite.setIconResource(iconRes)
            binding.favorite.icon?.let {
                if (it is AnimatedVectorDrawable) {
                    it.start()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class FullCoverPlayerAnimator(
        private val binding: FragmentFullCoverPlayerPlaybackControlsBinding,
        isEnabled: Boolean
    ) : PlayerAnimator(isEnabled) {
        private fun addAlphaAnimation(animators: LinkedList<Animator>, view: View, interpolator: TimeInterpolator) {
            animators.add(ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
                this.interpolator = interpolator
                this.duration = 500
            })
        }

        override fun onAddAnimation(animators: LinkedList<Animator>, interpolator: TimeInterpolator) {
            addAlphaAnimation(animators, binding.playPauseButton, interpolator)
            addAlphaAnimation(animators, binding.nextButton, interpolator)
            addAlphaAnimation(animators, binding.previousButton, interpolator)
            addAlphaAnimation(animators, binding.shuffleButton, interpolator)
            addAlphaAnimation(animators, binding.repeatButton, interpolator)
            addAlphaAnimation(animators, binding.songCurrentProgress, interpolator)
            addAlphaAnimation(animators, binding.songTotalTime, interpolator)
            addAlphaAnimation(animators, binding.songInfo, interpolator)
        }

        override fun onPrepareForAnimation() {
            binding.playPauseButton.alpha = 0f
            binding.nextButton.alpha = 0f
            binding.previousButton.alpha = 0f
            binding.shuffleButton.alpha = 0f
            binding.repeatButton.alpha = 0f
            binding.songCurrentProgress.alpha = 0f
            binding.songTotalTime.alpha = 0f
            binding.songInfo.alpha = 0f
        }
    }
}