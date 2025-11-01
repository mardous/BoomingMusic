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
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.preference.Preference
import com.google.android.material.color.DynamicColors
import com.mardous.booming.R
import com.mardous.booming.appInstance
import com.mardous.booming.extensions.hasR
import com.mardous.booming.extensions.hasS
import com.mardous.booming.ui.component.preferences.ThemePreference
import com.mardous.booming.util.*

/**
 * @author Christians M. A. (mardous)
 */
class AppearancePreferencesFragment : PreferencesScreenFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_appearance)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (hasR()) {
            findPreference<Preference>("lockscreen_category")?.isVisible = false
        }

        findPreference<ThemePreference>(GENERAL_THEME)?.apply {
            customCallback = object : ThemePreference.Callback {
                override fun onThemeSelected(themeName: String) {
                    Preferences.generalTheme = themeName
                    setDefaultNightMode(Preferences.getDayNightMode(themeName))
                    restartActivity()
                }
            }
        }

        findPreference<Preference>(BLACK_THEME)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val themeName = Preferences.getGeneralTheme((newValue as Boolean))
                setDefaultNightMode(Preferences.getDayNightMode(themeName))
                requireActivity().recreate()
                true
            }
        }

        findPreference<Preference>(MATERIAL_YOU)?.apply {
            isVisible = hasS()
            setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    DynamicColors.applyToActivitiesIfAvailable(appInstance())
                }
                requireActivity().recreate()
                true
            }
        }

        findPreference<Preference>(USE_CUSTOM_FONT)?.setOnPreferenceChangeListener { _, _ ->
            requireActivity().recreate()
            true
        }
    }
}