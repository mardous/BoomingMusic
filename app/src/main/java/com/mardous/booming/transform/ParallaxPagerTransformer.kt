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

class ParallaxPagerTransformer(private val id: Int) : ViewPager2.PageTransformer {

    private var speed = 0.2f

    override fun transformPage(page: View, position: Float) {
        val parallaxView = getCachedView(page) ?: return

        if (abs(position) < 1f) {
            val width = parallaxView.width.toFloat()
            parallaxView.translationX = -(position * width * speed)

            page.scaleX = 1f
            page.scaleY = 1f
        }
    }

    private fun getCachedView(page: View): View? {
        val cached = page.getTag(id)
        return if (cached is View) {
            cached
        } else {
            val found = page.findViewById<View>(id)
            if (found != null) {
                page.setTag(id, found)
            }
            found
        }
    }

    fun setSpeed(speed: Float) {
        this.speed = speed.coerceIn(0f, 1f)
    }
}
