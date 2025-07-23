package com.mardous.booming.fragments.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.mardous.booming.extensions.getOnBackPressedDispatcher
import com.mardous.booming.extensions.materialSharedAxis
import com.mardous.booming.ui.screens.about.OSSLicensesScreen
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.util.Preferences

class LicensesFragment : Fragment() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                BoomingMusicTheme(dynamicColor = Preferences.materialYou) {
                    OSSLicensesScreen {
                        getOnBackPressedDispatcher().onBackPressed()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        materialSharedAxis(view)
    }
}