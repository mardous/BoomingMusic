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

package com.mardous.booming.core.model.theme

import android.content.Context
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors
import com.mardous.booming.R
import com.mardous.booming.util.GeneralTheme
import com.mardous.booming.util.Preferences

class AppTheme private constructor(
    val id: String,
    val mode: Mode,
    val applyDynamicColors: Boolean,
    val seedColor: Int? = null
) {

    @StyleRes
    val themeRes: Int
        get() = mode.themeRes

    val isBlackTheme: Boolean
        get() = id == GeneralTheme.BLACK

    enum class Mode(@StyleRes val themeRes: Int) {
        Light(R.style.Theme_Booming_Light),
        Dark(R.style.Theme_Booming),
        Black(R.style.Theme_Booming_Black),
        FollowSystem(R.style.Theme_Booming_FollowSystem)
    }

    companion object {
        fun createAppTheme(context: Context): AppTheme {
            val generalTheme = Preferences.generalTheme
            // Auto+black: use FollowSystem so DayNight handles switching; BlackThemeOverlay
            // is applied separately (night-qualified) to get pure black only in dark mode.
            val themeMode = if (Preferences.isAutoBlackTheme) Mode.FollowSystem else Preferences.getThemeMode(generalTheme)
            if (DynamicColors.isDynamicColorAvailable()) {
                if (Preferences.isMaterialYouTheme) {
                    return AppTheme(
                        id = generalTheme,
                        mode = themeMode,
                        applyDynamicColors = true
                    )
                }
                if (context is ContextThemeWrapper) {
                    return AppTheme(
                        id = generalTheme,
                        mode = themeMode,
                        applyDynamicColors = true,
                        seedColor = ContextCompat.getColor(context, R.color.md_theme_primary)
                    )
                }
            }
            return AppTheme(
                id = generalTheme,
                mode = themeMode,
                applyDynamicColors = false
            )
        }
    }
}