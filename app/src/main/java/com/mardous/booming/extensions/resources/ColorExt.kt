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

package com.mardous.booming.extensions.resources

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.text.style.ForegroundColorSpan
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.mardous.booming.R
import com.mardous.booming.extensions.isNightMode
import com.mardous.booming.extensions.resolveColor
import kotlin.math.abs

val Int.isColorLight: Boolean
    get() = (1 - (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255) < 0.4

val Int.darkenColor: Int
    get() = shiftColor(0.9f)

/**
 * Ensures that the color has enough contrast against a given background.
 */
@ColorInt
fun Int.ensureContrastAgainst(@ColorInt background: Int, minContrastRatio: Double = 2.0): Int {
    return if (ColorUtils.calculateContrast(this, background) < minContrastRatio) {
        if (ColorUtils.calculateLuminance(background) > 0.5)
            ColorUtils.blendARGB(this, 0xFF000000.toInt(), 0.3f)
        else
            ColorUtils.blendARGB(this, 0xFFFFFFFF.toInt(), 0.3f)
    } else {
        this
    }
}

/**
 * Desaturates the color if it's too dark compared to the reference background.
 */
@ColorInt
fun Int.desaturateIfTooDarkComparedTo(@ColorInt background: Int): Int {
    val luminanceDiff = ColorUtils.calculateLuminance(background) - ColorUtils.calculateLuminance(this)
    return if (luminanceDiff > 0.3) ColorUtils.blendARGB(this, background, 0.3f) else this
}

fun Int.adjustSaturationIfTooHigh(surfaceColor: Int, isNightMode: Boolean): Int {
    if (isNightMode) return this

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this, hsl)

    val backgroundLuminance = ColorUtils.calculateLuminance(surfaceColor)
    val colorLuminance = ColorUtils.calculateLuminance(this)

    val delta = abs(colorLuminance - backgroundLuminance)

    if (hsl[1] > 0.5f && delta < 0.3f) {
        hsl[1] = 0.4f + (hsl[1] - 0.5f) * 0.5f
    }

    return ColorUtils.HSLToColor(hsl)
}

fun Int.shiftColor(by: Float): Int {
    if (by == 1f) return this
    val alpha = Color.alpha(this)
    val hsv = FloatArray(3)
    Color.colorToHSV(this, hsv)
    hsv[2] *= by // value component

    return (alpha shl 24) + (0x00ffffff and Color.HSVToColor(hsv))
}

fun Int.withAlpha(alpha: Float): Int {
    val a = 255.coerceAtMost(0.coerceAtLeast((alpha * 255).toInt())) shl 24
    val rgb = 0x00ffffff and this
    return a + rgb
}

fun Int.toForegroundColorSpan() = ForegroundColorSpan(this)

fun Int.toColorStateList(): ColorStateList = ColorStateList.valueOf(this)

@ColorInt
fun Context.getColorCompat(@ColorRes res: Int) = ContextCompat.getColor(this, res)

@ColorInt
fun Fragment.surfaceColor() = requireContext().surfaceColor()

@ColorInt
fun Context.surfaceColor() = resolveColor(com.google.android.material.R.attr.colorSurface)

@ColorInt
fun Context.onSurfaceColor() = resolveColor(com.google.android.material.R.attr.colorOnSurface)

@ColorInt
fun Fragment.primaryColor() = requireContext().primaryColor()

@ColorInt
fun Context.primaryColor() = resolveColor(androidx.appcompat.R.attr.colorPrimary)

@ColorInt
fun Fragment.secondaryColor() = requireContext().secondaryColor()

@ColorInt
fun Context.secondaryColor() = resolveColor(com.google.android.material.R.attr.colorSecondary)

@ColorInt
fun Fragment.textColorPrimary() = requireContext().textColorPrimary()

@ColorInt
fun Context.textColorPrimary() = resolveColor(android.R.attr.textColorPrimary)

@ColorInt
fun Fragment.textColorSecondary() = requireContext().textColorSecondary()

@ColorInt
fun Context.textColorSecondary() = resolveColor(android.R.attr.textColorSecondary)

fun SeekBar.applyColor(@ColorInt color: Int) {
    color.toColorStateList().let {
        thumbTintList = it
        progressTintList = it
        progressBackgroundTintList = it
    }
}

fun Slider.applyColor(@ColorInt color: Int) {
    color.toColorStateList().run {
        thumbTintList = this
        trackActiveTintList = this
        trackInactiveTintList = ColorStateList.valueOf(color.withAlpha(0.1f))
    }
}

fun MaterialButton.applyColor(color: Int, isIconButton: Boolean = false) {
    if (isIconButton) {
        val colorTintList = color.toColorStateList()
        setTextColor(colorTintList)
        iconTint = colorTintList
    } else {
        val backgroundColorStateList = color.toColorStateList()
        val textColorColorStateList = getPrimaryTextColor(context, color.isColorLight).toColorStateList()
        backgroundTintList = backgroundColorStateList
        setTextColor(textColorColorStateList)
        iconTint = textColorColorStateList
    }
}

fun FloatingActionButton.applyColor(color: Int) {
    val textColor = getPrimaryTextColor(context, color.isColorLight)
    backgroundTintList = ColorStateList.valueOf(color)
    imageTintList = ColorStateList.valueOf(textColor)
}

fun TextView.applyColor(color: Int) {
    setTextColor(color)
    TextViewCompat.setCompoundDrawableTintList(this, color.toColorStateList())
}

@SuppressLint("RestrictedApi")
fun Toolbar.colorizeToolbar(toolbarIconsColor: Int) {
    val colorFilter = PorterDuffColorFilter(toolbarIconsColor, PorterDuff.Mode.SRC_IN)

    for (i in 0 until childCount) {
        val v = getChildAt(i)

        if (v is ImageButton) {
            v.drawable?.mutate()?.colorFilter = colorFilter
        }

        if (v is ActionMenuView) {
            for (j in 0 until v.childCount) {
                val innerView = v.getChildAt(j)
                if (innerView is ActionMenuItemView) {
                    innerView.compoundDrawables.forEach { drawable ->
                        innerView.post {
                            drawable?.mutate()?.colorFilter = colorFilter
                        }
                    }
                }
            }
        }
    }

    setTitleTextColor(context.textColorSecondary())
    setSubtitleTextColor(context.textColorSecondary())

    overflowIcon?.mutate()?.colorFilter =
        PorterDuffColorFilter(toolbarIconsColor, PorterDuff.Mode.SRC_IN)
}

fun getPrimaryTextColor(context: Context, isDark: Boolean = !context.isNightMode, isDisabled: Boolean = false): Int {
    val resolveColor = if (isDark) {
        if (isDisabled) R.color.primary_text_disabled_light else R.color.primary_text_light
    } else {
        if (isDisabled) R.color.primary_text_disabled_dark else R.color.primary_text_dark
    }
    return context.getColorCompat(resolveColor)
}