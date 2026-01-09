package com.mardous.booming.ui.screen.sleeptimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.extensions.dip
import com.mardous.booming.playback.SleepTimer
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.getValue

class SleepTimerFragment: BottomSheetDialogFragment() {
    private val playerViewModel: PlayerViewModel by activityViewModel()

    private val viewModel: SleepTimerViewModel by viewModel()

    private val sleepTimer: SleepTimer by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            bottomSheetDialog.behavior.apply {
                isFitToContents = true
                skipCollapsed = true
                peekHeight = dip(R.dimen.sheet_height)
                //isDraggable = false
                maxHeight = peekHeight
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                BoomingMusicTheme() {
                    SleepTimerBottomSheet(
                        timerViewModel = viewModel,
                        context = requireContext(),
                        sleepTimer = sleepTimer
                    )
                }
            }
        }
    }
}