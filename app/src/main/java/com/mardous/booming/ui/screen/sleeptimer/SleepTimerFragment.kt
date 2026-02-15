package com.mardous.booming.ui.screen.sleeptimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class SleepTimerFragment: BottomSheetDialogFragment() {

    private val viewModel: SleepTimerViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BoomingMusicTheme {
                    SleepTimerBottomSheet(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}