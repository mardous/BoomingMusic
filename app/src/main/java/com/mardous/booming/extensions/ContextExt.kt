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

@file:SuppressLint("DiscouragedApi", "InternalInsetResource")

package com.mardous.booming.extensions

import android.annotation.SuppressLint
import android.app.*
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.ShapeAppearanceModel
import com.mardous.booming.extensions.files.readString
import com.mardous.booming.extensions.resources.getDrawableCompat
import com.mardous.booming.extensions.resources.getTinted
import com.mardous.booming.model.theme.AppTheme
import com.mardous.booming.util.AutoDownloadMetadataPolicy
import com.mardous.booming.util.Preferences
import io.ktor.http.encodeURLParameter

val Context.fileProviderAuthority: String
    get() = "$packageName.provider"

fun Float.dp(context: Context) = dp(context.resources)
fun Int.dp(context: Context) = dp(context.resources)

fun Float.dp(resources: Resources): Int = (this * resources.displayMetrics.density + 0.5f).toInt()
fun Int.dp(resources: Resources): Int = (this * resources.displayMetrics.density + 0.5f).toInt()

private val Resources.isNightMode: Boolean
    get() = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

val Resources.isLandscape: Boolean
    get() = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

/**
 * Indicates if the app is running on a **Android Auto** environemnt.
 */
val Resources.isCarMode: Boolean
    get() = configuration.uiMode == Configuration.UI_MODE_TYPE_CAR

val Resources.isTablet: Boolean
    get() = configuration.smallestScreenWidthDp >= 600

val Resources.isScreenLarge: Boolean
    get() = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_LARGE

val Context.isNightMode: Boolean
    get() = resources.isNightMode

val Context.systemContrast: Float
    get() = (if (hasU()) getSystemService<UiModeManager>()?.contrast else null) ?: 0f

fun Fragment.dip(id: Int) = resources.getDimensionPixelSize(id)

fun Fragment.intRes(id: Int) = resources.getInteger(id)

fun Context.dip(id: Int) = resources.getDimensionPixelSize(id)

fun Context.intRes(id: Int) = resources.getInteger(id)

fun Context.openUrl(url: String) =
    tryStartActivity(
        intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ) {
        if (it is ActivityNotFoundException) {
            showToast("No browser installed.")
        }
    }

fun Context.tryStartActivity(intent: Intent, onError: (Throwable) -> Unit = {}) = try {
    startActivity(intent)
} catch (t: Throwable) {
    onError(t)
}

fun Context.webSearch(vararg keys: String?) {
    val query = keys.filterNotNull().joinToString(" ")
    tryStartActivity(
        Intent(Intent.ACTION_WEB_SEARCH)
            .putExtra(SearchManager.QUERY, query)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ) {
        openUrl("https://google.com/search?q=${query.encodeURLParameter(spaceToPlus = true)}")
    }
}

fun Context.isOnline(requestOnlyWifi: Boolean): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)
    if (networkCapabilities != null) {
        return if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            true
        } else networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && !requestOnlyWifi
    }
    return false
}

fun Context.isAllowedToDownloadMetadata() = Preferences.autoDownloadMetadataPolicy.let { policy ->
    policy != AutoDownloadMetadataPolicy.NEVER && isOnline(AutoDownloadMetadataPolicy.ONLY_WIFI == policy)
}

fun Context.onUI(action: () -> Unit) {
    if (this is Activity) {
        runOnUiThread { action() }
    } else {
        Handler(Looper.getMainLooper()).post(action)
    }
}

fun Context.showToast(textId: Int, duration: Int = Toast.LENGTH_SHORT) {
    if (textId == 0)
        return

    showToast(getString(textId), duration)
}

fun Context.showToast(text: String?, duration: Int = Toast.LENGTH_SHORT) {
    if (text.isNullOrEmpty())
        return

    if (Looper.myLooper() != Looper.getMainLooper()) {
        onUI { Toast.makeText(applicationContext, text, duration).show() }
    } else {
        Toast.makeText(applicationContext, text, duration).show()
    }
}

fun Context.createNotificationChannel(
    channelId: String,
    channelName: String,
    channelDescription: String?,
    notificationManager: NotificationManager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
): NotificationChannel {
    var notificationChannel = notificationManager.getNotificationChannel(channelId)
    if (notificationChannel == null) {
        notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW).apply {
            description = channelDescription
            enableLights(false)
            enableVibration(false)
        }.also {
            notificationManager.createNotificationChannel(it)
        }
    }
    return notificationChannel
}

fun Context.readStringFromAsset(assetName: String): String? =
    runCatching { assets.open(assetName).use { it.readString() } }.getOrNull()

fun Context.getTintedDrawable(@DrawableRes resId: Int, @ColorInt color: Int): Drawable? =
    getDrawableCompat(resId).getTinted(color)

@Suppress("DEPRECATION")
fun Context.getScreenSize(): Point {
    if (hasR()) {
        val windowMetrics = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).currentWindowMetrics
        val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
        )
        val bounds = windowMetrics.bounds
        val insetsHeight = insets.top + insets.bottom
        val insetsWidth = insets.left + insets.right
        return Point(bounds.width() - insetsWidth, bounds.height() - insetsHeight)
    }
    return (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.let { display ->
        Point().also { point ->
            display.getSize(point)
        }
    }
}

fun Context.getShapeAppearanceModel(shapeAppearanceId: Int, shapeAppearanceOverlayId: Int = 0) =
    ShapeAppearanceModel.builder(this, shapeAppearanceId, shapeAppearanceOverlayId).build()

fun Context.getNavigationBarHeight(): Int {
    var result = 0
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = dip(resourceId)
    }
    return result
}

fun Context.plurals(@PluralsRes resId: Int, quantity: Int): String {
    return try {
        resources.getQuantityString(resId, quantity, quantity)
    } catch (e: Resources.NotFoundException) {
        e.printStackTrace()
        quantity.toString()
    }
}

/**
 * Try to resolve the given color attribute against *this* [Context]'s theme.
 */
@ColorInt
fun Context.resolveColor(@AttrRes colorAttr: Int, @ColorInt fallback: Int = Color.TRANSPARENT) =
    MaterialColors.getColor(this, colorAttr, fallback)

fun Context.createAppTheme() = AppTheme.createAppTheme(this)