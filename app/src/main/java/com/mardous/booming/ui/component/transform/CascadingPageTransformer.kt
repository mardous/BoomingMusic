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

package com.mardous.booming.ui.component.transform

import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

class CascadingPageTransformer : ViewPager.PageTransformer {

    private var mScaleOffset = 40

    override fun transformPage(page: View, position: Float) {
        page.apply {
            when {
                position < -1 -> { // [-Infinity,-1)
                    alpha = 0f
                }
                position <= 0 -> {
                    alpha = 1 - abs(position)
                    rotation = 45 * position
                    translationX = width / 3 * position
                }
                else -> {
                    alpha = 1f
                    rotation = 0f

                    val safeWidth = if (width == 0) 1 else width
                    val scale = (safeWidth - mScaleOffset * position) / safeWidth.toFloat()
                    val safeScale = if (scale.isFinite() && scale > 0f) scale else 1f

                    scaleX = safeScale
                    scaleY = safeScale

                    translationX = -width * position
                    translationY = mScaleOffset * 0.8f * position
                }
            }
        }
    }
}