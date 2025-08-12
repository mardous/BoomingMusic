package com.mardous.booming.ui.component.compose.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.mardous.booming.ui.theme.onSurfaceDark
import com.mardous.booming.ui.theme.onSurfaceLight
import com.mardous.booming.ui.theme.onSurfaceVariantDark
import com.mardous.booming.ui.theme.onSurfaceVariantLight

fun Color.isDark(): Boolean = this.luminance() < 0.4

fun Color.primaryTextColor(isDisabled: Boolean = false): Color {
    return if (isDark()) {
        if (isDisabled) Color(0x61FFFFFF) else onSurfaceDark
    } else {
        if (isDisabled) Color(0x61000000) else onSurfaceLight
    }
}

fun Color.secondaryTextColor(isDisabled: Boolean = false): Color {
    return if (isDark()) {
        if (isDisabled) Color(0x42FFFFFF) else onSurfaceVariantDark
    } else {
        if (isDisabled) Color(0x42000000) else onSurfaceVariantLight
    }
}