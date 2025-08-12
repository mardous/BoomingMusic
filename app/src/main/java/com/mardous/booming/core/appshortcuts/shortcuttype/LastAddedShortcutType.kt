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
import android.content.pm.ShortcutInfo
import com.mardous.booming.R
import com.mardous.booming.core.appshortcuts.AppShortcutIconGenerator.generateThemedIcon
import com.mardous.booming.core.appshortcuts.AppShortcutLauncherActivity

/**
 * @author Adrian Campos
 */
class LastAddedShortcutType(context: Context) : BaseShortcutType(context) {

    override val shortcutInfo: ShortcutInfo
        get() = ShortcutInfo.Builder(context, ID)
            .setShortLabel(context.getString(R.string.app_shortcut_last_added_short))
            .setLongLabel(context.getString(R.string.last_added_label))
            .setIcon(generateThemedIcon(context, R.drawable.ic_app_shortcut_last_added))
            .setIntent(getPlaySongsIntent(AppShortcutLauncherActivity.SHORTCUT_TYPE_LAST_ADDED))
            .build()

    companion object {
        const val ID: String = ID_PREFIX + "last_added"
    }
}
