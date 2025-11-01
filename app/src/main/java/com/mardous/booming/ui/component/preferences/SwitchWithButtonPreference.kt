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

package com.mardous.booming.ui.component.preferences

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.mardous.booming.R

/**
 * @author Christians M. A. (mardous)
 */
class SwitchWithButtonPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.switchPreferenceCompatStyle,
    defStyleRes: Int = 0
) : SwitchPreferenceCompat(context, attrs, defStyleAttr, defStyleRes) {

    interface OnButtonPressedListener {
        fun onButtonPressed()
    }

    private var button: Button? = null
    private var listener: OnButtonPressedListener? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        button = holder.findViewById(R.id.button) as? Button
        button?.setOnClickListener {
            listener?.onButtonPressed()
        }
    }

    override fun onDetached() {
        super.onDetached()
        button = null
        listener = null
    }

    fun setButtonPressedListener(listener: OnButtonPressedListener) {
        this.listener = listener
    }
}