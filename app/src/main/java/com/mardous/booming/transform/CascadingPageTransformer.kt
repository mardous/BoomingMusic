/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package com.mardous.booming.transform

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class CascadingPageTransformer(
    private val scaleOffsetPx: Int = 40
) : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        when {
            position < -1f -> {
                page.alpha = 0f
            }

            position <= 0f -> {
                page.alpha = 1f - abs(position)
                page.rotation = 45f * position
                page.translationX = page.width / 3f * position
                page.scaleX = 1f
                page.scaleY = 1f
                page.translationY = 0f
            }

            position <= 1f -> {
                val scale = (page.width - scaleOffsetPx * position) / page.width.toFloat()
                val safeScale = scale.coerceIn(0.7f, 1f)

                page.alpha = 1f
                page.rotation = 0f
                page.scaleX = safeScale
                page.scaleY = safeScale

                page.translationX = -page.width * position
                page.translationY = scaleOffsetPx * 0.8f * position
            }

            else -> {
                page.alpha = 0f
            }
        }
    }
}
