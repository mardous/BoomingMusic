package com.mardous.booming.fragments.player.cover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.mardous.booming.R
import com.mardous.booming.fragments.player.base.goToDestination
import com.mardous.booming.ui.screens.lyrics.CoverLyricsScreen
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.viewmodels.lyrics.LyricsViewModel
import com.mardous.booming.viewmodels.player.PlayerViewModel
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
                            goToDestination(requireActivity(), R.id.nav_lyrics)
                        })
                }
            }
        }
    }
}