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

package com.mardous.booming.extensions

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LAYOUT_DIRECTION_RTL
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.ImageView
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mardous.booming.R
import com.mardous.booming.appContext

fun WindowInsetsCompat?.getBottomGesturesInsets(): Int {
    if (this == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0

    val tappableElements = getInsets(Type.tappableElement())
    if (tappableElements.bottom == 0) {
        val insets = Insets.max(
            getInsets(Type.systemGestures()),
            getInsets(Type.systemBars())
        )
        return insets.bottom
    }
    return 0
}

fun WindowInsetsCompat?.getBottomInsets(): Int {
    return this?.getInsets(Type.systemBars())?.bottom
        ?: appContext().getNavigationBarHeight()
}

typealias InsetsConsumer = (View, WindowInsetsCompat) -> Unit

private fun View.isMarginRequired() =
    this is FloatingActionButton || this is Button || this is ImageView

fun View.applyScrollableContentInsets(
    scrollView: NestedScrollingChild3,
    ime: Boolean = false,
    addedPadding: Int = 0,
    consumer: InsetsConsumer? = null
) {
    applyHorizontalWindowInsets()
    if (scrollView is ViewGroup) {
        scrollView.applyBottomWindowInsets(
            ime = ime,
            addedSpace = Space.bottom(addedPadding),
            consumer = consumer
        )
    }
}

fun View.applyHorizontalWindowInsets(
    left: Boolean = true,
    right: Boolean = true,
    ime: Boolean = false,
    padding: Boolean = !isMarginRequired(),
    addedSpace: Space = Space(),
    consumer: InsetsConsumer? = null
) = applyWindowInsets(
    left = left,
    right = right,
    ime = ime,
    padding = padding,
    addedSpace = addedSpace,
    consumer = consumer
)

fun View.applyBottomWindowInsets(
    ime: Boolean = false,
    padding: Boolean = !isMarginRequired(),
    addedSpace: Space = Space(),
    consumer: InsetsConsumer? = null
) = applyWindowInsets(
    bottom = true,
    ime = ime,
    padding = padding,
    addedSpace = addedSpace,
    consumer = consumer
)

fun View.applyWindowInsets(
    top: Boolean = false,
    left: Boolean = false,
    right: Boolean = false,
    bottom: Boolean = false,
    ime: Boolean = false,
    padding: Boolean = !isMarginRequired(),
    addedSpace: Space = Space(),
    consumer: InsetsConsumer? = null
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v: View, insets: WindowInsetsCompat ->
        if (getTag(R.id.id_inset_consumed) == true)
            return@setOnApplyWindowInsetsListener insets

        val mask = Type.systemBars() or Type.displayCutout() or if (ime) Type.ime() else 0
        val i = insets.getInsets(mask)
        val start = if (layoutDirection == LAYOUT_DIRECTION_RTL) i.right else i.left
        val end = if (layoutDirection == LAYOUT_DIRECTION_RTL) i.left else i.right
        val currentValues = v.currentSpace(padding)
        val userAddedSpace = addedSpace.resolve(v)
        if (!padding) {
            if (v.layoutParams is MarginLayoutParams) {
                v.updateLayoutParams<MarginLayoutParams> {
                    marginStart = (if (left) (start + currentValues.left) else currentValues.left) + userAddedSpace.left
                    topMargin = (if (top) (i.top + currentValues.top) else currentValues.top) + userAddedSpace.top
                    marginEnd = (if (right) (end + currentValues.right) else currentValues.right) + userAddedSpace.right
                    bottomMargin = (if (bottom) (i.bottom + currentValues.bottom) else currentValues.bottom) + userAddedSpace.bottom
                }
            }
        } else {
            v.setPaddingRelative(
                (if (left) (start + currentValues.left) else currentValues.left) + userAddedSpace.left,
                (if (top) (i.top + currentValues.top) else currentValues.top) + userAddedSpace.top,
                (if (right) (end + currentValues.right) else currentValues.right) + userAddedSpace.right,
                (if (bottom) (i.bottom + currentValues.bottom) else currentValues.bottom) + userAddedSpace.bottom
            )
        }
        setTag(R.id.id_inset_consumed, true)
        consumer?.invoke(v, insets)
        insets
    }
}

private fun View.currentSpace(padding: Boolean): Space {
    val currentValues = when {
        padding -> Space.viewPadding()
        else -> Space.viewMargin()
    }
    return currentValues.resolve(this)
}

class Space internal constructor(
    val top: Int = 0,
    val bottom: Int = 0,
    val left: Int = 0,
    val right: Int = 0
) {
    fun resolve(view: View): Space {
        val definedTop = when (top) {
            USE_VIEW_MARGIN -> view.marginTop
            USE_VIEW_PADDING -> view.paddingTop
            else -> top
        }
        val definedBottom = when (bottom) {
            USE_VIEW_MARGIN -> view.marginBottom
            USE_VIEW_PADDING -> view.paddingBottom
            else -> bottom
        }
        val definedLeft = when (left) {
            USE_VIEW_MARGIN -> view.marginStart
            USE_VIEW_PADDING -> view.paddingStart
            else -> left
        }
        val definedRight = when (right) {
            USE_VIEW_MARGIN -> view.marginEnd
            USE_VIEW_PADDING -> view.paddingEnd
            else -> right
        }
        return Space(definedTop, definedBottom, definedLeft, definedRight)
    }

    override fun toString(): String {
        return "Space{top=$top, left=$left, right=$right, bottom=$bottom}"
    }

    companion object {
        private const val USE_VIEW_MARGIN = Int.MIN_VALUE
        private const val USE_VIEW_PADDING = USE_VIEW_MARGIN + 1

        fun viewMargin(
            top: Int = USE_VIEW_MARGIN,
            bottom: Int = USE_VIEW_MARGIN,
            left: Int = USE_VIEW_MARGIN,
            right: Int = USE_VIEW_MARGIN
        ) = Space(top, bottom, left, right)

        fun viewPadding(
            top: Int = USE_VIEW_PADDING,
            bottom: Int = USE_VIEW_PADDING,
            left: Int = USE_VIEW_PADDING,
            right: Int = USE_VIEW_PADDING
        ) = Space(top, bottom, left, right)

        fun horizontal(margin: Int) = Space(left = margin, right = margin)
        fun vertical(margin: Int) = Space(top = margin, bottom = margin)
        fun top(margin: Int) = Space(top = margin)
        fun left(margin: Int) = Space(left = margin)
        fun right(margin: Int) = Space(right = margin)
        fun bottom(margin: Int) = Space(bottom = margin)
    }
}