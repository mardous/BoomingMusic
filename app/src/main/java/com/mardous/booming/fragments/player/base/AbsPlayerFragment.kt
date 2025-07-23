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

package com.mardous.booming.fragments.player.base

import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.*
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mardous.booming.R
import com.mardous.booming.activities.MainActivity
import com.mardous.booming.activities.tageditor.AbsTagEditorActivity
import com.mardous.booming.activities.tageditor.SongTagEditorActivity
import com.mardous.booming.dialogs.LyricsDialog
import com.mardous.booming.dialogs.SleepTimerDialog
import com.mardous.booming.dialogs.WebSearchDialog
import com.mardous.booming.dialogs.playlists.AddToPlaylistDialog
import com.mardous.booming.dialogs.songs.ShareSongDialog
import com.mardous.booming.extensions.currentFragment
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.media.albumArtistName
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.extensions.navigation.albumDetailArgs
import com.mardous.booming.extensions.navigation.artistDetailArgs
import com.mardous.booming.extensions.navigation.genreDetailArgs
import com.mardous.booming.extensions.requestView
import com.mardous.booming.extensions.resources.animateBackgroundColor
import com.mardous.booming.extensions.resources.animateTintColor
import com.mardous.booming.extensions.resources.inflateMenu
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.fragments.lyrics.LyricsEditorFragmentArgs
import com.mardous.booming.fragments.player.PlayerAlbumCoverFragment
import com.mardous.booming.fragments.player.PlayerColorScheme
import com.mardous.booming.fragments.player.PlayerColorSchemeMode
import com.mardous.booming.fragments.player.PlayerTintTarget
import com.mardous.booming.helper.color.MediaNotificationProcessor
import com.mardous.booming.helper.menu.newPopupMenu
import com.mardous.booming.helper.menu.onSongMenu
import com.mardous.booming.model.Genre
import com.mardous.booming.model.GestureOnCover
import com.mardous.booming.model.NowPlayingAction
import com.mardous.booming.model.Song
import com.mardous.booming.taglib.EditTarget
import com.mardous.booming.util.Preferences
import com.mardous.booming.viewmodels.library.LibraryViewModel
import com.mardous.booming.viewmodels.player.PlayerViewModel
import com.mardous.booming.viewmodels.player.model.MediaEvent
import kotlinx.coroutines.flow.filter
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.math.abs

/**
 * @author Christians M. A. (mardous)
 */
abstract class AbsPlayerFragment(@LayoutRes layoutRes: Int) :
    Fragment(layoutRes), Toolbar.OnMenuItemClickListener, PlayerAlbumCoverFragment.Callbacks {

    val playerViewModel: PlayerViewModel by activityViewModel()
    val libraryViewModel: LibraryViewModel by activityViewModel()

    private var coverFragment: PlayerAlbumCoverFragment? = null

    protected abstract val colorSchemeMode: PlayerColorSchemeMode
    protected abstract val playerControlsFragment: AbsPlayerControlsFragment

    protected open val playerToolbar: Toolbar?
        get() = null

    private var colorAnimatorSet: AnimatorSet? = null

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onCreateChildFragments()
        onPrepareViewGestures(view)
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.mediaEvent.filter { it == MediaEvent.FavoriteContentChanged }
                .collect {
                    updateIsFavorite(withAnim = true)
                }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.currentSongFlow.collect {
                updateIsFavorite(withAnim = false)
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.nextSongFlow.collect {
                playerControlsFragment.onQueueInfoChanged(getNextSongInfo())
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.colorSchemeFlow.collect { scheme ->
                applyColorScheme(scheme)?.start()
            }
        }
    }

    @CallSuper
    protected open fun onCreateChildFragments() {
        coverFragment = whichFragment(R.id.playerAlbumCoverFragment)
        coverFragment?.setCallbacks(this)
    }

    protected open fun onPrepareViewGestures(view: View) {
        view.setOnTouchListener(
            FlingPlayBackController(
                requireContext(),
                playerViewModel
            )
        )
    }

    internal fun inflateMenuInView(view: View?): PopupMenu? {
        if (view != null) {
            if (view is Toolbar) {
                view.inflateMenu(R.menu.menu_now_playing, this) {
                    onMenuInflated(it)
                }
            } else {
                val popupMenu = newPopupMenu(view, R.menu.menu_now_playing) {
                    onMenuInflated(it)
                }.also { popupMenu ->
                    popupMenu.setOnMenuItemClickListener { onMenuItemClick(it) }
                }
                view.setOnClickListener {
                    popupMenu.show()
                }
                return popupMenu
            }
        }
        return null
    }

    @CallSuper
    protected open fun onMenuInflated(menu: Menu) {}

    protected fun Menu.setShowAsAction(itemId: Int, mode: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM) {
        findItem(itemId)?.setShowAsAction(mode)
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        val currentSong = playerViewModel.currentSong
        return when (menuItem.itemId) {
            R.id.action_playing_queue -> {
                onQuickActionEvent(NowPlayingAction.OpenPlayQueue)
                true
            }

            R.id.action_favorite -> {
                onQuickActionEvent(NowPlayingAction.ToggleFavoriteState)
                true
            }

            R.id.action_show_lyrics -> {
                onQuickActionEvent(NowPlayingAction.Lyrics)
                true
            }

            R.id.action_sound_settings -> {
                onQuickActionEvent(NowPlayingAction.SoundSettings)
                true
            }

            R.id.action_sleep_timer -> {
                onQuickActionEvent(NowPlayingAction.SleepTimer)
                true
            }

            R.id.action_tag_editor -> {
                onQuickActionEvent(NowPlayingAction.TagEditor)
                true
            }

            R.id.action_web_search -> {
                onQuickActionEvent(NowPlayingAction.WebSearch)
                true
            }

            R.id.action_go_to_album -> {
                onQuickActionEvent(NowPlayingAction.OpenAlbum)
                true
            }

            R.id.action_go_to_artist -> {
                onQuickActionEvent(NowPlayingAction.OpenArtist)
                true
            }

            R.id.action_go_to_genre -> {
                libraryViewModel.genreBySong(currentSong).observe(viewLifecycleOwner) { genre ->
                    goToGenre(requireActivity(), genre)
                }
                true
            }

            R.id.action_share_now_playing -> {
                ShareSongDialog.create(playerViewModel.currentSong)
                    .show(childFragmentManager, "SHARE_SONG")
                true
            }

            R.id.action_equalizer -> {
                goToDestination(requireActivity(), R.id.nav_equalizer)
                true
            }

            else -> currentSong.onSongMenu(this, menuItem)
        }
    }

    override fun onColorChanged(color: MediaNotificationProcessor) {
        playerViewModel.loadColorScheme(requireContext(), colorSchemeMode, color)
    }

    protected abstract fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget>

    private fun applyColorScheme(scheme: PlayerColorScheme): AnimatorSet? {
        colorAnimatorSet?.cancel()
        colorAnimatorSet = AnimatorSet()
            .setDuration(scheme.mode.preferredAnimDuration)
            .apply {
                playTogether(getTintTargets(scheme).map {
                    if (it.isSurface) {
                        it.target.animateBackgroundColor(it.newColor)
                    } else {
                        it.target.animateTintColor(
                            it.oldColor,
                            it.newColor,
                            isIconButton = it.isIcon
                        )
                    }
                })
            }
        return colorAnimatorSet
    }

    override fun onGestureDetected(gestureOnCover: GestureOnCover): Boolean {
        return when (gestureOnCover) {
            GestureOnCover.Tap -> onQuickActionEvent(Preferences.coverSingleTapAction)
            GestureOnCover.DoubleTap -> onQuickActionEvent(Preferences.coverDoubleTapAction)
            GestureOnCover.LongPress -> onQuickActionEvent(Preferences.coverLongPressAction)
        }
    }

    protected fun Menu.onLyricsVisibilityChang(lyricsVisible: Boolean) {
        val lyricsItem = findItem(R.id.action_show_lyrics)
        if (lyricsItem != null) {
            if (lyricsVisible) {
                lyricsItem.setIcon(getTintedDrawable(R.drawable.ic_lyrics_24dp))
                    .setTitle(R.string.action_hide_lyrics)
            } else {
                lyricsItem.setIcon(getTintedDrawable(R.drawable.ic_lyrics_outline_24dp))
                    .setTitle(R.string.action_show_lyrics)
            }
        }
    }

    override fun onLyricsVisibilityChange(animatorSet: AnimatorSet, lyricsVisible: Boolean) {
        playerToolbar?.menu?.onLyricsVisibilityChang(lyricsVisible)
    }

    override fun onDestroyView() {
        colorAnimatorSet?.cancel()
        colorAnimatorSet = null
        super.onDestroyView()
    }

    internal fun onQuickActionEvent(action: NowPlayingAction): Boolean {
        val currentSong = playerViewModel.currentSong
        return when (action) {
            NowPlayingAction.OpenAlbum -> {
                goToAlbum(requireActivity(), currentSong)
                true
            }

            NowPlayingAction.OpenArtist -> {
                goToArtist(requireActivity(), currentSong)
                true
            }

            NowPlayingAction.OpenPlayQueue -> {
                findNavController().navigate(R.id.nav_queue)
                true
            }

            NowPlayingAction.TogglePlayState -> {
                playerViewModel.togglePlayPause()
                true
            }

            NowPlayingAction.WebSearch -> {
                WebSearchDialog.create(currentSong).show(childFragmentManager, "WEB_SEARCH_DIALOG")
                true
            }

            NowPlayingAction.SaveAlbumCover -> {
                requestSaveCover()
                true
            }

            NowPlayingAction.ShufflePlayQueue -> {
                playerViewModel.shuffleQueue()
                true
            }

            NowPlayingAction.Lyrics -> {
                if (coverFragment?.isAllowedToLoadLyrics == true) {
                    coverFragment?.toggleLyrics()
                } else {
                    LyricsDialog.create(currentSong).show(childFragmentManager, "LYRICS_DIALOG")
                }
                true
            }

            NowPlayingAction.LyricsEditor -> {
                goToDestination(
                    requireActivity(),
                    R.id.nav_lyrics_editor,
                    LyricsEditorFragmentArgs.Builder(currentSong)
                        .build()
                        .toBundle()
                )
                true
            }

            NowPlayingAction.AddToPlaylist -> {
                AddToPlaylistDialog.create(currentSong)
                    .show(childFragmentManager, "ADD_TO_PLAYLIST")
                true
            }

            NowPlayingAction.ToggleFavoriteState -> {
                toggleFavorite()
                true
            }

            NowPlayingAction.TagEditor -> {
                val tagEditorIntent = Intent(requireContext(), SongTagEditorActivity::class.java)
                tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_TARGET, EditTarget.song(currentSong))
                startActivity(tagEditorIntent)
                true
            }

            NowPlayingAction.SleepTimer -> {
                SleepTimerDialog().show(childFragmentManager, "SLEEP_TIMER")
                true
            }

            NowPlayingAction.SoundSettings -> {
                findNavController().navigate(R.id.nav_sound_settings)
                true
            }

            NowPlayingAction.Nothing -> false
        }
    }

    fun onShow() {
        coverFragment?.showLyrics()
        playerControlsFragment.onShow()
    }

    fun onHide() {
        coverFragment?.hideLyrics()
        playerControlsFragment.onHide()
    }

    protected fun getTintedDrawable(
        drawableRes: Int,
        color: Int = playerViewModel.colorScheme.primaryTextColor
    ) = AppCompatResources.getDrawable(requireContext(), drawableRes).also {
        it?.setTint(color)
    }

    protected fun Menu.onIsFavoriteChanged(isFavorite: Boolean, withAnimation: Boolean) {
        val iconRes = if (withAnimation) {
            if (isFavorite) R.drawable.avd_favorite else R.drawable.avd_unfavorite
        } else {
            if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
        }
        val titleRes = if (isFavorite) R.string.action_remove_from_favorites else R.string.action_add_to_favorites

        findItem(R.id.action_favorite)?.apply {
            setTitle(titleRes)
            icon = getTintedDrawable(iconRes).also {
                if (it is AnimatedVectorDrawable) {
                    it.start()
                }
            }
        }
    }

    protected open fun onIsFavoriteChanged(isFavorite: Boolean, withAnimation: Boolean) {
        playerToolbar?.menu?.onIsFavoriteChanged(isFavorite, withAnimation)
    }

    private fun updateIsFavorite(song: Song = playerViewModel.currentSong, withAnim: Boolean = false) {
        libraryViewModel.isSongFavorite(song).observe(viewLifecycleOwner) { isFavorite ->
            onIsFavoriteChanged(isFavorite, withAnim)
        }
    }

    private fun toggleFavorite() {
        playerViewModel.toggleFavorite()
    }

    fun setViewAction(view: View, action: NowPlayingAction) {
        view.setOnClickListener { onQuickActionEvent(action) }
    }

    fun getSongArtist(song: Song): String {
        val artistName = if (Preferences.preferAlbumArtistName)
            song.albumArtistName() else song.artistName
        if (Preferences.displayAlbumTitle) {
            return buildInfoString(artistName, song.albumName)
        }
        return artistName
    }

    fun isExtraInfoEnabled(): Boolean =
        Preferences.displayExtraInfo && Preferences.nowPlayingExtraInfoList.any { it.isEnabled }

    fun getNextSongInfo(): String {
        val nextSong = playerViewModel.nextSong
        return if (!nextSong.isArtistNameUnknown()) {
            getString(R.string.next_song_x_by_artist_x, nextSong.title, nextSong.displayArtistName())
        } else {
            getString(R.string.next_song_x, nextSong.title)
        }
    }

    private fun requestSaveCover() {
        if (!Preferences.savedArtworkCopyrightNoticeShown) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.save_artwork_copyright_info_title)
                .setMessage(R.string.save_artwork_copyright_info_message)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                    Preferences.savedArtworkCopyrightNoticeShown = true
                    requestSaveCover()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            playerViewModel.saveCover(playerViewModel.currentSong).observe(viewLifecycleOwner) { result ->
                requestView { view ->
                    if (result.isWorking) {
                        Snackbar.make(view, R.string.saving_cover_please_wait, Snackbar.LENGTH_SHORT)
                            .show()
                    } else if (result.uri != null) {
                        Snackbar.make(view, R.string.save_artwork_success, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.save_artwork_view_action) {
                                try {
                                    startActivity(
                                        Intent(Intent.ACTION_VIEW)
                                            .setDataAndType(result.uri, "image/jpeg")
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    )
                                } catch (_: ActivityNotFoundException) {}
                            }
                            .show()
                    } else {
                        Snackbar.make(view, R.string.save_artwork_error, Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }
}

class FlingPlayBackController(context: Context, playerViewModel: PlayerViewModel) :
    View.OnTouchListener {
    private var flingPlayBackController = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (Preferences.isSwipeControls) {
                    if (abs(velocityX) > abs(velocityY)) {
                        if (velocityX < 0) {
                            playerViewModel.playNext()
                            return true
                        } else if (velocityX > 0) {
                            playerViewModel.playPrevious()
                            return true
                        }
                    }
                }
                return false
            }
        })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return flingPlayBackController.onTouchEvent(event)
    }
}

fun goToArtist(activity: Activity, song: Song) {
    goToDestination(
        activity,
        R.id.nav_artist_detail,
        artistDetailArgs(song),
        removeTransition = true,
        singleTop = false
    )
}

fun goToAlbum(activity: Activity, song: Song) {
    goToDestination(
        activity,
        R.id.nav_album_detail,
        albumDetailArgs(song.albumId),
        removeTransition = true,
        singleTop = false
    )
}

fun goToGenre(activity: Activity, genre: Genre) {
    goToDestination(
        activity,
        R.id.nav_genre_detail,
        genreDetailArgs(genre),
        singleTop = false
    )
}

fun goToDestination(
    activity: Activity,
    destinationId: Int,
    args: Bundle? = null,
    removeTransition: Boolean = false,
    singleTop: Boolean = true
) {
    if (activity !is MainActivity) return
    activity.apply {
        if (removeTransition) {
            // Remove exit transition of current fragment, so
            // it doesn't exit with a weird transition
            currentFragment(R.id.fragment_container)?.exitTransition = null
        }

        //Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
        setBottomNavVisibility(false)
        if (getBottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) {
            collapsePanel()
        }

        val navOptions = when {
            singleTop -> navOptions { launchSingleTop = true }
            else -> null
        }
        findNavController(R.id.fragment_container)
            .navigate(destinationId, args, navOptions)
    }
}