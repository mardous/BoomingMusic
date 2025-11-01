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

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import coil3.SingletonImageLoader
import com.mardous.booming.R
import com.mardous.booming.util.IGNORE_MEDIA_STORE
import com.mardous.booming.util.PREFERRED_IMAGE_SIZE
import com.mardous.booming.util.USE_FOLDER_ART
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author Christians M. A. (mardous)
 */
class MetadataPreferencesFragment : PreferencesScreenFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_metadata)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findPreference<Preference>(IGNORE_MEDIA_STORE)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
            }

        findPreference<Preference>(PREFERRED_IMAGE_SIZE)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
            }

        findPreference<Preference>(USE_FOLDER_ART)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
            }
    }

    private fun clearImageLoaderCache() = lifecycleScope.launch(Dispatchers.IO) {
        val imageLoader = SingletonImageLoader.get(requireContext())
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}