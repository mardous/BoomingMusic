package com.mardous.booming.ui.component.compose.color

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.palette.graphics.Palette
import com.kyant.m3color.score.Score
import com.mardous.booming.ui.theme.onSurfaceDark
import com.mardous.booming.ui.theme.onSurfaceLight
import com.mardous.booming.ui.theme.onSurfaceVariantDark
import com.mardous.booming.ui.theme.onSurfaceVariantLight

fun Color.isDark(): Boolean = this.luminance() < 0.4

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

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(16)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}