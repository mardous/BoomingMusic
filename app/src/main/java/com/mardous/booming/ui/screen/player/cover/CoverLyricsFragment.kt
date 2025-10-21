package com.mardous.booming.ui.screen.player.cover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.mardous.booming.R
import com.mardous.booming.extensions.currentFragment
import com.mardous.booming.extensions.keepScreenOn
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.ui.component.base.goToDestination
import com.mardous.booming.ui.screen.MainActivity
import com.mardous.booming.ui.screen.lyrics.CoverLyricsScreen
import com.mardous.booming.ui.screen.lyrics.LyricsFragment
import com.mardous.booming.ui.screen.lyrics.LyricsViewModel
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class CoverLyricsFragment : Fragment() {

    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private val playerViewModel: PlayerViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BoomingMusicTheme {
                    CoverLyricsScreen(
                        lyricsViewModel,
                        playerViewModel,
                        onExpandClick = {
                            if (currentFragment(R.id.fragment_container) is LyricsFragment) {
                                (activity as? MainActivity)?.collapsePanel()
                            } else {
                                goToDestination(requireActivity(), R.id.nav_lyrics)
                            }
                        })
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle(Lifecycle.State.RESUMED) {
            playerViewModel.isPlayingFlow.collect { isPlaying ->
                activity?.keepScreenOn(isPlaying)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        activity?.keepScreenOn(false)
    }
}