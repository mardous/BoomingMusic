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

package com.mardous.booming.ui.component.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.media3.common.Player
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mardous.booming.R
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.core.model.player.PlayerColorScheme
import com.mardous.booming.core.model.player.PlayerTintTarget
import com.mardous.booming.core.model.player.ProgressState
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.getShapeAppearanceModel
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.media.asReadableDuration
import com.mardous.booming.extensions.resources.applyColor
import com.mardous.booming.ui.component.preferences.dialog.NowPlayingExtraInfoPreferenceDialog
import com.mardous.booming.ui.component.views.MusicSlider
import com.mardous.booming.ui.screen.MainActivity
import com.mardous.booming.ui.screen.player.PlayerAnimator
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.util.DISPLAY_ALBUM_TITLE
import com.mardous.booming.util.DISPLAY_EXTRA_INFO
import com.mardous.booming.util.ENABLE_SCROLLING_TEXT
import com.mardous.booming.util.EXTRA_INFO
import com.mardous.booming.util.PREFER_ALBUM_ARTIST_NAME
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.SQUIGGLY_SEEK_BAR
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.lang.ref.WeakReference

/**
 * @author Christians M. A. (mardous)
 */
abstract class AbsPlayerControlsFragment(@LayoutRes layoutRes: Int) : Fragment(layoutRes),
    View.OnClickListener,
    View.OnLongClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener,
    SkipButtonTouchHandler.Callback {

    val playerViewModel: PlayerViewModel by activityViewModel()

    protected var playerFragment: AbsPlayerFragment? = null
    private var playerAnimator: PlayerAnimator? = null

    protected open val playPauseFab: FloatingActionButton? = null
    protected open val musicSlider: MusicSlider? = null
    protected open val repeatButton: MaterialButton? = null
    protected open val shuffleButton: MaterialButton? = null
    protected open val songTotalTime: TextView? = null
    protected open val songCurrentProgress: TextView? = null
    protected open val songTitleView: TextView? = null
    protected open val songArtistView: TextView? = null
    protected open val songInfoView: TextView? = null

    protected val isShuffleModeOn: Boolean
        get() = playerViewModel.shuffleModeEnabled

    protected val isRepeatModeOn: Boolean
        get() = playerViewModel.repeatMode != Player.REPEAT_MODE_OFF

    private var lastPlaybackControlsColor: Int = 0
    private var lastDisabledPlaybackControlsColor: Int = 0

    private var isShown = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        playerFragment = parentFragment as? AbsPlayerFragment
            ?: error("${javaClass.name} must be a child of ${AbsPlayerFragment::class.java.name}")
    }

    override fun onDetach() {
        super.onDetach()
        playerFragment = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.layoutDirection = View.LAYOUT_DIRECTION_LTR
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.colorSchemeFlow.collect { scheme ->
                lastPlaybackControlsColor = scheme.onSurfaceColor
                lastDisabledPlaybackControlsColor = scheme.onSurfaceVariantColor
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            combine(playerViewModel.currentSongFlow, playerViewModel.nextSongFlow)
            { currentSong, nextSong -> Pair(currentSong, nextSong) }.collect { (current, next) ->
                onSongInfoChanged(current, next)
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.extraInfoFlow.collect { extraInfo ->
                onExtraInfoChanged(extraInfo)
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.isPlayingFlow.collect { isPlaying ->
                onUpdatePlayPause(isPlaying)
                musicSlider?.animateSquigglyProgress = isPlaying
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.repeatModeFlow.collect { repeatMode ->
                onUpdateRepeatMode(repeatMode)
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.shuffleModeFlow.collect { shuffleMode ->
                onUpdateShuffleMode(shuffleMode)
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            combine(
                playerViewModel.progressFlow,
                playerViewModel.durationFlow
            ) { progress, duration -> ProgressState(progress, duration) }
                .filter { progress -> progress.mayUpdateUI }
                .collectLatest { progress ->
                    if (musicSlider?.isTrackingTouch == false) {
                        onUpdateSlider(progress.progress, progress.total)
                    }
                }
        }
        Preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStart() {
        super.onStart()
        playerAnimator = onCreatePlayerAnimator()
        if (Preferences.circularPlayButton) {
            playPauseFab?.shapeAppearanceModel = requireContext().getShapeAppearanceModel(
                com.google.android.material.R.style.ShapeAppearance_Material3_Corner_Large,
                R.style.CircularShapeAppearance
            )
        }
        songTotalTime?.setOnClickListener(this)
        songInfoView?.setOnLongClickListener(this)
        setUpProgressSlider()
    }

    override fun onResume() {
        super.onResume()
        val isShown = (activity as? MainActivity)?.panelState == STATE_EXPANDED
        if (isShown && playerAnimator?.isPrepared == true) {
            onShow()
        } else if (!isShown && playerAnimator?.isPrepared == false) {
            onHide()
        } else {
            onShow()
        }
    }

    override fun onClick(view: View) {
        when (view) {
            songTotalTime -> {
                val preferRemainingTime = Preferences.preferRemainingTime
                Preferences.preferRemainingTime = !preferRemainingTime
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        if (view.id == R.id.songInfo) {
            NowPlayingExtraInfoPreferenceDialog().show(childFragmentManager, "NOW_PLAYING_EXTRA_INFO")
            return true
        }
        return false
    }

    override fun onSkipButtonHold(direction: Int) {
        when (direction) {
            SkipButtonTouchHandler.DIRECTION_NEXT -> playerViewModel.seekForward()
            SkipButtonTouchHandler.DIRECTION_PREVIOUS -> playerViewModel.seekBack()
        }
    }

    override fun onSkipButtonTap(direction: Int) {
        when (direction) {
            SkipButtonTouchHandler.DIRECTION_NEXT -> playerViewModel.seekToNext()
            SkipButtonTouchHandler.DIRECTION_PREVIOUS -> playerViewModel.seekToPrevious()
        }
    }

    protected fun getSkipButtonTouchHandler(direction: Int): SkipButtonTouchHandler {
        return SkipButtonTouchHandler(direction, this)
    }

    private fun setUpProgressSlider() {
        musicSlider?.setUseSquiggly(Preferences.squigglySeekBar)
        musicSlider?.setListener(object : MusicSlider.Listener {
            override fun onProgressChanged(slider: MusicSlider, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    onUpdateSlider(progress.toLong(), playerViewModel.duration)
                }
            }

            override fun onStartTrackingTouch(slider: MusicSlider) {}

            override fun onStopTrackingTouch(slider: MusicSlider) {
                playerViewModel.seekTo(slider.value.toLong())
            }
        })
    }

    private fun onUpdateSlider(progress: Long, total: Long) {
        musicSlider?.valueTo = total.toInt()
        musicSlider?.value = progress.toInt()
        songCurrentProgress?.text = progress.asReadableDuration()
        songTotalTime?.text = if (Preferences.preferRemainingTime) {
            (total - progress).coerceAtLeast(0L).asReadableDuration()
        } else {
            total.asReadableDuration()
        }
    }

    protected open fun onCreatePlayerAnimator(): PlayerAnimator? = null

    protected abstract fun onSongInfoChanged(currentSong: Song, nextSong: Song)

    protected abstract fun onExtraInfoChanged(extraInfo: String?)

    protected abstract fun onUpdatePlayPause(isPlaying: Boolean)

    open fun onUpdateRepeatMode(repeatMode: Int) {
        val iconResource = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_24dp
            else -> R.drawable.ic_repeat_24dp
        }
        repeatButton?.let {
            it.setIconResource(iconResource)
            it.applyColor(
                getPlaybackControlsColor(repeatMode != Player.REPEAT_MODE_OFF),
                isIconButton = true
            )
        }
    }

    open fun onUpdateShuffleMode(shuffleModeEnabled: Boolean) {
        shuffleButton?.applyColor(
            getPlaybackControlsColor(shuffleModeEnabled),
            isIconButton = true
        )
    }

    fun setMarquee(vararg textView: TextView?, marquee: Boolean) {
        if (isShown) {
            playerFragment?.setMarquee(textView = textView, marquee = marquee)
        }
    }

    /**
     * Called to notify that the player has been expanded.
     */
    internal open fun onShow() {
        isShown = true
        playerAnimator?.start()
        setMarquee(songTitleView, songArtistView, songInfoView, marquee = true)
    }

    /**
     * Called to notify that the player has been collapsed.
     */
    internal open fun onHide() {
        playerAnimator?.prepare()
        setMarquee(songTitleView, songArtistView, songInfoView, marquee = false)
        isShown = false
    }

    abstract fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget>

    protected fun setViewAction(view: View, action: NowPlayingAction) =
        playerFragment?.setViewAction(view, action)

    protected fun getSongArtist(song: Song) =
        playerFragment?.getSongArtist(song)

    protected fun getNextSongInfo(nextSong: Song) =
        playerFragment?.getNextSongInfo(nextSong)

    protected fun isExtraInfoEnabled() =
        playerFragment?.isExtraInfoEnabled() ?: false

    protected fun getPlaybackControlsColor(
        isEnabled: Boolean,
        controlColor: Int = lastPlaybackControlsColor,
        disabledControlColor: Int = lastDisabledPlaybackControlsColor
    ) = if (isEnabled) controlColor else disabledControlColor

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            SQUIGGLY_SEEK_BAR -> {
                musicSlider?.setUseSquiggly(sharedPreferences.getBoolean(key, false))
                musicSlider?.animateSquigglyProgress = playerViewModel.isPlaying
            }
            ENABLE_SCROLLING_TEXT -> {
                val marquee = sharedPreferences.getBoolean(key, false)
                setMarquee(songTitleView, songArtistView, songInfoView, marquee = marquee)
            }
            DISPLAY_ALBUM_TITLE,
            PREFER_ALBUM_ARTIST_NAME -> onSongInfoChanged(playerViewModel.currentSong, playerViewModel.nextSong)
            DISPLAY_EXTRA_INFO,
            EXTRA_INFO -> playerViewModel.generateExtraInfo()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}

class SkipButtonTouchHandler(
    private val direction: Int,
    private val callback: Callback
) : OnTouchListener {

    interface Callback {
        fun onSkipButtonHold(direction: Int)
        fun onSkipButtonTap(direction: Int)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isHolding = false
    private var touchedViewRef: WeakReference<View>? = null

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val view = touchedViewRef?.get()
            if (view != null && view.isEnabled) {
                isHolding = true
                callback.onSkipButtonHold(direction)
                handler.postDelayed(this, SKIP_TRIGGER_NORMAL_INTERVAL_MILLIS)
            } else {
                cancel()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchedViewRef = WeakReference(view)
                isHolding = false
                view.isPressed = true
                handler.postDelayed(repeatRunnable, SKIP_TRIGGER_INITIAL_INTERVAL_MILLIS)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(repeatRunnable)
                view.isPressed = false

                if (!isHolding) {
                    callback.onSkipButtonTap(direction)
                }

                touchedViewRef?.clear()
                touchedViewRef = null
                isHolding = false
                return true
            }
        }
        return false
    }

    private fun cancel() {
        handler.removeCallbacks(repeatRunnable)
        touchedViewRef?.get()?.isPressed = false
        touchedViewRef = null
        isHolding = false
    }

    companion object {
        private const val SKIP_TRIGGER_INITIAL_INTERVAL_MILLIS = 1000L
        private const val SKIP_TRIGGER_NORMAL_INTERVAL_MILLIS = 250L

        const val DIRECTION_NEXT = 1
        const val DIRECTION_PREVIOUS = 2
    }
}