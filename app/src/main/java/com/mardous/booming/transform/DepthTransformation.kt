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

class DepthTransformation : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        when {
            position < -1f -> {
                page.alpha = 0f
            }

            position <= 0f -> {
                page.alpha = 1f
                page.translationX = 0f
                page.scaleX = 1f
                page.scaleY = 1f
            }

            position <= 1f -> {
                page.alpha = 1f - position
                page.translationX = -page.width * position

                val scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - position)
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor
            }

            else -> {
                page.alpha = 0f
            }
        }
    }

    companion object {
        private const val MIN_SCALE = 0.5f
    }
}