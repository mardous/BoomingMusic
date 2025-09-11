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
package com.mardous.booming.core.appshortcuts.shortcuttype

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.core.os.bundleOf
import com.mardous.booming.core.appshortcuts.AppShortcutLauncherActivity

/**
 * @author Adrian Campos
 */
abstract class BaseShortcutType(val context: Context) {

    abstract val shortcutInfo: ShortcutInfo?

    /**
     * Creates an Intent that will launch MainActivity and immediately play {@param songs} in either shuffle or normal mode
     *
     * @param shortcutType Describes the type of shortcut to create (ShuffleAll, TopTracks, custom playlist, etc.)
     */
    fun getPlaySongsIntent(shortcutType: Int): Intent {
        val intent = Intent(context, AppShortcutLauncherActivity::class.java)
        intent.setAction(Intent.ACTION_VIEW)
        val b = bundleOf(AppShortcutLauncherActivity.KEY_SHORTCUT_TYPE to shortcutType)
        intent.putExtras(b)
        return intent
    }

    companion object {
        internal const val ID_PREFIX: String = "com.mardous.booming.core.appshortcuts.id."
    }
}
