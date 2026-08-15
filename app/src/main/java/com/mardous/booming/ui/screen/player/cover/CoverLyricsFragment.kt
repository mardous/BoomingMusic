package com.mardous.booming.ui.screen.player.cover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.mardous.booming.R
import com.mardous.booming.extensions.currentFragment
import com.mardous.booming.presentation.MainActivity
import com.mardous.booming.presentation.screens.CoverLyricsScreen
import com.mardous.booming.presentation.theme.BoomingMusicTheme
import com.mardous.booming.ui.component.base.goToDestination
import com.mardous.booming.ui.screen.lyrics.LyricsFragment

class CoverLyricsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                BoomingMusicTheme {
                    CoverLyricsScreen(
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
}