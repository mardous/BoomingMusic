package com.mardous.booming.ui.screen.sharesong

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.extensions.dip
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.getValue

class ShareSongFragment: BottomSheetDialogFragment() {

    private val playerViewModel: PlayerViewModel by activityViewModel()

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
                peekHeight = dip(R.dimen.shuffle_height)
                //isDraggable = false
                maxHeight = peekHeight
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                BoomingMusicTheme() {
                    ShareSongBottomSheet(
                        playerViewModel = playerViewModel
                    )
                }
            }
        }
    }
}