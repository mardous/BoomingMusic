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

package com.mardous.booming

import android.app.Application
import android.content.Context
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.bumptech.glide.Glide
import com.mardous.booming.ui.screen.error.ErrorActivity
import com.mardous.booming.ui.screen.settings.SettingsScreen
import com.mardous.booming.data.local.ReplayGainTagExtractor
import com.mardous.booming.ui.screen.MainActivity
import com.mardous.booming.util.EXPERIMENTAL_UPDATES
import com.mardous.booming.util.Preferences.getDayNightMode
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

fun appInstance(): App = App.instance
fun appContext(): Context = appInstance().applicationContext

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin {
            androidContext(this@App)
            modules(appModules)
        }

        if (BuildConfig.DEBUG) enableStrictMode()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        // we cannot call setDefaultValues for multiple fragment based XML preference
        // files with readAgain flag set to false, so always check KEY_HAS_SET_DEFAULT_VALUES
        if (!prefs.getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
            for (screen in SettingsScreen.entries) {
                PreferenceManager.setDefaultValues(this, screen.layoutRes, true)
            }
            if (isExperimentalBuild()) {
                prefs.edit { putBoolean(EXPERIMENTAL_UPDATES, true) }
            }
        }

        // setting Error activity
        CaocConfig.Builder.create()
            .errorActivity(ErrorActivity::class.java)
            .restartActivity(MainActivity::class.java)
            .apply()

        AppCompatDelegate.setDefaultNightMode(getDayNightMode())
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Glide.get(this).clearMemory()
        ReplayGainTagExtractor.clearCache()
    }

    private fun enableStrictMode() {
        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )

        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyFlashScreen()
                .build()
        )
    }

    companion object {
        @get:Synchronized
        internal lateinit var instance: App
            private set

        fun isExperimentalBuild(): Boolean =
            BuildConfig.VERSION_NAME.contains("(alpha|beta|rc)".toRegex(RegexOption.IGNORE_CASE))
    }
}