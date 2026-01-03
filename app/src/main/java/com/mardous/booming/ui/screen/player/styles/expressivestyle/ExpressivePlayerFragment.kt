package com.mardous.booming.ui.screen.player.styles.expressivestyle

import android.animation.AnimatorSet
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.media3.common.Player
import com.mardous.booming.R
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.core.model.player.*
import com.mardous.booming.core.model.theme.NowPlayingScreen
import com.mardous.booming.databinding.FragmentExpressivePlayerBinding
import com.mardous.booming.extensions.getOnBackPressedDispatcher
import com.mardous.booming.extensions.isLandscape
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.resources.applyColor
import com.mardous.booming.extensions.whichFragment
import com.mardous.booming.ui.component.base.AbsPlayerControlsFragment
import com.mardous.booming.ui.component.base.AbsPlayerFragment
import com.mardous.booming.ui.component.preferences.dialog.NowPlayingExtraInfoPreferenceDialog
import com.mardous.booming.util.Preferences

class ExpressivePlayerFragment : AbsPlayerFragment(R.layout.fragment_expressive_player),
    View.OnClickListener, View.OnLongClickListener {

    private var _binding: FragmentExpressivePlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var controlsFragment: ExpressivePlayerControlsFragment

    override val playerControlsFragment: AbsPlayerControlsFragment
        get() = controlsFragment

    override val playerToolbar: Toolbar
        get() = binding.playerToolbar

    override val colorSchemeMode: PlayerColorSchemeMode
        get() = Preferences.getNowPlayingColorSchemeMode(NowPlayingScreen.Expressive)

    private var popupMenu: PopupMenu? = null
    private var isFavorite = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExpressivePlayerBinding.bind(view)
        setupToolbar()
        setupActions()
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.repeatModeFlow.collect { repeatMode ->
                binding.repeatButton.apply {
                    val iconResource = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_24dp
                        else -> R.drawable.ic_repeat_24dp
                    }
                    setIconResource(iconResource)
                    applyColor(
                        color = if (repeatMode != Player.REPEAT_MODE_OFF) {
                            playerViewModel.colorScheme.onSurfaceColor
                        } else {
                            playerViewModel.colorScheme.onSurfaceVariantColor
                        },
                        isIconButton = true
                    )
                }
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.shuffleModeFlow.collect { shuffleModeEnabled ->
                binding.shuffleButton.apply {
                    applyColor(
                        color = if (shuffleModeEnabled) {
                            playerViewModel.colorScheme.onSurfaceColor
                        } else {
                            playerViewModel.colorScheme.onSurfaceVariantColor
                        },
                        isIconButton = true
                    )
                }
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.currentSongFlow.collect { currentSong ->
                _binding?.let { nonNullBinding ->
                    nonNullBinding.title.text = currentSong.title
                    nonNullBinding.text.text = getSongArtist(currentSong)
                }
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.extraInfoFlow.collect { extraInfo ->
                _binding?.let { nonNullBinding ->
                    if (isExtraInfoEnabled()) {
                        nonNullBinding.songInfo.text = extraInfo
                        nonNullBinding.songInfo.isVisible = true
                    } else {
                        nonNullBinding.songInfo.isVisible = false
                    }
                }
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            val displayCutout = insets.getInsets(Type.displayCutout())
            v.updatePadding(left = displayCutout.left, right = displayCutout.right)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupActions() {
        popupMenu = inflateMenuInView(binding.moreButton)
        binding.songInfo.setOnLongClickListener(this)
        binding.repeatButton.setOnClickListener(this)
        binding.shuffleButton.setOnClickListener(this)
        setViewAction(binding.favoriteButton, NowPlayingAction.ToggleFavoriteState)
        setViewAction(binding.openQueueButton, NowPlayingAction.OpenPlayQueue)
        setViewAction(binding.showLyricsButton, NowPlayingAction.Lyrics)
        binding.soundSettingsButton?.let {
            setViewAction(it, NowPlayingAction.SoundSettings)
        }
    }

    private fun setupToolbar() {
        playerToolbar.setNavigationOnClickListener {
            getOnBackPressedDispatcher().onBackPressed()
        }
    }

    override fun onClick(view: View) {
        when (view) {
            binding.repeatButton -> playerViewModel.cycleRepeatMode()
            binding.shuffleButton -> playerViewModel.toggleShuffleMode()
        }
    }

    override fun onLongClick(view: View): Boolean {
        if (binding.songInfo == view) {
            NowPlayingExtraInfoPreferenceDialog()
                .show(childFragmentManager, "NOW_PLAYING_EXTRA_INFO")
            return true
        }
        return false
    }

    override fun onMenuInflated(menu: Menu) {
        super.onMenuInflated(menu)
        menu.removeItem(R.id.action_favorite)
        menu.removeItem(R.id.action_playing_queue)
        menu.removeItem(R.id.action_show_lyrics)
        binding.soundSettingsButton.let {
            menu.removeItem(R.id.action_sound_settings)
        }
    }

    override fun onCreateChildFragments() {
        super.onCreateChildFragments()
        controlsFragment = whichFragment(R.id.playbackControlsFragment)
    }

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> {
        val oldPrimaryTextColor = binding.title.currentTextColor
        val oldSecondaryTextColor = binding.text.currentTextColor

        val oldPrimaryColor = binding.moreButton.backgroundTintList?.defaultColor
            ?: Color.TRANSPARENT
        val oldTonalColor = binding.favoriteButton.backgroundTintList?.defaultColor
            ?: Color.TRANSPARENT

        val oldIconColor = binding.showLyricsButton.iconTint.defaultColor
        val oldRepeatColor = binding.repeatButton.iconTint.defaultColor
        val newRepeatColor = if (playerViewModel.repeatMode != Player.REPEAT_MODE_OFF) {
            scheme.onSurfaceColor
        } else {
            scheme.onSurfaceVariantColor
        }
        val oldShuffleColor = binding.shuffleButton.iconTint.defaultColor
        val newShuffleColor = if (playerViewModel.shuffleModeEnabled) {
            scheme.onSurfaceColor
        } else {
            scheme.onSurfaceVariantColor
        }
        return listOfNotNull(
            binding.root.surfaceTintTarget(scheme.surfaceColor),
            binding.playerToolbar.tintTarget(oldIconColor, scheme.onSurfaceColor),
            binding.title.tintTarget(oldPrimaryTextColor, scheme.onSurfaceColor),
            binding.text.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor),
            binding.songInfo.tintTarget(oldSecondaryTextColor, scheme.onSurfaceVariantColor),
            if (isLandscape()) {
                binding.favoriteButton.iconButtonTintTarget(oldIconColor, scheme.onSurfaceColor)
            } else {
                binding.favoriteButton.tintTarget(oldTonalColor, scheme.tonalColor)
            },
            if (isLandscape()) {
                binding.openQueueButton.iconButtonTintTarget(oldIconColor, scheme.onSurfaceColor)
            } else {
                binding.openQueueButton.tintTarget(oldTonalColor, scheme.tonalColor)
            },
            binding.repeatButton.iconButtonTintTarget(oldRepeatColor, newRepeatColor),
            binding.shuffleButton.iconButtonTintTarget(oldShuffleColor, newShuffleColor),
            binding.showLyricsButton.iconButtonTintTarget(oldIconColor, scheme.onSurfaceColor),
            binding.soundSettingsButton?.iconButtonTintTarget(oldIconColor, scheme.onSurfaceColor),
            binding.moreButton.tintTarget(oldPrimaryColor, scheme.primaryColor)
        ).toMutableList().also {
            it.addAll(playerControlsFragment.getTintTargets(scheme))
        }
    }

    override fun onIsFavoriteChanged(isFavorite: Boolean, withAnimation: Boolean) {
        if (this.isFavorite != isFavorite) {
            this.isFavorite = isFavorite
            binding.favoriteButton.setIsFavorite(isFavorite, false)
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

    override fun onShow() {
        super.onShow()
        setMarquee(binding.title, binding.text, binding.songInfo, marquee = true)
    }

    override fun onHide() {
        super.onHide()
        setMarquee(binding.title, binding.text, binding.songInfo, marquee = false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}