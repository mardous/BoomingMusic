/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withTranslation
import androidx.core.view.updatePadding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapes
import com.mardous.booming.R
import com.mardous.booming.extensions.dp
import kotlin.math.min

/**
 * This is based on **NewPlayerToggle.java**, created by
 * Grimmthejow for [XMusic](https://github.com/grimmthejow/XMusic).
 */
class MorphicIconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val morphPath = Path()
    private val morphMatrix = Matrix()
    private val bounds = Rect()

    private var imageView: ImageView? = null
    private var nextMorph: Morph? = null
    private var morphAnimator: ValueAnimator? = null
    private var rotateAnimator: ValueAnimator? = null

    private var currentShapeIndex = SHAPE_SQUARE
    private var targetShapeIndex = SHAPE_CIRCLE

    private var defaultBackgroundColor = Color.GRAY
    private var iconTintColor = Color.WHITE

    private var progress = 1f
    private var shapeRotation = 0f
        set(value) {
            field = value
            invalidate()
        }

    var morphDuration: Long = 450

    var rotationDuration: Long = 15000
        set(value) {
            field = value
            if (isRotating) {
                isRotating = false
                isRotating = true
            }
        }

    var isRotating = false
        set(value) {
            if (value && !field) {
                field = true
                rotateAnimator?.cancel()
                rotateAnimator = ValueAnimator.ofFloat(shapeRotation, shapeRotation + 360f).apply {
                    setDuration(rotationDuration)
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    addUpdateListener { animator: ValueAnimator ->
                        shapeRotation = animator.animatedValue as Float
                    }
                }
                rotateAnimator?.start()
            } else if (!value && field) {
                field = false
                rotateAnimator?.end()
                rotateAnimator = null
            }
        }

    init {
        setWillNotDraw(false)
        isClickable = true
        foregroundGravity = Gravity.CENTER

        val imagePadding = 20.dp(resources)
        imageView = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            updatePadding(imagePadding, imagePadding, imagePadding, imagePadding)
        }
        addView(imageView)

        defaultBackgroundColor = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary)
        iconTintColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary)

        context.withStyledAttributes(attrs, R.styleable.MorphicIconButton) {
            iconTintColor = getColor(R.styleable.MorphicIconButton_morphicIconTint, iconTintColor)
            morphDuration = getInt(R.styleable.MorphicIconButton_morphicDuration, morphDuration.toInt()).toLong()
            rotationDuration = getInt(R.styleable.MorphicIconButton_morphicRotationDuration, rotationDuration.toInt()).toLong()
            targetShapeIndex = getInt(R.styleable.MorphicIconButton_morphicShape, targetShapeIndex)
            currentShapeIndex = targetShapeIndex

            val iconResId = getResourceId(R.styleable.MorphicIconButton_morphicIcon, -1)
            if (iconResId != -1) {
                setIconResource(iconResId)
            }
        }
    }

    override fun setBackground(background: Drawable?) {
        throw UnsupportedOperationException("Please, use setBackgroundTintList() instead.")
    }

    override fun setBackgroundColor(color: Int) {
        throw UnsupportedOperationException("Please, use setBackgroundTintList() instead.")
    }

    override fun setBackgroundTintList(tint: ColorStateList?) {
        super.setBackgroundTintList(tint)
        invalidate()
    }

    override fun getBackgroundTintList(): ColorStateList {
        val backgroundTintList = super.getBackgroundTintList()
            ?: return ColorStateList.valueOf(defaultBackgroundColor)
        return backgroundTintList
    }

    fun setIcon(icon: Drawable?, animateIfAvd: Boolean = true) {
        icon?.mutate()?.let { iconDrawable ->
            iconDrawable.setTint(iconTintColor)
            imageView?.setImageDrawable(iconDrawable)
            if (animateIfAvd && iconDrawable is AnimatedVectorDrawable) {
                iconDrawable.start()
            }
        }
    }

    fun setIconResource(@DrawableRes resId: Int, animateIfAvd: Boolean = true) {
        setIcon(ContextCompat.getDrawable(context, resId), animateIfAvd)
    }

    fun setIconTintColor(@ColorInt color: Int) {
        this.iconTintColor = color
        imageView?.drawable?.setTint(color)
    }

    fun morphToShape(shapeIndex: Int) {
        if (shapeIndex < 0 || shapeIndex >= SHAPES.size || shapeIndex == targetShapeIndex) return

        morphAnimator?.cancel()

        currentShapeIndex = targetShapeIndex
        targetShapeIndex = shapeIndex

        nextMorph = Morph(SHAPES[currentShapeIndex], SHAPES[targetShapeIndex])
        morphAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            setDuration(morphDuration)
            addUpdateListener { animator: ValueAnimator ->
                progress = animator.animatedValue as Float
                invalidate()
            }
            doOnCancel { nextMorph = null }
            doOnEnd { nextMorph = null }
        }
        morphAnimator?.start()
    }

    override fun onDraw(canvas: Canvas) {
        getDrawingRect(bounds)

        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        val size = min(bounds.width(), bounds.height()).toFloat()

        canvas.withTranslation(centerX, centerY) {
            rotate(shapeRotation)

            morphPath.rewind()
            if (nextMorph != null) {
                nextMorph?.toPath(progress, morphPath)
            } else {
                SHAPES[targetShapeIndex].toPath(morphPath)
            }

            morphMatrix.setScale(size / 2f, size / 2f)
            morphPath.transform(morphMatrix)

            paint.color = backgroundTintList.defaultColor
            paint.style = Paint.Style.FILL
            drawPath(morphPath, paint)
        }
        super.onDraw(canvas)
    }

    companion object {
        const val SHAPE_SQUARE: Int = 0
        const val SHAPE_SOFT_BURST: Int = 1
        const val SHAPE_COOKIE_9: Int = 2
        const val SHAPE_PENTAGON: Int = 3
        const val SHAPE_PILL: Int = 4
        const val SHAPE_SUNNY: Int = 5
        const val SHAPE_COOKIE_4: Int = 6
        const val SHAPE_CIRCLE: Int = 7
        const val SHAPE_COOKIE_12: Int = 8

        @SuppressLint("RestrictedApi")
        private val SHAPES = arrayOf(
            MaterialShapes.normalize(MaterialShapes.SQUARE, true, RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.SOFT_BURST, true, RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.COOKIE_9, true, RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.PENTAGON, true, RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.PILL, true, RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.SUNNY, true, RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.COOKIE_4, true, RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.CIRCLE, true, RectF(-1f, -1f, 1f, 1f)),
            MaterialShapes.normalize(MaterialShapes.COOKIE_12, true, RectF(-1f, -1f, 1f, 1f))
        )
    }
}