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

package com.mardous.booming.fragments.player

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.*
import androidx.viewpager.widget.ViewPager
import com.mardous.booming.R
import com.mardous.booming.adapters.pager.AlbumCoverPagerAdapter
import com.mardous.booming.databinding.FragmentPlayerAlbumCoverBinding
import com.mardous.booming.extensions.currentFragment
import com.mardous.booming.extensions.isLandscape
import com.mardous.booming.extensions.keepScreenOn
import com.mardous.booming.extensions.resources.*
import com.mardous.booming.fragments.base.AbsMusicServiceFragment
import com.mardous.booming.fragments.lyrics.LyricsFragment
import com.mardous.booming.fragments.lyrics.LyricsViewModel
import com.mardous.booming.fragments.player.base.goToDestination
import com.mardous.booming.helper.MusicProgressViewUpdateHelper
import com.mardous.booming.helper.color.MediaNotificationProcessor
import com.mardous.booming.lyrics.LrcLyrics
import com.mardous.booming.model.GestureOnCover
import com.mardous.booming.model.theme.NowPlayingScreen
import com.mardous.booming.service.MusicPlayer
import com.mardous.booming.transform.CarouselPagerTransformer
import com.mardous.booming.transform.ParallaxPagerTransformer
import com.mardous.booming.util.LEFT_RIGHT_SWIPING
import com.mardous.booming.util.LYRICS_ON_COVER
import com.mardous.booming.util.Preferences
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class PlayerAlbumCoverFragment : AbsMusicServiceFragment(), ViewPager.OnPageChangeListener,
    SharedPreferences.OnSharedPreferenceChangeListener, MusicProgressViewUpdateHelper.Callback {

    private val lyricsViewModel: LyricsViewModel by activityViewModel()

    private var _binding: FragmentPlayerAlbumCoverBinding? = null
    private val binding get() = _binding!!
    private val viewPager get() = binding.viewPager

    private val nps: NowPlayingScreen by lazy {
        Preferences.nowPlayingScreen
    }

    private var lyricsLoaded = false

    private val isAllowedToLoadLyrics: Boolean
        get() = nps.supportsCoverLyrics && isAdded && !isRemoving
    private var isShowLyricsOnCover: Boolean
        get() = Preferences.showLyricsOnCover
        set(value) { Preferences.showLyricsOnCover = value }

    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    private var gestureDetector: GestureDetector? = null
    private var callbacks: Callbacks? = null

    private var currentPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gestureDetector =
            GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                    return consumeGesture(GestureOnCover.Tap)
                }

                override fun onDoubleTap(event: MotionEvent): Boolean {
                    return consumeGesture(GestureOnCover.DoubleTap)
                }

                override fun onLongPress(e: MotionEvent) {
                    consumeGesture(GestureOnCover.LongPress)
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_player_album_cover, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayerAlbumCoverBinding.bind(view)
        binding.syncedLyricsView.setDraggable(true) {
            MusicPlayer.seekTo(it.toInt())
            true
        }
        binding.openEditor.setOnClickListener {
            goToDestination(requireActivity(), R.id.nav_lyrics)
        }
        binding.openEditor.setOnLongClickListener {
            hideLyrics(true)
            true
        }
        applyWindowInsets()
        setupPageTransformer()
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this, 500, 1000)
        Preferences.registerOnSharedPreferenceChangeListener(this)
    }

    private fun applyWindowInsets() {
        if (nps == NowPlayingScreen.Gradient) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.lyricsLayout) { v: View, insets: WindowInsetsCompat ->
                val padding = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
                v.updatePadding(left = padding.left, right = padding.right)
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    private fun setupPageTransformer() {
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()
        viewPager.addOnPageChangeListener(this)
        viewPager.setOnTouchListener { _: View?, motionEvent: MotionEvent? ->
            if (motionEvent != null) {
                gestureDetector?.onTouchEvent(motionEvent) == true
            } else false
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (Preferences.getNowPlayingColorSchemeKey(nps) == key) {
            requestColor(currentPosition)
        } else when (key) {
            LYRICS_ON_COVER -> {
                val isShowLyrics = sharedPreferences.getBoolean(key, true)
                if (isShowLyrics && !binding.lyricsLayout.isVisible) {
                    showLyrics()
                } else if (!isShowLyrics && binding.lyricsLayout.isVisible) {
                    removeLyrics()
                }
            }

            LEFT_RIGHT_SWIPING -> {
                viewPager.setAllowSwiping(Preferences.allowCoverSwiping)
            }
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageScrollStateChanged(state: Int) {}
    override fun onPageSelected(position: Int) {
        currentPosition = position
        requestColor(position)
        if (position != MusicPlayer.position) {
            MusicPlayer.playSongAt(position)
        }
    }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
        updateLyrics()
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
    }

    override fun onDestroyView() {
        viewPager.removeOnPageChangeListener(this)
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
        gestureDetector = null
        _binding = null
        super.onDestroyView()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updatePlayingQueue()
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        val position = MusicPlayer.position
        if (viewPager.currentItem != position) {
            viewPager.setCurrentItem(position, true)
        }
        updateLyrics()
    }

    override fun onQueueChanged() {
        super.onQueueChanged()
        updatePlayingQueue()
    }

    override fun onUpdateProgressViews(progress: Long, total: Long) {
        if (!isShowingLyrics())
            return

        _binding?.syncedLyricsView?.updateTime(progress)
    }

    fun isShowingLyrics() = lyricsLoaded

    fun toggleLyrics() {
        if (isShowLyricsOnCover) {
            hideLyrics(true)
        } else {
            showLyrics(true)
        }
    }

    fun showLyrics(isForced: Boolean = false) {
        if (!lyricsLoaded || binding.lyricsLayout.isVisible || (!isShowLyricsOnCover && !isForced))
            return

        setLyricsLayoutVisible(true) {
            isShowLyricsOnCover = true
        }
    }

    fun hideLyrics(isPermanent: Boolean = false) {
        if (!lyricsLoaded || !binding.lyricsLayout.isVisible) return

        setLyricsLayoutVisible(false) {
            if (isPermanent) {
                isShowLyricsOnCover = false
            }
        }
    }

    private fun updateLyrics() {
        removeLyrics {
            if (!isAllowedToLoadLyrics) {
                return@removeLyrics
            }
            lyricsViewModel.getAllLyrics(
                MusicPlayer.currentSong,
                allowDownload = true
            ).observe(viewLifecycleOwner) {
                if (it.loading)
                    return@observe

                if (it.id == MusicPlayer.currentSong.id) {
                    if (it.isSynced) {
                        setLyricsContent(it.lrcData, null)
                    } else {
                        setLyricsContent(null, it.data)
                    }
                }
            }
        }
    }

    private fun removeLyrics(onCompleted: AnimationCompleted? = null) {
        this.lyricsLoaded = false
        setLyricsLayoutVisible(false, isFullHide = true, onCompleted = onCompleted)
    }

    private fun setLyricsLayoutVisible(
        isVisible: Boolean,
        isFullHide: Boolean = false,
        onCompleted: AnimationCompleted? = null
    ) {
        _binding?.let { safeBinding ->
            val isInLyricsFragment = requireActivity().currentFragment(R.id.fragment_container) is LyricsFragment
            requireActivity().keepScreenOn(isVisible || isInLyricsFragment)

            val animatorSet = AnimatorSet()
            animatorSet.duration = BOOMING_ANIM_TIME
            if (isVisible) {
                animatorSet.playTogether(
                    ObjectAnimator.ofFloat(safeBinding.lyricsLayout, View.ALPHA, 1f),
                    ObjectAnimator.ofFloat(safeBinding.viewPager, View.ALPHA, 0f)
                )
                animatorSet.doOnStart { safeBinding.lyricsLayout.isVisible = true }
                animatorSet.doOnEnd { safeBinding.viewPager.isInvisible = true }
            } else {
                animatorSet.playTogether(
                    ObjectAnimator.ofFloat(safeBinding.lyricsLayout, View.ALPHA, 0f),
                    ObjectAnimator.ofFloat(safeBinding.viewPager, View.ALPHA, 1f)
                )
                animatorSet.doOnStart { safeBinding.viewPager.isInvisible = false }
                animatorSet.doOnEnd {
                    safeBinding.lyricsLayout.isVisible = false
                    if (isFullHide) {
                        safeBinding.syncedLyricsView.reset()
                        safeBinding.syncedLyricsView.hide()
                        safeBinding.normalLyrics.text = null
                        safeBinding.normalLyrics.hide()
                    }
                }
            }
            animatorSet.doOnEnd {
                animatorSet.removeAllListeners()
                onCompleted?.invoke()
            }
            callbacks?.onLyricsVisibilityChange(animatorSet, isVisible)
            animatorSet.start()
        }
    }

    private fun setLyricsContent(lrcData: LrcLyrics?, normalLyrics: String?) {
        _binding?.let { safeBinding ->
            val isLyricsLayoutVisible = safeBinding.lyricsLayout.isVisible
            if (lrcData != null && lrcData.hasLines) {
                val animatorSet = AnimatorSet()
                animatorSet.duration = BOOMING_ANIM_TIME
                animatorSet.playSequentially(
                    ObjectAnimator.ofFloat(safeBinding.normalLyrics, View.ALPHA, 0f),
                    ObjectAnimator.ofFloat(safeBinding.syncedLyricsView, View.ALPHA, 1f)
                )
                animatorSet.doOnStart {
                    safeBinding.syncedLyricsView.setLRCContent(lrcData)
                    safeBinding.syncedLyricsView.show()
                }
                animatorSet.doOnEnd {
                    safeBinding.normalLyrics.text = null
                    safeBinding.normalLyrics.hide()
                }
                animatorSet.start()
                if (isShowLyricsOnCover && !isLyricsLayoutVisible) {
                    setLyricsLayoutVisible(true)
                }
                this.lyricsLoaded = true
            } else if (!normalLyrics.isNullOrBlank()) {
                val animatorSet = AnimatorSet()
                animatorSet.duration = BOOMING_ANIM_TIME
                animatorSet.playSequentially(
                    ObjectAnimator.ofFloat(safeBinding.normalLyrics, View.ALPHA, 1f),
                    ObjectAnimator.ofFloat(safeBinding.syncedLyricsView, View.ALPHA, 0f)
                )
                animatorSet.doOnStart {
                    safeBinding.lyricsScrollView.scrollTo(0, 0)
                    safeBinding.normalLyrics.text = normalLyrics
                    safeBinding.normalLyrics.show()
                }
                animatorSet.doOnEnd {
                    safeBinding.syncedLyricsView.reset()
                    safeBinding.syncedLyricsView.hide()
                }
                animatorSet.start()
                if (isShowLyricsOnCover && !isLyricsLayoutVisible) {
                    setLyricsLayoutVisible(true)
                }
                this.lyricsLoaded = true
            } else {
                removeLyrics()
            }
        }
    }

    private fun setLrcViewColors(@ColorInt primaryColor: Int, @ColorInt secondaryColor: Int) {
        binding.syncedLyricsView.apply {
            setCurrentColor(primaryColor)
            setTimeTextColor(primaryColor)
            setTimelineColor(primaryColor)
            setNormalColor(secondaryColor)
            setTimelineTextColor(primaryColor)
        }
    }

    private fun updatePlayingQueue() {
        _binding?.viewPager?.apply {
            adapter = AlbumCoverPagerAdapter(parentFragmentManager, MusicPlayer.playingQueue)
            currentItem = MusicPlayer.position
        }
        onPageSelected(MusicPlayer.position)
    }

    private fun requestColor(position: Int) {
        if (MusicPlayer.playingQueue.isNotEmpty()) {
            (viewPager.adapter as AlbumCoverPagerAdapter?)?.receiveColor(colorReceiver, position)
        }
    }

    private val colorReceiver = object : AlbumCoverPagerAdapter.AlbumCoverFragment.ColorReceiver {
        override fun onColorReady(color: MediaNotificationProcessor, request: Int) {
            if (currentPosition == request) {
                notifyColorChange(color)
            }
        }
    }

    private fun notifyColorChange(color: MediaNotificationProcessor) {
        callbacks?.onColorChanged(color)
        val primaryColor = getPrimaryTextColor(requireContext(), surfaceColor().isColorLight)
        val secondaryColor =
            getSecondaryTextColor(requireContext(), surfaceColor().isColorLight, true)
        if (nps == NowPlayingScreen.Gradient) {
            setLrcViewColors(color.primaryTextColor, color.secondaryTextColor)
            binding.normalLyrics.setTextColor(color.primaryTextColor)
            binding.openEditor.applyColor(color.primaryTextColor)
        } else {
            setLrcViewColors(primaryColor, secondaryColor)
        }
    }

    private fun consumeGesture(gesture: GestureOnCover): Boolean {
        return callbacks?.onGestureDetected(gesture) ?: false
    }

    internal fun setCallbacks(callbacks: Callbacks?) {
        this.callbacks = callbacks
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