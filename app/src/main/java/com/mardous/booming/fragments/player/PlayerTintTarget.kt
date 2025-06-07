/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.fragments.player

import android.graphics.Color
import android.view.View

class PlayerTintTarget(
    val target: View,
    val newColor: Int,
    val oldColor: Int = Color.TRANSPARENT,
    val isSurface: Boolean = false,
    val isIcon: Boolean = false
)

fun View.surfaceTintTarget(newColor: Int): PlayerTintTarget {
    return PlayerTintTarget(this, newColor, isSurface = true)
}

fun View.iconButtonTintTarget(oldColor: Int, newColor: Int): PlayerTintTarget {
    return PlayerTintTarget(this, newColor, oldColor, isIcon = true)
}

fun View.tintTarget(oldColor: Int, newColor: Int): PlayerTintTarget {
    return PlayerTintTarget(this, newColor, oldColor)
}