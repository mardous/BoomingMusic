package com.mardous.booming.fragments.other

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.ui.screens.ShuffleModeBottomSheet
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.viewmodels.library.LibraryViewModel
import com.mardous.booming.viewmodels.player.PlayerViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ShuffleModeFragment : BottomSheetDialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val playerViewModel: PlayerViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                BoomingMusicTheme {
                    ShuffleModeBottomSheet(
                        libraryViewModel = libraryViewModel,
                        playerViewModel = playerViewModel
                    )
                }
            }
        }
    }
}