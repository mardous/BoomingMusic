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
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.button.MaterialButtonToggleGroup
import com.mardous.booming.R
import com.mardous.booming.util.GeneralTheme
import com.mardous.booming.util.Preferences

/**
 * @author Christians M. A. (mardous)
 */
class ThemePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr), MaterialButtonToggleGroup.OnButtonCheckedListener {

    var customCallback: Callback? = null

    private var widgetView: View? = null
    private val selectorIds = hashMapOf(
        GeneralTheme.LIGHT to R.id.lightTheme,
        GeneralTheme.DARK to R.id.darkTheme,
        GeneralTheme.AUTO to R.id.systemDefault
    )

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        // Disable ripple effect
        holder.itemView.background = null

        val generalTheme = Preferences.generalTheme
        widgetView = holder.findViewById(android.R.id.widget_frame)
        widgetView?.findViewById<MaterialButtonToggleGroup>(R.id.buttonGroup)?.apply {
            isEnabled = (generalTheme != GeneralTheme.BLACK)
            check(generalTheme.let { selectorIds[it] ?: R.id.systemDefault })
            addOnButtonCheckedListener(this@ThemePreference)
        }
    }

    override fun onPrepareForRemoval() {
        super.onPrepareForRemoval()
        customCallback = null
        widgetView = null
    }

    override fun onButtonChecked(group: MaterialButtonToggleGroup, checkedId: Int, isChecked: Boolean) {
        if (isChecked) {
            when (checkedId) {
                R.id.lightTheme -> customCallback?.onThemeSelected(GeneralTheme.LIGHT)
                R.id.darkTheme -> customCallback?.onThemeSelected(GeneralTheme.DARK)
                R.id.systemDefault -> customCallback?.onThemeSelected(GeneralTheme.AUTO)
            }
        }
    }

    interface Callback {
        fun onThemeSelected(themeName: String)
    }
}
