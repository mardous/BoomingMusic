package com.mardous.booming.ui.screen.scrobbling

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mardous.booming.data.model.network.ScrobblingService
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.presentation.screens.ScrobblingLoginScreen
import com.mardous.booming.presentation.theme.BoomingMusicTheme

class ScrobblingServiceLoginFragment : BottomSheetDialogFragment() {

    private val service: ScrobblingService by extraNotNull("service")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.let {
            it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }

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
                    ScrobblingLoginScreen(service)
                }
            }
        }
    }

    companion object {
        fun create(service: ScrobblingService) =
            ScrobblingServiceLoginFragment().withArgs {
                putSerializable("service", service)
            }
    }
}
