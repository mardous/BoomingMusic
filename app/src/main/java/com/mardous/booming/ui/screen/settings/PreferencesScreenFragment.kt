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

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.mardous.booming.BuildConfig
import com.mardous.booming.R
import com.mardous.booming.extensions.hasS
import com.mardous.booming.extensions.navigation.findActivityNavController
import com.mardous.booming.extensions.utilities.toEnum
import com.mardous.booming.ui.component.preferences.dialog.*
import com.mardous.booming.ui.screen.library.LibraryViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

open class PreferencesScreenFragment : PreferenceFragmentCompat() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val dialogFragment: DialogFragment? = when (preference) {
            is NowPlayingExtraInfoPreference -> NowPlayingExtraInfoPreferenceDialog()
            is CategoriesPreference -> CategoriesPreferenceDialog()
            is NowPlayingScreenPreference -> NowPlayingScreenPreferenceDialog()
            is ActionOnCoverPreference -> ActionOnCoverPreferenceDialog.newInstance(preference.key, preference.title!!)
            is PreAmpPreference -> PreAmpPreferenceDialog()
            else -> null
        }

        if (dialogFragment != null) {
            dialogFragment.show(childFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDivider(Color.TRANSPARENT.toDrawable())
        if (hasS()) {
            listView.overScrollMode = View.OVER_SCROLL_NEVER
        }

        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            listView.updatePadding(bottom = it.getWithSpace())
        }

        findPreference<Preference>("about")?.summary =
            getString(R.string.about_summary, BuildConfig.VERSION_NAME)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val settingsScreen = preference.key.toEnum<SettingsScreen>()
        if (settingsScreen != null) {
            findNavController().navigate(settingsScreen.navAction, bundleOf(EXTRA_SCREEN to settingsScreen))
        } else if (preference.key == "about") {
            findActivityNavController(R.id.fragment_container).navigate(R.id.nav_about)
        }
        return true
    }

    protected fun restartActivity() {
        activity?.recreate()
    }

    companion object {
        private const val EXTRA_SCREEN = "extra_screen"
    }
}