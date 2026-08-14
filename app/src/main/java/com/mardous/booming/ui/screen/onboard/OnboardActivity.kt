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

package com.mardous.booming.ui.screen.onboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.preference.PreferenceManager
import com.mardous.booming.extensions.EXTRA_IS_PERMISSION_REQUEST
import com.mardous.booming.extensions.hasS
import com.mardous.booming.extensions.observeKeyAsState
import com.mardous.booming.ui.component.base.AbsBaseActivity
import com.mardous.booming.ui.screen.MainActivity
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.util.GENERAL_THEME
import com.mardous.booming.util.GeneralTheme
import com.mardous.booming.util.MATERIAL_YOU
import com.mardous.booming.util.Preferences

/**
 * @author Christians M. A. (mardous)
 */
class OnboardActivity : AbsBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isPermissionRequest = intent.getBooleanExtra(EXTRA_IS_PERMISSION_REQUEST, false)

        setContent {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)

            val generalTheme by prefs.observeKeyAsState(GENERAL_THEME, GeneralTheme.AUTO)
            val materialYou by prefs.observeKeyAsState(MATERIAL_YOU, hasS())

            BoomingMusicTheme(
                darkTheme = when (generalTheme) {
                    GeneralTheme.LIGHT -> false
                    GeneralTheme.DARK -> true
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = materialYou
            ) {
                OnboardScreen(
                    onFinish = {
                        Preferences.onboardShown = true
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    },
                    onBackToExit = {
                        finishAffinity()
                    },
                    availableSteps = if (isPermissionRequest) {
                        listOf(OnboardStep.PERMISSIONS)
                    } else {
                        OnboardStep.entries
                    }
                )
            }
        }
    }
}