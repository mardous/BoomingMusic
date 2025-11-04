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
import com.mardous.booming.data.local.room.InclExclDao
import com.mardous.booming.extensions.hasR
import com.mardous.booming.ui.component.preferences.SwitchWithButtonPreference
import com.mardous.booming.ui.dialogs.library.BlacklistWhitelistDialog
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.library.ReloadType
import com.mardous.booming.util.BLACKLIST_ENABLED
import com.mardous.booming.util.IGNORE_MEDIA_STORE
import com.mardous.booming.util.LAST_ADDED_CUTOFF
import com.mardous.booming.util.PREFERRED_IMAGE_SIZE
import com.mardous.booming.util.TRASH_MUSIC_FILES
import com.mardous.booming.util.USE_FOLDER_ART
import com.mardous.booming.util.WHITELIST_ENABLED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians M. A. (mardous)
 */
class LibraryPreferencesFragment : PreferencesScreenFragment() {

    private val libraryViewModel by activityViewModel<LibraryViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_library)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!hasR()) {
            findPreference<Preference>(TRASH_MUSIC_FILES)?.isVisible = false
        }

        findPreference<Preference>(LAST_ADDED_CUTOFF)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                libraryViewModel.forceReload(ReloadType.Suggestions)
                true
            }

        findPreference<SwitchWithButtonPreference>(WHITELIST_ENABLED)?.apply {
            setButtonPressedListener(object : SwitchWithButtonPreference.OnButtonPressedListener {
                override fun onButtonPressed() {
                    showLibraryFolderSelector(InclExclDao.WHITELIST)
                }
            })
        }

        findPreference<SwitchWithButtonPreference>(BLACKLIST_ENABLED)?.apply {
            setButtonPressedListener(object : SwitchWithButtonPreference.OnButtonPressedListener {
                override fun onButtonPressed() {
                    showLibraryFolderSelector(InclExclDao.BLACKLIST)
                }
            })
        }

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

    private fun showLibraryFolderSelector(type: Int) {
        BlacklistWhitelistDialog.newInstance(type).show(childFragmentManager, "LIBRARY_PATHS_PREFERENCE")
    }

    private fun clearImageLoaderCache() = lifecycleScope.launch(Dispatchers.IO) {
        val imageLoader = SingletonImageLoader.get(requireContext())
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}