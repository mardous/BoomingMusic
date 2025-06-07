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

package com.mardous.booming.fragments.player.styles.gradientstyle

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updatePadding
import com.mardous.booming.R
import com.mardous.booming.databinding.FragmentGradientPlayerBinding
import com.mardous.booming.extensions.resources.darkenColor
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.fragments.player.*
import com.mardous.booming.fragments.player.base.AbsPlayerControlsFragment
import com.mardous.booming.fragments.player.base.AbsPlayerFragment
import com.mardous.booming.model.NowPlayingAction
import com.mardous.booming.model.Song
import com.mardous.booming.model.theme.NowPlayingScreen
import com.mardous.booming.service.MusicPlayer
import com.mardous.booming.util.Preferences

class GradientPlayerFragment : AbsPlayerFragment(R.layout.fragment_gradient_player), View.OnClickListener {

    private var _binding: FragmentGradientPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var controlsFragment: GradientPlayerControlsFragment

    override val colorSchemeMode: PlayerColorSchemeMode
        get() = Preferences.getNowPlayingColorSchemeMode(NowPlayingScreen.Gradient)

    override val playerControlsFragment: AbsPlayerControlsFragment
        get() = controlsFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGradientPlayerBinding.bind(view)
        ViewCompat.setOnApplyWindowInsetsListener(binding.darkColorBackground) { v: View, insets: WindowInsetsCompat ->
            val navigationBar = insets.getInsets(Type.systemBars())
            v.updatePadding(bottom = navigationBar.bottom)
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            insets
        }
        setupListeners()
    }

    private fun setupListeners() {
        binding.nextSongLabel.setOnClickListener(this)
        binding.volumeIcon.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v) {
            binding.nextSongLabel -> onQuickActionEvent(NowPlayingAction.OpenPlayQueue)
            binding.volumeIcon -> onQuickActionEvent(NowPlayingAction.SoundSettings)
        }
    }

    private fun updateNextSong() {
        val nextSong = MusicPlayer.getNextSong()
        if (nextSong != null) {
            binding.nextSongLabel.text = nextSong.title
        } else {
            binding.nextSongLabel.setText(R.string.now_playing)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateNextSong()
    }

    override fun onPlayStateChanged() {
        super.onPlayStateChanged()
        updateNextSong()
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateNextSong()
    }

    override fun onQueueChanged() {
        super.onQueueChanged()
        updateNextSong()
    }

    override fun onToggleFavorite(song: Song, isFavorite: Boolean) {
        super.onToggleFavorite(song, isFavorite)
        onIsFavoriteChanged(isFavorite, true)
    }

    override fun onIsFavoriteChanged(isFavorite: Boolean, withAnimation: Boolean) {
        controlsFragment.setFavorite(isFavorite, withAnimation)
    }

    override fun onMenuInflated(menu: Menu) {
        super.onMenuInflated(menu)
        menu.removeItem(R.id.action_playing_queue)
        menu.removeItem(R.id.action_sound_settings)
        menu.removeItem(R.id.action_favorite)
    }

    override fun onCreateChildFragments() {
        super.onCreateChildFragments()
        controlsFragment = whichFragment(R.id.playbackControlsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldMaskColor = binding.mask.backgroundTintList?.defaultColor
            ?: Color.TRANSPARENT
        val oldPrimaryTextColor = binding.volumeIcon.iconTint.defaultColor
        return mutableListOf(
            binding.colorBackground.surfaceTintTarget(scheme.surfaceColor),
            binding.darkColorBackground.surfaceTintTarget(scheme.surfaceColor.darkenColor),
            binding.mask.tintTarget(oldMaskColor, scheme.surfaceColor),
            binding.nextSongLabel.tintTarget(oldPrimaryTextColor, scheme.primaryTextColor),
            binding.volumeIcon.iconButtonTintTarget(oldPrimaryTextColor, scheme.primaryTextColor)
        ).also {
            it.addAll(playerControlsFragment.getTintTargets(scheme))
        }
    }

    override fun onLyricsVisibilityChange(animatorSet: AnimatorSet, lyricsVisible: Boolean) {
        controlsFragment.setLyricsVisible(lyricsVisible)
        if (lyricsVisible) {
            animatorSet.play(ObjectAnimator.ofFloat(binding.mask, View.ALPHA, 0f))
        } else {
            animatorSet.play(ObjectAnimator.ofFloat(binding.mask, View.ALPHA, 1f))
        }
    }
}