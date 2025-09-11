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
package com.mardous.booming.core.appshortcuts

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import androidx.core.content.getSystemService
import com.mardous.booming.core.appshortcuts.shortcuttype.LastAddedShortcutType
import com.mardous.booming.core.appshortcuts.shortcuttype.ShuffleAllShortcutType
import com.mardous.booming.core.appshortcuts.shortcuttype.TopTracksShortcutType

/**
 * @author Adrian Campos
 */
class DynamicShortcutManager(private val context: Context) {

    private val shortcutManager: ShortcutManager? = context.getSystemService()

    private val defaultShortcuts: List<ShortcutInfo>
        get() = listOf(
            ShuffleAllShortcutType(context).shortcutInfo,
            TopTracksShortcutType(context).shortcutInfo,
            LastAddedShortcutType(context).shortcutInfo
        )

    fun initDynamicShortcuts() {
        if (shortcutManager == null)
            return

        if (shortcutManager.dynamicShortcuts.isEmpty()) {
            shortcutManager.dynamicShortcuts = defaultShortcuts
        }
    }

    fun updateDynamicShortcuts() {
        shortcutManager?.updateShortcuts(defaultShortcuts)
    }

    companion object {
        fun reportShortcutUsed(context: Context, shortcutId: String) {
            context.getSystemService(ShortcutManager::class.java).reportShortcutUsed(shortcutId)
        }
    }
}
