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

package com.mardous.booming.ui.component.views

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.addListener
import com.google.android.material.slider.Slider
import com.mardous.booming.extensions.resources.toColorStateList
import com.mardous.booming.extensions.resources.withAlpha

/**
 * @author Christians M. A. (mardous)
 */
class AnimSlider @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = com.google.android.material.R.attr.sliderStyle) :
    Slider(context, attrs, defStyleAttr) {

    private var progressAnimator: Animator? = null
    private var isAnimatingToValue = Float.NaN

    var isDragging: Boolean = false
        private set

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addOnSliderTouchListener(onSliderTouchListener)
    }

    override fun onDetachedFromWindow() {
        removeOnSliderTouchListener(onSliderTouchListener)
        super.onDetachedFromWindow()
    }

    override fun setValueFrom(valueFrom: Float) {
        cancelAnimation()
        super.setValueFrom(valueFrom)
    }

    override fun setValueTo(valueTo: Float) {
        cancelAnimation()
        super.setValueTo(valueTo)
    }

    fun setSliderColor(color: Int) {
        val colorStateList = color.toColorStateList()
        val colorWithAlpha = color.withAlpha(0.25f).toColorStateList()
        thumbTintList = colorStateList
        haloTintList = colorWithAlpha
        trackActiveTintList = colorStateList
        trackInactiveTintList = colorWithAlpha
    }

    fun setValueAnimated(progress: Float) {
        if (isAnimatingToValue.isNaN() || isAnimatingToValue != progress) {
            cancelAnimation()

            val fromProcess = value.coerceIn(valueFrom, valueTo)
            val toProgress = progress.coerceIn(valueFrom, valueTo)
            if (fromProcess == toProgress) {
                value = toProgress
            } else {
                progressAnimator = ObjectAnimator.ofFloat(this, "value", fromProcess, toProgress).apply {
                    duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                    interpolator = DecelerateInterpolator()

                    addListener(
                        onEnd = {
                            isAnimatingToValue = Float.NaN
                        },
                        onCancel = {
                            isAnimatingToValue = Float.NaN
                        }
                    )
                }.also { animator ->
                    animator.start()
                }
            }
        }
    }

    private fun cancelAnimation() {
        progressAnimator?.cancel()
        progressAnimator = null
    }

    private val onSliderTouchListener = object : OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {
            isDragging = true
            cancelAnimation()
        }

        override fun onStopTrackingTouch(slider: Slider) {
            isDragging = false
        }
    }
}