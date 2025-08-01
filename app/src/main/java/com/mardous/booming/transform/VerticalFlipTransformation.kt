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
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class VerticalFlipTransformation : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width

        page.translationX = -position * pageWidth
        page.cameraDistance = (pageWidth * 10).toFloat()
        page.visibility = if (position in -0.5f..0.5f) View.VISIBLE else View.INVISIBLE

        when {
            position < -1f -> {
                page.alpha = 0f
            }
            position <= 0f -> {
                page.alpha = 1f
                page.rotationY = 180f * (1f + position)
            }
            position <= 1f -> {
                page.alpha = 1f
                page.rotationY = -180f * (1f - position)
            }
            else -> {
                page.alpha = 0f
            }
        }
    }
}
