package com.mardous.booming.ui.screen.websearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.R
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.dip
import com.mardous.booming.ui.theme.BoomingMusicTheme
import kotlin.getValue

class WebSearchFragment: BottomSheetDialogFragment() {

    private val navArgs: WebSearchFragmentArgs by navArgs()

    private val song: Song
        get() = navArgs.extraSong

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
                    WebSearchBottomSheet(
                        song = song,
                        context = requireActivity()
                    )
                }
            }
        }
    }
}