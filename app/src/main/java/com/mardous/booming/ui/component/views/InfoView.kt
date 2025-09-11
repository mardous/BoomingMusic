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

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import com.mardous.booming.R

/**
 * @author Christians M. A. (mardous)
 */
class InfoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private var titleView: TextView? = null
    private var textView: TextView? = null

    init {
        val ta = getContext().obtainStyledAttributes(attrs, R.styleable.InfoView)
        val style = ta.getInt(R.styleable.InfoView_infoStyle, 0)
        val view = if (style == STYLE_HOR) {
            inflate(context, R.layout.info_item_horizontal, null)
        } else {
            inflate(context, R.layout.info_item_vertical, null)
        }
        titleView = view.findViewById(R.id.title)
        titleView?.text = ta.getString(R.styleable.InfoView_infoTitle)
        if (ta.hasValue(R.styleable.InfoView_infoTitleColor)) {
            titleView?.setTextColor(ta.getColorStateList(R.styleable.InfoView_infoTitleColor))
        }
        textView = view.findViewById(R.id.text)
        textView?.text = ta.getString(R.styleable.InfoView_infoText)
        if (ta.hasValue(R.styleable.InfoView_infoTextColor)) {
            textView?.setTextColor(ta.getColorStateList(R.styleable.InfoView_infoTextColor))
        }
        ta.recycle()
        addView(view)
    }

    fun setTitle(title: CharSequence) {
        titleView?.text = title
    }

    fun setText(textRes: Int) {
        textView?.setText(textRes)
    }

    fun setText(text: CharSequence) {
        textView?.text = text
    }

    companion object {
        private const val STYLE_HOR = 0
        private const val STYLE_VER = 1
    }
}