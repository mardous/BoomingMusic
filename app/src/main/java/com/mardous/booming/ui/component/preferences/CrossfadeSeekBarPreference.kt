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

package com.mardous.booming.ui.component.preferences

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.preference.PreferenceViewHolder
import androidx.preference.R
import androidx.preference.SeekBarPreference

class CrossfadeSeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.seekBarPreferenceStyle,
    defStyleRes: Int = 0
) : SeekBarPreference(context, attrs, defStyleAttr, defStyleRes) {

    private var isFormatting = false

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val tv = holder.findViewById(R.id.seekbar_value) as? TextView
        if (tv != null) {
            updateValueText(tv)
            tv.doAfterTextChanged { text ->
                if (isFormatting) return@doAfterTextChanged
                val str = text?.toString() ?: return@doAfterTextChanged
                if (str.isNotEmpty() && !str.endsWith(" s")) {
                    isFormatting = true
                    val numeric = str.replace(" s", "").trim()
                    tv.text = "$numeric s"
                    isFormatting = false
                }
            }
        }
    }

    private fun updateValueText(tv: TextView) {
        val str = tv.text?.toString() ?: return
        if (str.isNotEmpty() && !str.endsWith(" s")) {
            isFormatting = true
            val numeric = str.replace(" s", "").trim()
            tv.text = "$numeric s"
            isFormatting = false
        }
    }
}
