/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.mardous.booming.presentation.components.color

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.score.Score
import com.mardous.booming.presentation.theme.onSurfaceDark
import com.mardous.booming.presentation.theme.onSurfaceLight
import com.mardous.booming.presentation.theme.onSurfaceVariantDark
import com.mardous.booming.presentation.theme.onSurfaceVariantLight

fun Color.isDark(): Boolean = this.luminance() < 0.4

fun Color.darken(maxTone: Double = 30.0): Color {
    val hct = Hct.fromInt(this.toArgb())
    if (hct.tone > maxTone) {
        hct.tone = maxTone
    }
    return Color(hct.toInt())
}

fun Color.onThis(
    isPrimary: Boolean = true,
    isDisabled: Boolean = false
): Color {
    return if (isPrimary) {
        if (isDark()) {
            if (isDisabled) Color(0x61FFFFFF) else onSurfaceDark
        } else {
            if (isDisabled) Color(0x61000000) else onSurfaceLight
        }
    } else {
        if (isDark()) {
            if (isDisabled) Color(0x42FFFFFF) else onSurfaceVariantDark
        } else {
            if (isDisabled) Color(0x42000000) else onSurfaceVariantLight
        }
    }
}

fun Bitmap.extractGradientColors(fallbackColorArgb: Int): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(16)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 4, fallbackColorArgb, true)
    return if (orderedColors.isNotEmpty()) {
        orderedColors.map { Color(it).darken(20.0) }
    } else {
        emptyList()
    }
}