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

package com.mardous.booming.ui.screen.player.cover

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnPreDraw
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.viewpager.widget.ViewPager
import com.mardous.booming.R
import com.mardous.booming.core.model.PaletteColor
import com.mardous.booming.core.model.theme.NowPlayingScreen
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentPlayerAlbumCoverBinding
import com.mardous.booming.extensions.isLandscape
import com.mardous.booming.extensions.keepScreenOn
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.navigation.findActivityNavController
import com.mardous.booming.extensions.resources.BOOMING_ANIM_TIME
import com.mardous.booming.ui.adapters.pager.CustomFragmentStatePagerAdapter
import com.mardous.booming.ui.component.base.AbsPlayerFragment
import com.mardous.booming.ui.component.transform.CarouselPagerTransformer
import com.mardous.booming.ui.component.transform.ParallaxPagerTransformer
import com.mardous.booming.ui.screen.player.PlayerGesturesController
import com.mardous.booming.ui.screen.player.PlayerGesturesController.GestureType
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.screen.player.cover.page.ImageFragment
import com.mardous.booming.ui.screen.player.cover.page.ImageFragment.ColorReceiver
import com.mardous.booming.util.SWIPE_ON_COVER
import com.mardous.booming.util.LYRICS_ON_COVER
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.FlowPreview
import org.koin.androidx.viewmodel.ext.android.activityViewModel

@SuppressLint("ClickableViewAccessibility")
class CoverPagerFragment : Fragment(R.layout.fragment_player_album_cover),
    ViewPager.OnPageChangeListener,
    SharedPreferences.OnSharedPreferenceChangeListener,
    NavController.OnDestinationChangedListener {

    private val playerViewModel: PlayerViewModel by activityViewModel()

    private var _binding: FragmentPlayerAlbumCoverBinding? = null
    private val binding get() = _binding!!
    private val viewPager get() = binding.viewPager

    private var gesturesController: PlayerGesturesController? = null

    private var navController: NavController? = null
    private var coverLyricsFragment: CoverLyricsFragment? = null
    private val nps: NowPlayingScreen by lazy {
        Preferences.nowPlayingScreen
    }

    private var isAnimatingLyrics: Boolean = false
    private val isLyricsViewVisible: Boolean
        get() = _binding?.coverLyricsFragment?.isVisible == true
    private var isShowLyricsOnCover: Boolean
        get() = Preferences.showLyricsOnCover
        set(value) { Preferences.showLyricsOnCover = value }

    val isAllowedToLoadLyrics: Boolean
        get() = nps.supportsCoverLyrics

    private var callbacks: Callbacks? = null

    private var currentPosition = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayerAlbumCoverBinding.bind(view)
        coverLyricsFragment =
            childFragmentManager.findFragmentById(R.id.coverLyricsFragment) as? CoverLyricsFragment
        navController = findActivityNavController(R.id.fragment_container)
        navController?.addOnDestinationChangedListener(this)
        setupPageTransformer()
        setupEventObserver()
        Preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: androidx.savedstate.SavedState?
    ) {
        if (isLyricsViewVisible && isShowLyricsOnCover) {
            // If the user opens any of queue, sound settings or song details dialogs
            // we must ensure that we don't keep the screen on unnecessarily.
            activity?.keepScreenOn(destination.navigatorName != "dialog" && playerViewModel.isPlaying)
        }
    }

    private fun setupPageTransformer() {
        val gesturesListener = (parentFragment as? AbsPlayerFragment)
        if (gesturesListener != null) {
            gesturesController = PlayerGesturesController(
                context = viewPager.context,
                acceptedGestures = setOf(
                    GestureType.Tap,
                    GestureType.DoubleTap,
                    GestureType.LongPress
                ),
                listener = gesturesListener
            )
            viewPager.setOnTouchListener(gesturesController)
        }

        if (nps == NowPlayingScreen.Peek)
            return

        if (nps.supportsCarouselEffect && Preferences.isCarouselEffect && !resources.isLandscape) {
            val metrics = resources.displayMetrics
            val ratio = metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat()
            val padding = if (ratio >= 1.777f) 40 else 100
            viewPager.clipToPadding = false
            viewPager.setPadding(padding, 0, padding, 0)
            viewPager.pageMargin = 0
            viewPager.setPageTransformer(false, CarouselPagerTransformer(requireContext()))
        } else if (nps == NowPlayingScreen.FullCover) {
            val transformer = ParallaxPagerTransformer(R.id.player_image)
            transformer.setSpeed(0.3f)
            viewPager.offscreenPageLimit = 2
            viewPager.setPageTransformer(false, transformer)
        } else {
            viewPager.offscreenPageLimit = 2
            viewPager.setPageTransformer(true, Preferences.coverSwipingEffect)
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupEventObserver() {
        viewPager.addOnPageChangeListener(this)
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.queueFlow.collect { queue ->
                    _binding?.viewPager?.let { pager ->
                        pager.adapter = AlbumCoverPagerAdapter(parentFragmentManager, queue)
                        pager.doOnPreDraw {
                            val itemCount = pager.adapter?.count ?: 0
                            val lastIndex = (itemCount - 1).coerceAtLeast(0)
                            val target = playerViewModel.position.current.coerceIn(0, lastIndex)
                            if (itemCount > 0) {
                                if (pager.currentItem != target) {
                                    pager.setCurrentItem(target, false)
                                }
                                onPageSelected(target)
                            }
                        }
                    }
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.positionFlow.collect { position ->
                _binding?.viewPager?.let { pager ->
                    if (pager.currentItem != position.current) {
                        pager.setCurrentItem(position.current, true)
                    }
                }
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.isPlayingFlow.collect {
                activity?.keepScreenOn(isShowLyricsOnCover && isLyricsViewVisible)
            }
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

            SWIPE_ON_COVER -> {
                viewPager.setAllowSwiping(Preferences.swipeOnCover)
            }
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageScrollStateChanged(state: Int) {}
    override fun onPageSelected(position: Int) {
        currentPosition = position
        requestColor(position)
        if (position != playerViewModel.position.current) {
            playerViewModel.playSongAt(position)
        }
    }

    override fun onDestroyView() {
        gesturesController?.release()
        gesturesController = null
        viewPager.adapter = null
        viewPager.setOnTouchListener(null)
        viewPager.removeOnPageChangeListener(this)
        navController?.removeOnDestinationChangedListener(this)
        navController = null
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
        _binding = null
    }

    fun toggleLyrics() {
        if (isAnimatingLyrics) return
        if (isShowLyricsOnCover) {
            hideLyrics(true)
        } else {
            showLyrics(true)
        }
    }

    fun showLyrics(isForced: Boolean = false) {
        if (!isAllowedToLoadLyrics || (!isShowLyricsOnCover && !isForced) || isAnimatingLyrics)
            return

        isAnimatingLyrics = true

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(binding.coverLyricsFragment, View.ALPHA, 1f),
            ObjectAnimator.ofFloat(binding.viewPager, View.ALPHA, 0f)
        )
        animatorSet.duration = BOOMING_ANIM_TIME
        animatorSet.doOnEnd {
            _binding?.viewPager?.isInvisible = true
            isAnimatingLyrics = false
            it.removeAllListeners()
        }
        animatorSet.doOnStart {
            coverLyricsFragment?.let { fragment ->
                activity?.keepScreenOn(playerViewModel.isPlaying)
                if (isVisible) {
                    childFragmentManager.beginTransaction()
                        .setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
                        .commitAllowingStateLoss()
                }
            }
            isShowLyricsOnCover = true
            _binding?.coverLyricsFragment?.isVisible = true
        }
        callbacks?.onLyricsVisibilityChange(animatorSet, true)
        animatorSet.start()
    }

    fun hideLyrics(isPermanent: Boolean = false) {
        if (!isShowLyricsOnCover || isAnimatingLyrics) return

        isAnimatingLyrics = true

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(binding.coverLyricsFragment, View.ALPHA, 0f),
            ObjectAnimator.ofFloat(binding.viewPager, View.ALPHA, 1f)
        )
        animatorSet.duration = BOOMING_ANIM_TIME
        animatorSet.doOnStart {
            _binding?.viewPager?.isInvisible = false
        }
        animatorSet.doOnEnd {
            coverLyricsFragment?.let { fragment ->
                activity?.keepScreenOn(false)
                if (isVisible) {
                    childFragmentManager.beginTransaction()
                        .setMaxLifecycle(fragment, Lifecycle.State.STARTED)
                        .commitAllowingStateLoss()
                }
            }
            if (isPermanent) {
                isShowLyricsOnCover = false
            }
            _binding?.coverLyricsFragment?.isVisible = false
            isAnimatingLyrics = false
            it.removeAllListeners()
        }
        callbacks?.onLyricsVisibilityChange(animatorSet, false)
        animatorSet.start()
    }

    private fun requestColor(position: Int) {
        if (playerViewModel.queue.isNotEmpty()) {
            (viewPager.adapter as? AlbumCoverPagerAdapter)
                ?.receiveColor(colorReceiver, position)
        }
    }

    private val colorReceiver = object : ColorReceiver {
        override fun onColorReady(color: PaletteColor, request: Int) {
            if (currentPosition == request) {
                callbacks?.onColorChanged(color)
            }
        }
    }

    internal fun setCallbacks(callbacks: Callbacks?) {
        this.callbacks = callbacks
    }

    interface Callbacks {
        fun onColorChanged(color: PaletteColor)
        fun onLyricsVisibilityChange(animatorSet: AnimatorSet, lyricsVisible: Boolean)
    }

    companion object {
        const val TAG = "PlayerAlbumCoverFragment"
    }
}

class AlbumCoverPagerAdapter(fm: FragmentManager, private val dataSet: List<Song>) :
    CustomFragmentStatePagerAdapter(fm) {

    private var currentPaletteReceiver: ColorReceiver? = null
    private var currentColorReceiverPosition = -1

    override fun getItem(position: Int): Fragment {
        return ImageFragment.newInstance(dataSet[position])
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val o = super.instantiateItem(container, position)
        if (currentPaletteReceiver != null && currentColorReceiverPosition == position) {
            receiveColor(currentPaletteReceiver!!, currentColorReceiverPosition)
        }
        return o
    }

    /**
     * Only the latest passed [ImageFragment.ColorReceiver] is guaranteed to receive a response
     */
    fun receiveColor(paletteReceiver: ColorReceiver, @ColorInt position: Int) {
        val fragment = getFragment(position) as ImageFragment?
        if (fragment != null) {
            currentPaletteReceiver = null
            currentColorReceiverPosition = -1
            fragment.receivePalette(paletteReceiver, position)
        } else {
            currentPaletteReceiver = paletteReceiver
            currentColorReceiverPosition = position
        }
    }
}