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

import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.mardous.booming.R

/**
 * @author Christians M. A. (mardous)
 */
enum class SettingsScreen(@LayoutRes val layoutRes: Int, @IdRes val navAction: Int) {
    Appearance(R.xml.preferences_screen_appearance, R.id.action_to_appearancePreferences),
    NowPlaying(R.xml.preferences_screen_now_playing, R.id.action_to_nowPlayingPreferences),
    Playback(R.xml.preferences_screen_playback, R.id.action_to_playbackPreferences),
    Metadata(R.xml.preferences_screen_metadata, R.id.action_to_metadataPreferences),
    Library(R.xml.preferences_screen_library, R.id.action_to_libraryPreferences),
    Notification(R.xml.preferences_screen_notification, R.id.action_to_notificationPreferences),
    Update(R.xml.preferences_screen_update, R.id.action_to_updatePreferences),
    Advanced(R.xml.preferences_screen_advanced, R.id.action_to_advancedPreferences);
}