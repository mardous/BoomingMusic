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
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.IconCompat
import com.mardous.booming.R
import com.mardous.booming.extensions.getTintedDrawable
import com.mardous.booming.extensions.resources.getColorCompat
import com.mardous.booming.extensions.resources.toBitmap

/**
 * @author Adrian Campos
 */
object AppShortcutIconGenerator {
    fun generateThemedIcon(context: Context, @DrawableRes iconId: Int): Icon {
        val foregroundColor = context.getColorCompat(R.color.app_shortcut_default_foreground)
        val backgroundColor = context.getColorCompat(R.color.app_shortcut_default_background)
        // Get and tint foreground and background drawables
        val vectorDrawable = context.getTintedDrawable(iconId, foregroundColor)
        val backgroundDrawable = context.getTintedDrawable(R.drawable.ic_app_shortcut_background, backgroundColor)
        return IconCompat.createWithAdaptiveBitmap(
            AdaptiveIconDrawable(backgroundDrawable, vectorDrawable).toBitmap()
        ).toIcon(context)
    }
}