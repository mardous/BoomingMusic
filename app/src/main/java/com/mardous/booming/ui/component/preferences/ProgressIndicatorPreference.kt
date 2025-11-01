/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.progressindicator.CircularProgressIndicator

/**
 * @author Christians M. A. (mardous)
 */
class ProgressIndicatorPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr) {

    private var progressIndicator: CircularProgressIndicator? = null
    private var shouldShowProgress = false

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        progressIndicator = holder.findViewById(android.R.id.progress) as? CircularProgressIndicator
        applyProgressVisibility()
    }

    override fun onPrepareForRemoval() {
        super.onPrepareForRemoval()
        progressIndicator = null
    }

    fun showProgressIndicator() {
        shouldShowProgress = true
        applyProgressVisibility()
    }

    fun hideProgressIndicator() {
        shouldShowProgress = false
        applyProgressVisibility()
    }

    private fun applyProgressVisibility() {
        progressIndicator?.let {
            if (shouldShowProgress) {
                it.show()
            } else {
                it.hide()
            }
        }
    }
}
