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
import androidx.preference.Preference
import com.mardous.booming.R
import com.mardous.booming.extensions.isTablet
import com.mardous.booming.util.*

/**
 * @author Christians M. A. (mardous)
 */
class NowPlayingPreferencesFragment : PreferencesScreenFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_now_playing)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findPreference<Preference>(ADD_EXTRA_CONTROLS)?.isVisible = !resources.isTablet
        updateNowPlayingScreen()
        updateCoverActions()
        Preferences.registerOnSharedPreferenceChangeListener(this)
    }

    private fun updateNowPlayingScreen() {
        findPreference<Preference>(NOW_PLAYING_SCREEN)?.summary =
            getString(Preferences.nowPlayingScreen.titleRes)
    }

    private fun updateCoverActions() {
        findPreference<Preference>(COVER_SINGLE_TAP_ACTION)?.summary =
            getString(Preferences.coverSingleTapAction.titleRes)

        findPreference<Preference>(COVER_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_LEFT_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverLeftDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_RIGHT_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverRightDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_LONG_PRESS_ACTION)?.summary =
            getString(Preferences.coverLongPressAction.titleRes)
    }

    override fun onDestroyView() {
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        when (key) {
            NOW_PLAYING_SCREEN -> updateNowPlayingScreen()
            COVER_DOUBLE_TAP_ACTION,
            COVER_LEFT_DOUBLE_TAP_ACTION,
            COVER_RIGHT_DOUBLE_TAP_ACTION,
            COVER_LONG_PRESS_ACTION -> updateCoverActions()
        }
    }
}