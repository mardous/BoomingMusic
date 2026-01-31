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

import android.animation.AnimatorSet
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import coil3.load
import coil3.request.crossfade
import coil3.request.transformations
import coil3.size.Precision
import coil3.size.Scale
import com.commit451.coiltransformations.BlurTransformation
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mardous.booming.R
import com.mardous.booming.core.model.MediaEvent
import com.mardous.booming.core.model.PaletteColor
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.core.model.player.PlayerColorScheme
import com.mardous.booming.core.model.player.PlayerColorSchemeMode
import com.mardous.booming.core.model.player.PlayerTintTarget
import com.mardous.booming.data.local.EditTarget
import com.mardous.booming.data.model.Genre
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.currentFragment
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.media.albumArtistName
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.extensions.navigation.albumDetailArgs
import com.mardous.booming.extensions.navigation.artistDetailArgs
import com.mardous.booming.extensions.navigation.findActivityNavController
import com.mardous.booming.extensions.navigation.genreDetailArgs
import com.mardous.booming.extensions.requestView
import com.mardous.booming.extensions.resources.animateBackgroundColor
import com.mardous.booming.extensions.resources.animateTintColor
import com.mardous.booming.extensions.resources.inflateMenu
import com.mardous.booming.extensions.resources.setMarquee
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.ui.component.menu.newPopupMenu
import com.mardous.booming.ui.component.menu.onSongMenu
import com.mardous.booming.ui.dialogs.WebSearchDialog
import com.mardous.booming.ui.dialogs.playlists.AddToPlaylistDialog
import com.mardous.booming.ui.dialogs.songs.DeleteSongsDialog
import com.mardous.booming.ui.dialogs.songs.ShareSongDialog
import com.mardous.booming.ui.screen.MainActivity
import com.mardous.booming.ui.screen.equalizer.EqualizerFragment
import com.mardous.booming.ui.screen.equalizer.EqualizerFragmentArgs
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.lyrics.LyricsEditorFragmentArgs
import com.mardous.booming.ui.screen.lyrics.LyricsFragment
import com.mardous.booming.ui.screen.player.PlayerGesturesController
import com.mardous.booming.ui.screen.player.PlayerGesturesController.GestureType
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.screen.player.cover.CoverPagerFragment
import com.mardous.booming.ui.screen.tageditor.SongTagEditorActivity
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians M. A. (mardous)
 */
abstract class AbsPlayerFragment(@LayoutRes layoutRes: Int) : Fragment(layoutRes),
    Toolbar.OnMenuItemClickListener,
    PlayerGesturesController.Listener,
    CoverPagerFragment.Callbacks {

    val playerViewModel: PlayerViewModel by activityViewModel()
    val libraryViewModel: LibraryViewModel by activityViewModel()

    private var gesturesController: PlayerGesturesController? = null
    private var coverFragment: CoverPagerFragment? = null

    protected abstract val colorSchemeMode: PlayerColorSchemeMode
    protected abstract val playerControlsFragment: AbsPlayerControlsFragment

    protected open val playerToolbar: Toolbar?
        get() = null

    protected open val blurView: ImageView?
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
            playerViewModel.colorSchemeFlow.collect { scheme ->
                applyColorScheme(scheme)?.start()
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            combine(
                playerViewModel.currentSongFlow,
                playerViewModel.colorSchemeFlow
            ) { song, scheme -> Pair(song, scheme) }
                .filter { (song, scheme) -> song.id != -1L && scheme != PlayerColorScheme.Unspecified }
                .collect { (song, scheme) ->
                    applyBlur(song, scheme)
                }
        }
    }

    @CallSuper
    protected open fun onCreateChildFragments() {
        coverFragment = whichFragment(R.id.playerAlbumCoverFragment)
        coverFragment?.setCallbacks(this)
    }

    protected open fun onPrepareViewGestures(view: View) {
        gesturesController = PlayerGesturesController(
            context = view.context,
            acceptedGestures = setOf(
                GestureType.Fling(GestureType.Fling.DIRECTION_UP),
                GestureType.Fling(GestureType.Fling.DIRECTION_LEFT),
                GestureType.Fling(GestureType.Fling.DIRECTION_RIGHT)
            ),
            listener = this
        )
        view.setOnTouchListener(gesturesController)
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
                if (currentFragment(R.id.fragment_container) is EqualizerFragment) {
                    (activity as? MainActivity)?.collapsePanel()
                } else {
                    goToDestination(
                        activity = requireActivity(),
                        destinationId = R.id.nav_equalizer,
                        args = EqualizerFragmentArgs.Builder()
                            .setFromPlayer(true)
                            .build()
                            .toBundle()
                    )
                }
                true
            }

            else -> currentSong.onSongMenu(this, menuItem)
        }
    }

    override fun gestureDetected(gestureType: GestureType): Boolean {
        return when (gestureType) {
            is GestureType.Tap -> onQuickActionEvent(Preferences.coverSingleTapAction)
            is GestureType.LongPress -> onQuickActionEvent(Preferences.coverLongPressAction)
            is GestureType.DoubleTap -> {
                when (gestureType.type) {
                    GestureType.DoubleTap.TYPE_LEFT_EDGE -> {
                        val action = Preferences.coverLeftDoubleTapAction
                            .takeIf { it != NowPlayingAction.Nothing }
                            ?: Preferences.coverDoubleTapAction

                        onQuickActionEvent(action)
                    }

                    GestureType.DoubleTap.TYPE_RIGHT_EDGE -> {
                        val action = Preferences.coverRightDoubleTapAction
                            .takeIf { it != NowPlayingAction.Nothing }
                            ?: Preferences.coverDoubleTapAction

                        onQuickActionEvent(action)
                    }

                    GestureType.DoubleTap.TYPE_CENTER -> {
                        onQuickActionEvent(Preferences.coverDoubleTapAction)
                    }

                    else -> false
                }
            }
            is GestureType.Fling -> {
                when (gestureType.direction) {
                    GestureType.Fling.DIRECTION_LEFT -> {
                        if (Preferences.isSwipeAnywhere) {
                            playerViewModel.seekToNext()
                            true
                        } else false
                    }

                    GestureType.Fling.DIRECTION_RIGHT -> {
                        if (Preferences.isSwipeAnywhere) {
                            playerViewModel.seekToPrevious()
                            true
                        } else false
                    }

                    GestureType.Fling.DIRECTION_UP -> {
                        if (Preferences.isSwipeUpQueue) {
                            findNavController().navigate(R.id.nav_queue)
                            true
                        } else false
                    }

                    else -> false
                }
            }
        }
    }

    override fun onColorChanged(color: PaletteColor) {
        playerViewModel.generatePlayerScheme(requireContext(), colorSchemeMode, color)
    }

    protected abstract fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget>

    private fun applyColorScheme(scheme: PlayerColorScheme): AnimatorSet? {
        cancelColorAnimator()
        colorAnimatorSet = AnimatorSet()
            .setDuration(scheme.mode.preferredAnimDuration)
            .apply {
                val tintTargets = getTintTargets(scheme).mapTo(mutableListOf()) {
                    if (it.isSurface) {
                        it.target.animateBackgroundColor(it.newColor)
                    } else {
                        it.target.animateTintColor(
                            fromColor = it.oldColor,
                            toColor = it.newColor,
                            isForeground = it.isForeground,
                            isIconButton = it.isIcon
                        )
                    }
                }
                blurView?.let {
                    if (scheme.blurToken.isBlur) {
                        tintTargets.add(
                            it.animateTintColor(
                                fromColor = it.foregroundTintList?.defaultColor
                                    ?: Color.TRANSPARENT,
                                toColor = scheme.surfaceColor,
                                isForeground = true
                            )
                        )
                    }
                }
                playTogether(tintTargets)
            }
        return colorAnimatorSet
    }

    private fun cancelColorAnimator() {
        colorAnimatorSet?.cancel()
        colorAnimatorSet = null
    }

    private fun applyBlur(song: Song, scheme: PlayerColorScheme) {
        blurView?.let {
            if (scheme.blurToken.isBlur) {
                it.isVisible = true
                it.load(song) {
                    size(400)
                    precision(Precision.EXACT)
                    scale(Scale.FILL)
                    memoryCacheKey("nowplaying:song:${song.id}")
                    crossfade(1000)
                    transformations(BlurTransformation(
                        context = it.context,
                        radius = scheme.blurToken.blurRadius.coerceIn(0f, 25f)
                    ))
                }
            } else {
                it.isGone = true
                it.setImageDrawable(null)
            }
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
        view?.setOnTouchListener(null)
        gesturesController?.release()
        gesturesController = null
        cancelColorAnimator()
        coverFragment = null
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

            NowPlayingAction.DeleteFromDevice -> {
                DeleteSongsDialog.create(currentSong).show(childFragmentManager, "DELETE_SONGS")
                true
            }

            NowPlayingAction.Lyrics -> {
                if (coverFragment?.isAllowedToLoadLyrics == true) {
                    coverFragment?.toggleLyrics()
                } else {
                    if (currentFragment(R.id.fragment_container) is LyricsFragment) {
                        (activity as? MainActivity)?.collapsePanel()
                    } else {
                        goToDestination(requireActivity(), R.id.nav_lyrics)
                    }
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
                findActivityNavController(R.id.fragment_container)
                    .navigate(R.id.nav_sleep_timer)
                true
            }

            NowPlayingAction.SoundSettings -> {
                findNavController().navigate(R.id.nav_sound_settings)
                true
            }

            NowPlayingAction.SeekBackward -> {
                playerViewModel.seekBack()
                true
            }

            NowPlayingAction.SeekForward -> {
                playerViewModel.seekForward()
                true
            }

            NowPlayingAction.Nothing -> false
        }
    }

    @CallSuper
    open fun onShow() {
        coverFragment?.showLyrics()
        playerControlsFragment.onShow()
    }

    @CallSuper
    open fun onHide() {
        coverFragment?.hideLyrics()
        playerControlsFragment.onHide()
    }

    protected fun getTintedDrawable(
        drawableRes: Int,
        color: Int = playerViewModel.colorScheme.onSurfaceColor
    ) = AppCompatResources.getDrawable(requireContext(), drawableRes).also {
        it?.setTint(color)
    }

    protected open fun onIsFavoriteChanged(isFavorite: Boolean, withAnimation: Boolean) {
        playerToolbar?.menu?.setIsFavorite(isFavorite, withAnimation)
    }

    private fun updateIsFavorite(song: Song = playerViewModel.currentSong, withAnim: Boolean = false) {
        libraryViewModel.isSongFavorite(song).observe(viewLifecycleOwner) { isFavorite ->
            onIsFavoriteChanged(isFavorite, withAnim)
        }
    }

    private fun toggleFavorite() {
        playerViewModel.toggleFavorite()
    }

    fun setMarquee(vararg textView: TextView?, marquee: Boolean) {
        val scrollingTextEnabled = Preferences.enableScrollingText
        textView.forEach { it?.setMarquee(marquee && scrollingTextEnabled) }
    }

    fun MaterialButton.setIsFavorite(isFavorite: Boolean, withAnimation: Boolean) {
        /*
        val iconRes = if (withAnimation) {
            if (isFavorite) R.drawable.avd_favorite else R.drawable.avd_unfavorite
        } else {
            if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
        }
         */
        // There's a bug in the Material Components library that affects the
        // icon animation on a MaterialButton, so for now, we'll change the
        // icon in a simple way.
        val iconRes = if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
        icon = ContextCompat.getDrawable(context, iconRes).also { drawable ->
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
        }
    }

    protected fun Menu.setIsFavorite(isFavorite: Boolean, withAnimation: Boolean) {
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

    fun setViewAction(view: View, action: NowPlayingAction) {
        view.setOnClickListener { onQuickActionEvent(action) }
    }

    fun getSongArtist(song: Song): CharSequence {
        val artistName = if (Preferences.preferAlbumArtistName) {
            song.albumArtistName().displayArtistName()
        } else {
            song.displayArtistName()
        }
        if (Preferences.displayAlbumTitle) {
            return buildInfoString(artistName, song.albumName)
        }
        return artistName
    }

    fun isExtraInfoEnabled(): Boolean =
        Preferences.displayExtraInfo && Preferences.nowPlayingExtraInfoList.any { it.isEnabled }

    fun getNextSongInfo(nextSong: Song): String {
        return if (nextSong != Song.emptySong) {
            if (!nextSong.isArtistNameUnknown()) {
                getString(R.string.next_song_x_by_artist_x, nextSong.title, nextSong.displayArtistName())
            } else {
                getString(R.string.next_song_x, nextSong.title)
            }
        } else {
            getString(R.string.list_end)
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

        Handler(Looper.getMainLooper()).postDelayed(250) {
            val navOptions = when {
                singleTop -> navOptions { launchSingleTop = true }
                else -> null
            }
            findNavController(R.id.fragment_container)
                .navigate(destinationId, args, navOptions)
        }
    }
}