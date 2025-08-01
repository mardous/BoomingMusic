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
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class HingeTransformation : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        page.apply {
            // Move the page horizontally based on swipe position
            translationX = -position * width

            pivotX = 0f
            pivotY = 0f

            when {
                position < -1 -> { // [-Infinity, -1)
                    alpha = 0f
                    isVisible = false // Set visibility to false to avoid intercepting touch events
                }

                position <= 0 -> { // [-1, 0]
                    rotation = 90 * abs(position)
                    // Fade out while rotating
                    alpha = 1 - abs(position)
                    isVisible = true
                }

                position <= 1 -> { // (0, 1]
                    rotation = 0f
                    alpha = 1f
                    isVisible = true
                }

                else -> { // (1, +Infinity]
                    alpha = 0f
                    isVisible = false
                }
            }
        }
    }
}