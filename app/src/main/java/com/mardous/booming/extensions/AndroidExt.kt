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

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ResolveInfoFlags
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.core.text.HtmlCompat
import com.mardous.booming.appContext

fun hasPie() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

fun hasQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

fun hasR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

fun hasS() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun hasT() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun hasU() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

fun PackageManager.packageInfo(packageName: String = appContext().packageName) =
    runCatching {
        if (hasT()) getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        else getPackageInfo(packageName, 0)
    }.getOrNull()

fun PackageManager.resolveActivity(intent: Intent) =
    if (hasT())
        resolveActivity(intent, ResolveInfoFlags.of(0))
    else resolveActivity(intent, 0)

fun CharSequence.toHtml() = HtmlCompat.fromHtml(this.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT)

fun <R> MediaPlayer.execSafe(command: MediaPlayer.() -> R): R? {
    try {
        return command()
    } catch (t: Throwable) {
        Log.e("MediaPlayer", "Failed to execute a command", t)
    }
    return null
}