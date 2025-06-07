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
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import com.mardous.booming.extensions.media.albumArtistName
import com.mardous.booming.extensions.media.extraInfo
import com.mardous.booming.extensions.navigation.albumDetailArgs
import com.mardous.booming.extensions.navigation.artistDetailArgs
import com.mardous.booming.extensions.navigation.genreDetailArgs
import com.mardous.booming.extensions.openIntent
import com.mardous.booming.extensions.resources.animateBackgroundColor
import com.mardous.booming.extensions.resources.animateTintColor
import com.mardous.booming.extensions.resources.inflateMenu
import com.mardous.booming.extensions.showToast
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.fragments.LibraryViewModel
import com.mardous.booming.fragments.base.AbsMusicServiceFragment
import com.mardous.booming.fragments.lyrics.LyricsEditorFragmentArgs
import com.mardous.booming.fragments.player.PlayerAlbumCoverFragment
import com.mardous.booming.fragments.player.PlayerColorScheme
import com.mardous.booming.fragments.player.PlayerColorSchemeMode
import com.mardous.booming.fragments.player.PlayerTintTarget
import com.mardous.booming.helper.color.MediaNotificationProcessor
import com.mardous.booming.helper.menu.newPopupMenu
import com.mardous.booming.helper.menu.onSongMenu
import com.mardous.booming.misc.CoverSaverCoroutine
import com.mardous.booming.model.Genre
import com.mardous.booming.model.GestureOnCover
import com.mardous.booming.model.NowPlayingAction
import com.mardous.booming.model.Song
import com.mardous.booming.service.MusicPlayer
import com.mardous.booming.service.constants.ServiceEvent
import com.mardous.booming.util.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians M. A. (mardous)
 */
abstract class AbsPlayerFragment(@LayoutRes layoutRes: Int) :
    AbsMusicServiceFragment(layoutRes),
    Toolbar.OnMenuItemClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener,
    PlayerAlbumCoverFragment.Callbacks {

    val libraryViewModel: LibraryViewModel by activityViewModel()

    private var coverFragment: PlayerAlbumCoverFragment? = null
    private val coverSaver: CoverSaverCoroutine by lazy {
        CoverSaverCoroutine(requireContext(), viewLifecycleOwner.lifecycleScope, IO)
    }

    protected abstract val colorSchemeMode: PlayerColorSchemeMode
    protected abstract val playerControlsFragment: AbsPlayerControlsFragment

    protected open val playerToolbar: Toolbar?
        get() = null

    private var colorAnimatorSet: AnimatorSet? = null
    private var colorJob: Job? = null

    private var lastColorSchemeMode: PlayerColorSchemeMode? = null

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onCreateChildFragments()
        Preferences.registerOnSharedPreferenceChangeListener(this)
    }

    @CallSuper
    protected open fun onCreateChildFragments() {
        coverFragment = whichFragment(R.id.playerAlbumCoverFragment)
        coverFragment?.setCallbacks(this)
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
        val currentSong = MusicPlayer.currentSong
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
                ShareSongDialog.create(MusicPlayer.currentSong)
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
        cancelOngoingColorTransition()

        val newSchemeMode = colorSchemeMode
        val currentScheme = lastColorSchemeMode?.takeIf { it == PlayerColorSchemeMode.AppTheme }
        if (currentScheme == newSchemeMode)
            return

        colorJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                PlayerColorScheme.autoColorScheme(requireContext(), color, newSchemeMode)
            }
            if (isActive && result.isSuccess) {
                val scheme = result.getOrThrow().also {
                    lastColorSchemeMode = it.mode
                }
                applyColorScheme(scheme).start()
            } else if (result.isFailure) {
                Log.w("AbsPlayerFragment", "Failed to apply color scheme", result.exceptionOrNull())
            }
        }
    }

    protected abstract fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget>

    private fun applyColorScheme(scheme: PlayerColorScheme): AnimatorSet {
        return AnimatorSet()
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
                doOnStart { libraryViewModel.setPaletteColor(scheme.surfaceColor) }
                doOnEnd { playerControlsFragment.applyColorScheme(scheme) }
            }.also {
                colorAnimatorSet = it
            }
    }

    override fun onGestureDetected(gestureOnCover: GestureOnCover): Boolean {
        return when (gestureOnCover) {
            GestureOnCover.DoubleTap -> onQuickActionEvent(Preferences.coverDoubleTapAction)
            GestureOnCover.LongPress -> onQuickActionEvent(Preferences.coverLongPressAction)
            else -> false
        }
    }

    protected fun Menu.onLyricsVisibilityChang(lyricsVisible: Boolean) {
        val lyricsItem = findItem(R.id.action_show_lyrics)
        if (lyricsItem != null) {
            if (lyricsVisible) {
                lyricsItem.setIcon(R.drawable.ic_lyrics_24dp)
                    .setTitle(R.string.action_hide_lyrics)
            } else {
                lyricsItem.setIcon(R.drawable.ic_lyrics_outline_24dp)
                    .setTitle(R.string.action_show_lyrics)
            }
        }
    }

    override fun onLyricsVisibilityChange(animatorSet: AnimatorSet, lyricsVisible: Boolean) {
        playerToolbar?.menu?.onLyricsVisibilityChang(lyricsVisible)
    }

    protected open fun onSongInfoChanged(song: Song) {
        playerControlsFragment.onSongInfoChanged(song)
    }

    override fun onFavoritesStoreChanged() {
        super.onFavoritesStoreChanged()
        updateIsFavorite(withAnim = true)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        onSongInfoChanged(MusicPlayer.currentSong)
        playerControlsFragment.onQueueInfoChanged(MusicPlayer.getNextSongInfo(requireContext()))
        playerControlsFragment.onUpdatePlayPause(MusicPlayer.isPlaying)
        playerControlsFragment.onUpdateRepeatMode(MusicPlayer.repeatMode)
        playerControlsFragment.onUpdateShuffleMode(MusicPlayer.shuffleMode)
        updateIsFavorite(withAnim = false)
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        onSongInfoChanged(MusicPlayer.currentSong)
        playerControlsFragment.onQueueInfoChanged(MusicPlayer.getNextSongInfo(requireContext()))
        updateIsFavorite(withAnim = false)
    }

    override fun onPlayStateChanged() {
        super.onPlayStateChanged()
        playerControlsFragment.onUpdatePlayPause(MusicPlayer.isPlaying)
    }

    override fun onRepeatModeChanged() {
        super.onRepeatModeChanged()
        playerControlsFragment.onUpdateRepeatMode(MusicPlayer.repeatMode)
    }

    override fun onShuffleModeChanged() {
        super.onShuffleModeChanged()
        playerControlsFragment.onUpdateShuffleMode(MusicPlayer.shuffleMode)
    }

    override fun onQueueChanged() {
        super.onQueueChanged()
        playerControlsFragment.onQueueInfoChanged(MusicPlayer.getNextSongInfo(requireContext()))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            DISPLAY_EXTRA_INFO,
            EXTRA_INFO -> {
                playerControlsFragment.onSongInfoChanged(MusicPlayer.currentSong)
            }

            DISPLAY_ALBUM_TITLE,
            PREFER_ALBUM_ARTIST_NAME -> {
                playerControlsFragment.onSongInfoChanged(MusicPlayer.currentSong)
            }
        }
    }

    override fun onDestroyView() {
        cancelOngoingColorTransition()
        super.onDestroyView()
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    internal fun onQuickActionEvent(action: NowPlayingAction): Boolean {
        val currentSong = MusicPlayer.currentSong
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
                MusicPlayer.togglePlayPause()
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
                MusicPlayer.shuffleQueue()
                true
            }

            NowPlayingAction.Lyrics -> {
                if (coverFragment?.isShowingLyrics() == true) {
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
                toggleFavorite(currentSong)
                true
            }

            NowPlayingAction.TagEditor -> {
                val tagEditorIntent = Intent(requireContext(), SongTagEditorActivity::class.java)
                tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_ID, currentSong.id)
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

    protected fun Menu.onIsFavoriteChanged(isFavorite: Boolean, withAnimation: Boolean) {
        val iconRes = if (withAnimation) {
            if (isFavorite) R.drawable.avd_favorite else R.drawable.avd_unfavorite
        } else {
            if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
        }
        val titleRes = if (isFavorite) R.string.action_remove_from_favorites else R.string.action_add_to_favorites

        findItem(R.id.action_favorite)?.apply {
            setIcon(iconRes)
            setTitle(titleRes)
            icon.also {
                if (it is AnimatedVectorDrawable) {
                    it.start()
                }
            }
        }
    }

    protected open fun onIsFavoriteChanged(isFavorite: Boolean, withAnimation: Boolean) {
        playerToolbar?.menu?.onIsFavoriteChanged(isFavorite, withAnimation)
    }

    protected open fun onToggleFavorite(song: Song, isFavorite: Boolean) {
        val textId = when {
            isFavorite -> R.string.added_to_favorites_label
            else -> R.string.removed_from_favorites_label
        }
        showToast(textId)
    }

    private fun updateIsFavorite(song: Song = MusicPlayer.currentSong, withAnim: Boolean = false) {
        libraryViewModel.isSongFavorite(song).observe(viewLifecycleOwner) { isFavorite ->
            onIsFavoriteChanged(isFavorite, withAnim)
        }
    }

    private fun toggleFavorite(song: Song = MusicPlayer.currentSong) {
        libraryViewModel.toggleFavorite(song).observe(viewLifecycleOwner) {
            LocalBroadcastManager.getInstance(requireContext())
                .sendBroadcast(Intent(ServiceEvent.FAVORITE_STATE_CHANGED))
        }
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

    fun getExtraInfoString(song: Song) =
        if (isExtraInfoEnabled()) song.extraInfo(Preferences.nowPlayingExtraInfoList) else null

    private fun cancelOngoingColorTransition() {
        colorJob?.cancel()
        colorAnimatorSet?.cancel()
        colorAnimatorSet = null
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
            coverSaver.saveArtwork(
                MusicPlayer.currentSong,
                onPreExecute = {
                    view?.let { safeView ->
                        Snackbar.make(
                            safeView,
                            R.string.saving_cover_please_wait,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                },
                onSuccess = { uri, mimeType ->
                    view?.let { safeView ->
                        Snackbar.make(
                            safeView,
                            R.string.save_artwork_success,
                            Snackbar.LENGTH_SHORT
                        )
                            .setAction(R.string.save_artwork_view_action) {
                                try {
                                    startActivity(uri.openIntent(mimeType))
                                } catch (e: ActivityNotFoundException) {
                                    context?.showToast(e.toString())
                                }
                            }
                            .show()
                    }
                },
                onError = { errorMessage ->
                    view?.let { safeView ->
                        if (!errorMessage.isNullOrEmpty()) {
                            Snackbar.make(safeView, errorMessage, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                })
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

        val navOptions = when {
            singleTop -> navOptions { launchSingleTop = true }
            else -> null
        }
        findNavController(R.id.fragment_container)
            .navigate(destinationId, args, navOptions)
    }
}