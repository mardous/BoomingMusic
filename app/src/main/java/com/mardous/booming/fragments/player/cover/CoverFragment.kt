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

package com.mardous.booming.fragments.player.cover

import android.animation.AnimatorSet
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.widget.ViewPager2
import com.mardous.booming.R
import com.mardous.booming.adapters.pager.AlbumCoverPagerAdapter
import com.mardous.booming.adapters.pager.AlbumCoverPagerAdapter.AlbumCoverFragment.CoverEventReceiver
import com.mardous.booming.databinding.FragmentPlayerAlbumCoverBinding
import com.mardous.booming.extensions.isLandscape
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.resources.disableNestedScrolling
import com.mardous.booming.extensions.resources.setCarouselEffect
import com.mardous.booming.extensions.resources.setParallaxEffect
import com.mardous.booming.helper.color.MediaNotificationProcessor
import com.mardous.booming.model.GestureOnCover
import com.mardous.booming.model.theme.NowPlayingScreen
import com.mardous.booming.util.LEFT_RIGHT_SWIPING
import com.mardous.booming.util.LYRICS_ON_COVER
import com.mardous.booming.util.Preferences
import com.mardous.booming.viewmodels.player.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.math.abs

class CoverFragment : Fragment(R.layout.fragment_player_album_cover),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val playerViewModel: PlayerViewModel by activityViewModel()

    private var _binding: FragmentPlayerAlbumCoverBinding? = null
    private val binding get() = _binding!!

    private var adapter: AlbumCoverPagerAdapter? = null
    private val viewPager get() = binding.viewPager

    private var coverLyricsController: CoverLyricsController? = null
    private var coverLyricsFragment: CoverLyricsFragment? = null

    private val nps: NowPlayingScreen
        get() = Preferences.nowPlayingScreen

    private var callbacks: Callbacks? = null
    private var currentPosition = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayerAlbumCoverBinding.bind(view)
        coverLyricsController = CoverLyricsController(nps, binding.coverLyricsFragment, binding.viewPager)
        coverLyricsFragment = childFragmentManager.findFragmentById(R.id.coverLyricsFragment) as? CoverLyricsFragment
        adapter = AlbumCoverPagerAdapter(this)
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            combine(
                playerViewModel.playingQueueFlow,
                playerViewModel.currentPositionFlow
            ) { queue, position -> queue to position }
                .filter { (_, position) -> position > -1 }
                .collectLatest { (queue, position) ->
                    adapter?.updateData(queue) {
                        _binding?.viewPager?.doOnPreDraw {
                            _binding?.viewPager?.setCurrentItem(position, abs(viewPager.currentItem - position) <= 2)
                        }
                    }
                }
        }
        setUpViewPager()
        Preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStart() {
        super.onStart()
        _binding?.viewPager?.registerOnPageChangeCallback(pageChangeCallback)
    }

    private fun setUpViewPager() {
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = Preferences.allowCoverSwiping
        viewPager.disableNestedScrolling() // https://stackoverflow.com/a/69213473

        if (nps == NowPlayingScreen.Peek) return

        if (nps.supportsCarouselEffect && Preferences.isCarouselEffect && !resources.isLandscape) {
            viewPager.setCarouselEffect()
        } else if (nps == NowPlayingScreen.FullCover) {
            viewPager.setParallaxEffect(R.id.player_image)
        } else {
            viewPager.offscreenPageLimit = 2
            viewPager.setPageTransformer(Preferences.coverSwipingEffect)
        }
    }

    fun toggleLyrics() {
        coverLyricsController?.let {
            if (it.isAnimatingLyrics) return
            if (it.isShowLyricsOnCover) {
                hideLyrics(true)
            } else {
                showLyrics(true)
            }
        }
    }

    fun showLyrics(isForced: Boolean = false) {
        coverLyricsController?.showLyrics(
            isForced = isForced,
            onPrepared = { animatorSet ->
                callbacks?.onLyricsVisibilityChange(animatorSet, true)
            },
            onStart = {
                coverLyricsFragment?.let { fragment ->
                    childFragmentManager.beginTransaction()
                        .setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
                        .commitAllowingStateLoss()
                }
            })
    }

    fun hideLyrics(isPermanent: Boolean = false) {
        coverLyricsController?.hideLyrics(
            isPermanent = isPermanent,
            onPrepared = { animatorSet ->
                callbacks?.onLyricsVisibilityChange(animatorSet, false)
            },
            onEnd = {
                coverLyricsFragment?.let { fragment ->
                    childFragmentManager.beginTransaction()
                        .setMaxLifecycle(fragment, Lifecycle.State.STARTED)
                        .commitAllowingStateLoss()
                }
            }
        )
    }

    fun setCallbacks(callbacks: Callbacks?) {
        this.callbacks = callbacks
    }

    private fun moveToItem(target: Int) {
        val currentItem = viewPager.currentItem
        if (currentItem != target) {
            viewPager.setCurrentItem(target, abs(currentItem - target) <= 2)
        }
    }

    private fun requestColor(position: Int) {
        if (playerViewModel.playingQueue.isNotEmpty()) {
            (viewPager.adapter as? AlbumCoverPagerAdapter)
                ?.receiveColor(coverEventReceiver, position)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (Preferences.getNowPlayingColorSchemeKey(nps) == key) {
            requestColor(currentPosition)
        } else when (key) {
            LYRICS_ON_COVER -> {
                val isShowLyrics = sharedPreferences.getBoolean(key, true)
                if (isShowLyrics && !binding.coverLyricsFragment.isVisible) {
                    showLyrics()
                } else if (!isShowLyrics && binding.coverLyricsFragment.isVisible) {
                    hideLyrics()
                }
            }

            LEFT_RIGHT_SWIPING -> {
                viewPager.isUserInputEnabled = Preferences.allowCoverSwiping
            }
        }
    }

    override fun onDestroyView() {
        _binding?.viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
        adapter = null
        _binding = null
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        private var isUserDragging = false

        override fun onPageScrollStateChanged(state: Int) {
            isUserDragging = state == ViewPager2.SCROLL_STATE_DRAGGING
                    || state == ViewPager2.SCROLL_STATE_SETTLING
        }

        override fun onPageSelected(position: Int) {
            currentPosition = position
            requestColor(position)
            if (isUserDragging) {
                if (position != playerViewModel.currentPosition) {
                    playerViewModel.playSongAt(position)
                }
            }
        }
    }

    private val coverEventReceiver = object : CoverEventReceiver {
        override fun onColorReady(color: MediaNotificationProcessor, request: Int) {
            if (currentPosition == request) {
                callbacks?.onColorChanged(color)
            }
        }

        override fun onGestureEvent(gesture: GestureOnCover): Boolean {
            return callbacks?.onGestureDetected(gesture) == true
        }
    }

    interface Callbacks {
        fun onColorChanged(color: MediaNotificationProcessor)
        fun onGestureDetected(gestureOnCover: GestureOnCover): Boolean
        fun onLyricsVisibilityChange(animatorSet: AnimatorSet, lyricsVisible: Boolean)
    }

    companion object {
        const val TAG = "PlayerAlbumCoverFragment"
    }
}