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

package com.mardous.booming.ui.screen.other

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import coil3.request.Disposable
import com.mardous.booming.R
import com.mardous.booming.coil.songImage
import com.mardous.booming.core.model.player.ProgressState
import com.mardous.booming.core.model.theme.NowPlayingButtonStyle
import com.mardous.booming.databinding.FragmentMiniPlayerBinding
import com.mardous.booming.extensions.isTablet
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.resources.show
import com.mardous.booming.extensions.resources.textColorPrimary
import com.mardous.booming.extensions.resources.textColorSecondary
import com.mardous.booming.extensions.resources.toForegroundColorSpan
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_NEXT
import com.mardous.booming.ui.component.base.SkipButtonTouchHandler.Companion.DIRECTION_PREVIOUS
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.math.abs

class MiniPlayerFragment : Fragment(R.layout.fragment_mini_player),
    View.OnClickListener, SkipButtonTouchHandler.Callback {

    private val playerViewModel: PlayerViewModel by activityViewModel()

    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var primaryColorSpan: ForegroundColorSpan
    private lateinit var secondaryColorSpan: ForegroundColorSpan

    private val buttonStyle: NowPlayingButtonStyle
        get() = if (Preferences.adaptiveControls) {
            Preferences.nowPlayingScreen.buttonStyle
        } else {
            NowPlayingButtonStyle.Normal
        }

    private var disposable: Disposable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMiniPlayerBinding.bind(view)
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.currentSongFlow.collect {
                updateCurrentSong()
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            combine(
                playerViewModel.currentProgressFlow,
                playerViewModel.totalDurationFlow
            ) { progress, duration -> ProgressState(progress.toLong(), duration.toLong()) }
                .filter { progress -> progress.mayUpdateUI }
                .collectLatest {
                    binding.progressBar.max = it.total.toInt()
                    binding.progressBar.setProgressCompat(it.progress.toInt(), true)
                }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.isPlayingFlow.collect { isPlaying ->
                updatePlayPause(isPlaying, buttonStyle)
            }
        }
        primaryColorSpan = textColorPrimary().toForegroundColorSpan()
        secondaryColorSpan = textColorSecondary().toForegroundColorSpan()
        setUpButtons()
        view.setOnTouchListener { _, event -> flingPlayBackController.onTouchEvent(event) }
    }

    private fun setUpButtons() {
        setupButtonStyle()
        setupExtraControls()
        binding.actionNext.setOnTouchListener(SkipButtonTouchHandler(DIRECTION_NEXT, this))
        binding.actionPrevious.setOnTouchListener(SkipButtonTouchHandler(DIRECTION_PREVIOUS, this))
        binding.actionPlayPause.setOnClickListener(this)
    }

    fun setupButtonStyle() {
        val buttonStyle = this.buttonStyle
        binding.actionNext.setIconResource(buttonStyle.skipNext)
        binding.actionPrevious.setIconResource(buttonStyle.skipPrevious)
        updatePlayPause(playerViewModel.isPlaying, buttonStyle)
    }

    fun setupExtraControls() {
        if (resources.isTablet) {
            binding.actionNext.show()
            binding.actionPrevious.show()
        } else {
            binding.actionNext.isVisible = Preferences.extraControls
            binding.actionPrevious.isVisible = Preferences.extraControls
        }
    }

    override fun onSkipButtonHold(direction: Int) {
        when (direction) {
            DIRECTION_NEXT -> playerViewModel.fastForward()
            DIRECTION_PREVIOUS -> playerViewModel.rewind()
        }
    }

    override fun onSkipButtonTap(direction: Int) {
        when (direction) {
            DIRECTION_NEXT -> playerViewModel.playNext()
            DIRECTION_PREVIOUS -> playerViewModel.playPrevious()
        }
    }

    override fun onClick(view: View) {
        when (view) {
            binding.actionPlayPause -> playerViewModel.togglePlayPause()
        }
    }

    override fun onDestroyView() {
        disposable?.dispose()
        super.onDestroyView()
        _binding = null
    }

    private fun updatePlayPause(isPlaying: Boolean, buttonStyle: NowPlayingButtonStyle) {
        if (isPlaying) {
            binding.actionPlayPause.setIconResource(buttonStyle.pause)
        } else {
            binding.actionPlayPause.setIconResource(buttonStyle.play)
        }
    }

    private fun updateCurrentSong() {
        val song = playerViewModel.currentSong

        binding.songTitle.isSelected = true
        binding.songArtist.isSelected = true

        binding.songTitle.text = song.title
        binding.songArtist.text = song.displayArtistName()

        disposable = binding.image.songImage(song)
    }

    private var flingPlayBackController = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (abs(velocityX) > abs(velocityY)) {
                    if (velocityX < 0) {
                        playerViewModel.playNext()
                        return true
                    } else if (velocityX > 0) {
                        playerViewModel.playPrevious()
                        return true
                    }
                }
                return false
            }
        })
}