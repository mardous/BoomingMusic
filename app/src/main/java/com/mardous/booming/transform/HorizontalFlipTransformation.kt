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

class HorizontalFlipTransformation : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        page.apply {
            // Move the page horizontally based on position
            translationX = -position * width
            cameraDistance = 20000f
            isVisible = position in -0.5..0.5

            when {
                position < -1 -> { // [-Infinity, -1)
                    // Page is far off to the left
                    alpha = 0f
                }

                position <= 0 -> { // [-1, 0]
                    alpha = 1f
                    // Flip along X-axis from the front
                    rotationX = 180 * (1 - abs(position) + 1)
                }

                position <= 1 -> { // (0, 1]
                    alpha = 1f
                    // Flip along X-axis from the back
                    rotationX = -180 * (1 - abs(position) + 1)
                }

                else -> { // (1, +Infinity]
                    // Page is far off to the right
                    alpha = 0f
                }
            }
        }
    }
}