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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.scale
import java.io.OutputStream
import kotlin.math.roundToInt

fun Context.getDrawableCompat(@DrawableRes resId: Int) = ContextCompat.getDrawable(this, resId)

@CheckResult
fun Drawable?.getTinted(@ColorInt color: Int): Drawable? {
    return this?.let {
        DrawableCompat.wrap(it.mutate())
    }?.apply {
        DrawableCompat.setTintMode(this, PorterDuff.Mode.SRC_IN)
        DrawableCompat.setTint(this, color)
    }
}

fun Drawable.toBitmap(sizeMultiplier: Float = 1f): Bitmap {
    return createBitmap(
        (intrinsicWidth * sizeMultiplier).toInt(),
        (intrinsicHeight * sizeMultiplier).toInt()
    ).apply {
        Canvas(this).let { c ->
            setBounds(0, 0, c.width, c.height)
            draw(c)
        }
    }
}

fun Bitmap.getResized(maxForSmallerSize: Int): Bitmap {
    val width = width
    val height = height
    val dstWidth: Int
    val dstHeight: Int
    if (width < height) {
        if (maxForSmallerSize >= width) {
            return this
        }
        val ratio = height.toFloat() / width
        dstWidth = maxForSmallerSize
        dstHeight = (maxForSmallerSize * ratio).roundToInt()
    } else {
        if (maxForSmallerSize >= height) {
            return this
        }
        val ratio = width.toFloat() / height
        dstWidth = (maxForSmallerSize * ratio).roundToInt()
        dstHeight = maxForSmallerSize
    }
    return this.scale(dstWidth, dstHeight, false)
}

fun Bitmap.toJPG(quality: Int = 90, stream: OutputStream?): Boolean {
    return stream != null && compress(Bitmap.CompressFormat.JPEG, quality, stream)
}