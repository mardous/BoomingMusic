/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.ui.screen.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.bumptech.glide.Glide
import com.mardous.booming.R
import com.mardous.booming.extensions.glide.clearCache
import com.mardous.booming.extensions.showToast
import com.mardous.booming.util.*
import com.mardous.booming.ui.screen.lyrics.LyricsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians M. A. (mardous)
 */
class MetadataPreferencesFragment : PreferencesScreenFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val lyricsViewModel: LyricsViewModel by activityViewModel()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_metadata)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Preferences.registerOnSharedPreferenceChangeListener(this)
        findPreference<Preference>(IGNORE_MEDIA_STORE)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearGlideCache()
                true
            }

        findPreference<Preference>(PREFERRED_ARTIST_IMAGE_SIZE)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearGlideCache()
                true
            }

        findPreference<Preference>(USE_FOLDER_ART)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearGlideCache()
                true
            }

        findPreference<Preference>("clear_lyrics")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                lyricsViewModel.deleteLyrics()
                showToast(R.string.lyrics_cleared)
                true
            }

        updateOnlineArtistImagesState()
    }

    private fun updateOnlineArtistImagesState() {
        findPreference<Preference>(ALLOW_ONLINE_ARTIST_IMAGES)?.isEnabled =
            Preferences.autoDownloadMetadataPolicy != AutoDownloadMetadataPolicy.NEVER
    }

    private fun clearGlideCache() {
        lifecycleScope.launch {
            Glide.get(requireContext()).clearCache(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        when (key) {
            AUTO_DOWNLOAD_METADATA_POLICY -> updateOnlineArtistImagesState()
        }
    }
}