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
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.REPLAYGAIN_PREAMP
import com.mardous.booming.util.REPLAYGAIN_SOURCE_MODE
import com.mardous.booming.util.ReplayGainSourceMode

/**
 * @author Christians M. A. (mardous)
 */
class PlaybackPreferencesFragment : PreferencesScreenFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_playback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Preferences.registerOnSharedPreferenceChangeListener(this)
        updatePreAmpState()
    }

    private fun updatePreAmpState() {
        findPreference<Preference>(REPLAYGAIN_PREAMP)?.isEnabled =
            Preferences.replayGainSourceMode != ReplayGainSourceMode.MODE_NONE
    }

    override fun onDestroyView() {
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        when (key) {
            REPLAYGAIN_SOURCE_MODE -> updatePreAmpState()
        }
    }
}