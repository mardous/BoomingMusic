package com.mardous.booming.ui.screen.settings

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import com.mardous.booming.R
import com.mardous.booming.extensions.hasS
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.screen.lyrics.LyricsViewModel
import com.mardous.booming.ui.screen.lyrics.LyricsViewSettings
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.getValue

class LyricsPreferencesFragment : PreferencesScreenFragment() {

    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private val importFontLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            lyricsViewModel.importCustomFont(requireContext(), it).observe(viewLifecycleOwner) { success ->
                if (success) {
                    showToast(R.string.font_imported_successfully)
                } else {
                    showToast(R.string.could_not_import_font)
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_lyrics)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findPreference<Preference>(LyricsViewSettings.Key.BLUR_EFFECT)
            ?.isVisible = hasS()

        findPreference<Preference>("lyrics_custom_font")
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                importFontLauncher.launch(
                    arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf")
                )
                true
            }

        findPreference<Preference>("clear_lyrics")
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                lyricsViewModel.deleteLyrics()
                showToast(R.string.lyrics_cleared)
                true
            }
    }
}