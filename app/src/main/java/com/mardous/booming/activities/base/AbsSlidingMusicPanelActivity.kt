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

package com.mardous.booming.activities.base

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.animation.doOnEnd
import androidx.core.view.*
import androidx.fragment.app.commit
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import com.mardous.booming.R
import com.mardous.booming.activities.PermissionsActivity
import com.mardous.booming.databinding.SlidingMusicPanelLayoutBinding
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.resources.*
import com.mardous.booming.fragments.LibraryViewModel
import com.mardous.booming.fragments.other.MiniPlayerFragment
import com.mardous.booming.fragments.player.base.AbsPlayerFragment
import com.mardous.booming.fragments.player.styles.defaultstyle.DefaultPlayerFragment
import com.mardous.booming.fragments.player.styles.fullcoverstyle.FullCoverPlayerFragment
import com.mardous.booming.fragments.player.styles.gradientstyle.GradientPlayerFragment
import com.mardous.booming.fragments.player.styles.m3style.M3PlayerFragment
import com.mardous.booming.fragments.player.styles.peekplayerstyle.PeekPlayerFragment
import com.mardous.booming.fragments.player.styles.plainstyle.PlainPlayerFragment
import com.mardous.booming.fragments.queue.PlayingQueueFragment
import com.mardous.booming.model.CategoryInfo
import com.mardous.booming.model.theme.NowPlayingScreen
import com.mardous.booming.service.MusicPlayer
import com.mardous.booming.util.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Christians M. A. (mardous)
 */
abstract class AbsSlidingMusicPanelActivity : AbsMusicServiceActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    protected lateinit var binding: SlidingMusicPanelLayoutBinding
    protected val libraryViewModel: LibraryViewModel by viewModel()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var nowPlayingScreen: NowPlayingScreen

    private var miniPlayerFragment: MiniPlayerFragment? = null
    private var windowInsets: WindowInsetsCompat? = null

    var isInOneTabMode: Boolean = false

    val navigationView: NavigationBarView
        get() = binding.navigationView
    val slidingPanel: FrameLayout
        get() = binding.sheetView

    private var playerFragment: AbsPlayerFragment? = null
    private var paletteColor: Int = 0

    var panelState: Int
        get() = bottomSheetBehavior.state
        set(value) { bottomSheetBehavior.state = value }
    private var panelStateBefore: Int? = null
    private var panelStateCurrent: Int? = null
    val isBottomNavVisible: Boolean
        get() = navigationView.isVisible && navigationView is BottomNavigationView

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (handleBackPress()) {
                return
            }
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
            if (!navHostFragment.navController.navigateUp()) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasPermissions()) {
            startActivity(Intent(this, PermissionsActivity::class.java))
            finish()
        }
        binding = SlidingMusicPanelLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            insets.also { windowInsets = it }
        }
        chooseFragmentForTheme()
        setupNavigationView()
        setupSlidingUpPanel()
        setupBottomSheet()
        updateColor()

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun setupNavigationView() {
        navigationView.labelVisibilityMode = Preferences.bottomTitlesMode
        if (navigationView is NavigationRailView) {
            navigationView.applyWindowInsets(left = true, top = true)
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = from(binding.sheetView)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
        bottomSheetBehavior.isHideable = Preferences.swipeDownToDismiss
        bottomSheetBehavior.significantVelocityThreshold = 300
        setMiniPlayerAlphaProgress(0F)
    }

    private fun setupSlidingUpPanel() {
        binding.sheetView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.sheetView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (nowPlayingScreen == NowPlayingScreen.Peek) {
                    slidingPanel.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
                when (panelState) {
                    STATE_EXPANDED -> onPanelExpanded()
                    STATE_COLLAPSED -> onPanelCollapsed()
                    else -> {
                        // playerFragment!!.onHide()
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Preferences.registerOnSharedPreferenceChangeListener(this)
        if (nowPlayingScreen != Preferences.nowPlayingScreen) {
            postRecreate()
        }
        if (bottomSheetBehavior.state == STATE_EXPANDED) {
            setMiniPlayerAlphaProgress(1f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback)
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private val bottomSheetCallback = object : BottomSheetCallback() {
        @SuppressLint("SwitchIntDef")
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (panelStateCurrent != null) {
                panelStateBefore = panelStateCurrent
            }
            panelStateCurrent = newState
            when (newState) {
                STATE_EXPANDED -> onPanelExpanded()
                STATE_COLLAPSED -> onPanelCollapsed()
                STATE_HIDDEN -> MusicPlayer.clearQueue()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            setMiniPlayerAlphaProgress(slideOffset)
        }
    }

    fun setBottomNavVisibility(
        visible: Boolean,
        animate: Boolean = false,
        hideBottomSheet: Boolean = MusicPlayer.playingQueue.isEmpty(),
    ) {
        if (isInOneTabMode) {
            hideBottomSheet(hide = hideBottomSheet, animate = animate, isBottomNavVisible = false)
            return
        }
        val isBottomNavView = (navigationView is BottomNavigationView)
        if (visible xor navigationView.isVisible) {
            val mAnimate = animate && isBottomNavView && panelState == STATE_COLLAPSED
            if (mAnimate) {
                if (visible) {
                    navigationView.bringToFront()
                    navigationView.show()
                } else {
                    navigationView.hide()
                }
            } else {
                navigationView.isVisible = visible
                if (visible && isBottomNavView && panelState != STATE_EXPANDED) {
                    navigationView.bringToFront()
                }
            }
        }
        hideBottomSheet(
            hide = hideBottomSheet,
            animate = animate,
            isBottomNavVisible = visible && navigationView is BottomNavigationView
        )
    }

    private fun hideBottomSheet(
        hide: Boolean,
        animate: Boolean = false,
        isBottomNavVisible: Boolean = navigationView.isVisible && navigationView is BottomNavigationView
    ) {
        val heightOfBar = windowInsets.getBottomInsets() + dip(R.dimen.mini_player_height)
        val heightOfBarWithTabs = heightOfBar + dip(R.dimen.bottom_nav_height)
        if (hide) {
            bottomSheetBehavior.peekHeight = (-windowInsets.getBottomInsets()).coerceAtLeast(0)
            panelState = STATE_COLLAPSED
            libraryViewModel.setFabMargin(this, if (isBottomNavVisible) dip(R.dimen.bottom_nav_height) else 0)
        } else {
            if (MusicPlayer.playingQueue.isNotEmpty()) {
                slidingPanel.elevation = 0f
                navigationView.elevation = 5f
                if (isBottomNavVisible) {
                    if (animate) {
                        bottomSheetBehavior.peekHeightAnimate(heightOfBarWithTabs)
                    } else {
                        bottomSheetBehavior.peekHeight = heightOfBarWithTabs
                    }
                    libraryViewModel.setFabMargin(this, dip(R.dimen.bottom_nav_mini_player_height))
                } else {
                    if (animate) {
                        bottomSheetBehavior.peekHeightAnimate(heightOfBar).doOnEnd {
                            slidingPanel.bringToFront()
                        }
                    } else {
                        bottomSheetBehavior.peekHeight = heightOfBar
                        slidingPanel.bringToFront()
                    }
                    libraryViewModel.setFabMargin(this, dip(R.dimen.mini_player_height))
                }
            }
        }
    }

    fun collapsePanel() {
        panelState = STATE_COLLAPSED
    }

    fun expandPanel() {
        panelState = STATE_EXPANDED
    }

    fun getBottomSheetBehavior() = bottomSheetBehavior

    protected open fun onPanelCollapsed() {
        setMiniPlayerAlphaProgress(0f)
        // restore values
        setLightStatusBar()
        setLightNavigationBar()
        playerFragment?.onHide()
    }

    protected open fun onPanelExpanded() {
        setMiniPlayerAlphaProgress(1f)
        onPaletteColorChanged()
        playerFragment?.onShow()
    }

    protected fun updateTabs() {
        navigationView.menu.clear()
        val currentTabs: List<CategoryInfo> = Preferences.libraryCategories
        for (tab in currentTabs) {
            if (tab.visible) {
                val menu = tab.category
                navigationView.menu.add(0, menu.id, 0, menu.titleRes)
                    .setIcon(menu.iconRes)
            }
        }
        if (navigationView.menu.size == 1) {
            isInOneTabMode = true
            navigationView.isVisible = false
        } else {
            isInOneTabMode = false
        }
    }

    private fun setMiniPlayerAlphaProgress(progress: Float) {
        if (progress < 0) return
        val alpha = 1 - progress
        miniPlayerFragment?.view?.alpha = 1 - (progress / 0.2F)
        miniPlayerFragment?.view?.isGone = alpha == 0f
        if (!resources.isLandscape) {
            binding.navigationView.translationY = progress * 500
            binding.navigationView.alpha = alpha
        }
        binding.playerContainer.alpha = (progress - 0.2F) / 0.2F
    }

    private fun updateColor() {
        libraryViewModel.getPaletteColor().observe(this) { color ->
            this.paletteColor = color
            onPaletteColorChanged()
        }
    }

    private fun onPaletteColorChanged() {
        if (panelState == STATE_EXPANDED) {
            val isColorLight = paletteColor.isColorLight
            when (nowPlayingScreen) {
                NowPlayingScreen.Default,
                NowPlayingScreen.Plain,
                NowPlayingScreen.Peek,
                NowPlayingScreen.M3 -> {
                    setLightStatusBar(isColorLight)
                    setLightNavigationBar(isColorLight)
                }
                NowPlayingScreen.FullCover -> {
                    setLightNavigationBar(isColorLight)
                    setLightStatusBar(false)
                }
                NowPlayingScreen.Gradient -> {
                    val navigationbarColor = paletteColor.darkenColor
                    setLightNavigationBar(navigationbarColor.isColorLight)
                    setLightStatusBar(isColorLight)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        hideBottomSheet(false)
    }

    override fun onQueueChanged() {
        super.onQueueChanged()
        if (currentFragment(R.id.fragment_container) !is PlayingQueueFragment) {
            hideBottomSheet(MusicPlayer.playingQueue.isEmpty())
        }
    }

    private fun handleBackPress(): Boolean {
        if (panelState == STATE_EXPANDED || (panelState == STATE_SETTLING && panelStateBefore != STATE_EXPANDED)) {
            collapsePanel()
            return true
        }
        return false
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String?) {
        when (key) {
            TAB_TITLES_MODE -> navigationView.labelVisibilityMode = Preferences.bottomTitlesMode
            LIBRARY_CATEGORIES -> updateTabs()
            NOW_PLAYING_SCREEN -> {
                chooseFragmentForTheme()
                slidingPanel.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = if (nowPlayingScreen != NowPlayingScreen.Peek) {
                        ViewGroup.LayoutParams.MATCH_PARENT
                    } else {
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
                onServiceConnected()
            }

            ADD_EXTRA_CONTROLS -> {
                miniPlayerFragment?.setupExtraControls()
            }

            CAROUSEL_EFFECT,
            COVER_SWIPING_EFFECT,
            NOW_PLAYING_IMAGE_CORNER_RADIUS,
            CIRCLE_PLAY_BUTTON -> {
                chooseFragmentForTheme()
                onServiceConnected()
            }

            SWIPE_TO_DISMISS -> bottomSheetBehavior.isHideable =
                Preferences.swipeDownToDismiss
        }
    }

    private fun chooseFragmentForTheme() {
        nowPlayingScreen = Preferences.nowPlayingScreen

        val fragment: AbsPlayerFragment = when (nowPlayingScreen) {
            NowPlayingScreen.FullCover -> FullCoverPlayerFragment()
            NowPlayingScreen.Gradient -> GradientPlayerFragment()
            NowPlayingScreen.Peek -> PeekPlayerFragment()
            NowPlayingScreen.Plain -> PlainPlayerFragment()
            NowPlayingScreen.M3 -> M3PlayerFragment()
            else -> DefaultPlayerFragment()
        }

        supportFragmentManager.commit {
            replace(R.id.player_container, fragment)
        }
        supportFragmentManager.executePendingTransactions()
        playerFragment = whichFragment(R.id.player_container)
        miniPlayerFragment = whichFragment(R.id.mini_player_container)
        miniPlayerFragment?.view?.setOnClickListener { expandPanel() }
    }
}