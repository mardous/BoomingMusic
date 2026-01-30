package com.mardous.booming.ui.screen.player.styles.expressivestyle

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.TimeInterpolator
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import com.mardous.booming.R
import com.mardous.booming.core.model.player.PlayerColorScheme
import com.mardous.booming.core.model.player.PlayerTintTarget
import com.mardous.booming.core.model.player.tintTarget
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentExpressivePlayerPlaybackControlsBinding
import com.mardous.booming.extensions.dp
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_NEXT
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_PREVIOUS
import com.mardous.booming.ui.component.views.MusicSlider
import com.mardous.booming.ui.screen.player.PlayerAnimator
import com.mardous.booming.util.Preferences
import java.util.LinkedList

class ExpressivePlayerControlsFragment : AbsPlayerControlsFragment(R.layout.fragment_expressive_player_playback_controls) {

    private var _binding: FragmentExpressivePlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val musicSlider: MusicSlider
        get() = binding.progressSlider

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExpressivePlayerPlaybackControlsBinding.bind(view)
        binding.playPauseButton.setOnClickListener(this)
        binding.nextButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_NEXT))
        binding.previousButton.setOnTouchListener(getSkipButtonTouchHandler(DIRECTION_PREVIOUS))
    }

    override fun onCreatePlayerAnimator(): PlayerAnimator {
        return ExpressivePlayerAnimator(binding, Preferences.animateControls)
    }

    override fun onSongInfoChanged(currentSong: Song, nextSong: Song) {}

    override fun onExtraInfoChanged(extraInfo: String?) {}

    override fun onUpdatePlayPause(isPlaying: Boolean) {
        _binding?.playPauseButton?.let {
            it.setImageResource(
                if (isPlaying) {
                    R.drawable.ic_pause_m3_24dp
                } else {
                    R.drawable.ic_play_m3_24dp
                }
            )
            val playPauseBg = if (isPlaying) {
                ContextCompat.getDrawable(it.context, R.drawable.avd_play)
            } else {
                ContextCompat.getDrawable(it.context, R.drawable.avd_pause)
            }
            it.background = playPauseBg
            if (playPauseBg is AnimatedVectorDrawable) {
                playPauseBg.start()
            }
        }
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldSliderColor = binding.progressSlider.currentColor
        val oldOnSurfaceVariantColor = binding.songCurrentProgress.currentTextColor
        val oldPrimaryButtonColor = binding.playPauseButton.backgroundTintList?.defaultColor
            ?: Color.TRANSPARENT
        val oldSecondaryButtonColor = binding.previousButton.backgroundTintList?.defaultColor
            ?: Color.TRANSPARENT
        return listOfNotNull(
            binding.progressSlider.progressView?.tintTarget(oldSliderColor, scheme.primaryColor),
            binding.songCurrentProgress.tintTarget(oldOnSurfaceVariantColor, scheme.onSurfaceVariantColor),
            binding.songTotalTime.tintTarget(oldOnSurfaceVariantColor, scheme.onSurfaceVariantColor),
            binding.playPauseButton.tintTarget(oldPrimaryButtonColor, scheme.primaryColor),
            binding.nextButton.tintTarget(oldSecondaryButtonColor, scheme.primaryColor),
            binding.previousButton.tintTarget(oldSecondaryButtonColor, scheme.primaryColor)
        )
    }

    override fun onClick(view: View) {
        super.onClick(view)
        when (view) {
            binding.playPauseButton -> playerViewModel.togglePlayPause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ExpressivePlayerAnimator(
        private val binding: FragmentExpressivePlayerPlaybackControlsBinding,
        isEnabled: Boolean
    ) : PlayerAnimator(isEnabled) {

        private val skipButtonTranslationInPx: Float
            @Px get() = 32.dp(binding.root.context).toFloat()

        override fun onAddAnimation(animators: LinkedList<Animator>, interpolator: TimeInterpolator) {
            animators.add(
                ObjectAnimator.ofPropertyValuesHolder(
                    binding.nextButton,
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -skipButtonTranslationInPx, 0f),
                    PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)
                ).apply {
                    setInterpolator(DecelerateInterpolator())
                    duration = 250
                    startDelay = 100
                }
            )
            animators.add(
                ObjectAnimator.ofPropertyValuesHolder(
                    binding.previousButton,
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_X, skipButtonTranslationInPx, 0f),
                    PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)
                ).apply {
                    setInterpolator(DecelerateInterpolator())
                    duration = 250
                    startDelay = 100
                }
            )
            addScaleAnimation(animators, binding.songCurrentProgress, interpolator, 200)
            addScaleAnimation(animators, binding.songTotalTime, interpolator, 200)
        }

        override fun onPrepareForAnimation() {
            binding.nextButton.translationX = -skipButtonTranslationInPx
            binding.nextButton.alpha = 0f
            binding.previousButton.translationX = skipButtonTranslationInPx
            binding.previousButton.alpha = 0f
            prepareForScaleAnimation(binding.songCurrentProgress)
            prepareForScaleAnimation(binding.songTotalTime)
        }
    }
}