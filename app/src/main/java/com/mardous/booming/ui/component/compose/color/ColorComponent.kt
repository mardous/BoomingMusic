package com.mardous.booming.ui.component.compose.color

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.score.Score
import com.mardous.booming.ui.theme.onSurfaceDark
import com.mardous.booming.ui.theme.onSurfaceLight
import com.mardous.booming.ui.theme.onSurfaceVariantDark
import com.mardous.booming.ui.theme.onSurfaceVariantLight

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

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(16)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 4, 0xff4285f4.toInt(), true)

    return if (orderedColors.isNotEmpty())
        orderedColors.map { Color(it).darken(20.0) }
    else
        listOf(Color(0xFF262626), Color(0xFF1A1A1A), Color(0xFF0D0D0D))
}