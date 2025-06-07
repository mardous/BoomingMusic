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

package com.mardous.booming.fragments.player.styles.m3style

import android.animation.AnimatorSet
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updatePadding
import com.mardous.booming.R
import com.mardous.booming.databinding.FragmentM3PlayerBinding
import com.mardous.booming.extensions.getOnBackPressedDispatcher
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.fragments.player.*
import com.mardous.booming.fragments.player.base.AbsPlayerControlsFragment
import com.mardous.booming.fragments.player.base.AbsPlayerFragment
import com.mardous.booming.model.NowPlayingAction
import com.mardous.booming.model.theme.NowPlayingScreen
import com.mardous.booming.util.Preferences

/**
 * @author Christians M. A. (mardous)
 */
class M3PlayerFragment : AbsPlayerFragment(R.layout.fragment_m3_player) {

    private var _binding: FragmentM3PlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var controlsFragment: M3PlayerControlsFragment

    private var popupMenu: PopupMenu? = null

    override val playerControlsFragment: AbsPlayerControlsFragment
        get() = controlsFragment

    override val colorSchemeMode: PlayerColorSchemeMode
        get() = Preferences.getNowPlayingColorSchemeMode(NowPlayingScreen.M3)

    override val playerToolbar: Toolbar
        get() = binding.playerToolbar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentM3PlayerBinding.bind(view)
        setupToolbar()
        setupActions()
        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupActions() {
        popupMenu = inflateMenuInView(binding.moreAction)
        setViewAction(binding.openQueueButton, NowPlayingAction.OpenPlayQueue)
        setViewAction(binding.showLyricsButton, NowPlayingAction.Lyrics)
        setViewAction(binding.sleepTimerAction, NowPlayingAction.SleepTimer)
        setViewAction(binding.addToPlaylistAction, NowPlayingAction.AddToPlaylist)
    }

    private fun setupToolbar() {
        playerToolbar.setNavigationOnClickListener {
            getOnBackPressedDispatcher().onBackPressed()
        }
    }

    override fun onMenuInflated(menu: Menu) {
        super.onMenuInflated(menu)
        menu.removeItem(R.id.action_playing_queue)
        menu.removeItem(R.id.action_sleep_timer)
        menu.removeItem(R.id.action_show_lyrics)
        menu.removeItem(R.id.action_add_to_playlist)
    }

    override fun onCreateChildFragments() {
        super.onCreateChildFragments()
        controlsFragment = whichFragment(R.id.playbackControlsFragment)
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldColor = binding.openQueueButton.iconTint.defaultColor
        return mutableListOf(
            binding.root.surfaceTintTarget(scheme.surfaceColor),
            binding.openQueueButton.iconButtonTintTarget(oldColor, scheme.primaryControlColor),
            binding.showLyricsButton.iconButtonTintTarget(oldColor, scheme.primaryControlColor),
            binding.sleepTimerAction.iconButtonTintTarget(oldColor, scheme.primaryControlColor),
            binding.addToPlaylistAction.iconButtonTintTarget(oldColor, scheme.primaryControlColor),
            binding.moreAction.iconButtonTintTarget(oldColor, scheme.primaryControlColor),
        ).also {
            it.addAll(playerControlsFragment.getTintTargets(scheme))
        }
    }

    override fun onLyricsVisibilityChange(animatorSet: AnimatorSet, lyricsVisible: Boolean) {
        _binding?.showLyricsButton?.let {
            if (lyricsVisible) {
                it.setIconResource(R.drawable.ic_lyrics_24dp)
                it.contentDescription = getString(R.string.action_hide_lyrics)
            } else {
                it.setIconResource(R.drawable.ic_lyrics_outline_24dp)
                it.contentDescription = getString(R.string.action_show_lyrics)
            }
        }
    }

    override fun onIsFavoriteChanged(isFavorite: Boolean, withAnimation: Boolean) {
        popupMenu?.menu?.onIsFavoriteChanged(isFavorite, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}