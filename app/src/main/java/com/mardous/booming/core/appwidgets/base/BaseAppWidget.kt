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

package com.mardous.booming.core.appwidgets.base

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mardous.booming.R
import com.mardous.booming.coil.DEFAULT_SONG_IMAGE
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.media.songInfo
import com.mardous.booming.extensions.resources.getDrawableCompat
import com.mardous.booming.service.MusicService
import com.mardous.booming.service.constants.ServiceAction

abstract class BaseAppWidget : AppWidgetProvider() {
    /**
     * {@inheritDoc}
     */
    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        defaultAppWidget(context, appWidgetIds)
        val updateIntent = Intent(ServiceAction.ACTION_APP_WIDGET_UPDATE)
        updateIntent.setPackage(context.packageName)
        updateIntent.putExtra(ServiceAction.Extras.EXTRA_APP_WIDGET_NAME, NAME)
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
        LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
    }

    /**
     * Handle a change notification coming over from
     * [MusicService]
     */
    fun notifyChange(service: MusicService) {
        if (hasInstances(service)) {
            performUpdate(service, null)
        }
    }

    protected fun pushUpdate(context: Context, appWidgetIds: IntArray?, views: RemoteViews) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetIds != null) {
            appWidgetManager.updateAppWidget(appWidgetIds, views)
        } else {
            appWidgetManager.updateAppWidget(ComponentName(context, javaClass), views)
        }
    }

    /**
     * Check against [AppWidgetManager] if there are any instances of this
     * widget.
     */
    private fun hasInstances(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val mAppWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass))
        return mAppWidgetIds.isNotEmpty()
    }

    protected fun buildPendingIntent(context: Context, action: String, serviceName: ComponentName): PendingIntent {
        val intent = Intent(action).setComponent(serviceName)
        return PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    protected abstract fun defaultAppWidget(context: Context, appWidgetIds: IntArray)

    abstract fun performUpdate(service: MusicService, appWidgetIds: IntArray?)

    @Suppress("DEPRECATION")
    protected fun getAlbumArtDrawable(context: Context, bitmap: Bitmap?): Drawable {
        return bitmap?.toDrawable(context.resources)
            ?: context.getDrawableCompat(DEFAULT_SONG_IMAGE)!!
    }

    protected fun getSongArtistAndAlbum(song: Song): String {
        return song.songInfo(true)
    }

    protected fun getInnerRadius(context: Context): Float {
        if (innerRadius == 0F) {
            innerRadius = context.resources.getDimension(R.dimen.widget_inner_radius)
        }
        return innerRadius
    }

    protected fun getWidgetRadius(context: Context): Float {
        if (widgetRadius == 0f) {
            widgetRadius = context.resources.getDimension(R.dimen.widget_background_radius)
        }
        return widgetRadius
    }

    companion object {
        const val NAME = "app_widget"

        private var widgetRadius = 0F
        private var innerRadius = 0F

        @JvmStatic
        protected fun createRoundedBitmap(
            drawable: Drawable?,
            width: Int,
            height: Int,
            tl: Float,
            tr: Float,
            bl: Float,
            br: Float
        ): Bitmap? {
            if (drawable == null)
                return null

            val bitmap = createBitmap(width, height)
            val c = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(c)
            val rounded = createBitmap(width, height)
            val canvas = Canvas(rounded)
            val paint = Paint()
            paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            paint.isAntiAlias = true
            canvas.drawPath(
                composeRoundedRectPath(RectF(0f, 0f, width.toFloat(), height.toFloat()), tl, tr, bl, br),
                paint
            )
            return rounded
        }

        protected fun composeRoundedRectPath(rect: RectF, tl: Float, tr: Float, bl: Float, br: Float): Path {
            val topLeft = if (tl < 0) 0f else tl
            val topRight = if (tr < 0) 0f else tr
            val bottomLeft = if (bl < 0) 0f else bl
            val bottomRight = if (br < 0) 0f else br

            val path = Path()
            path.moveTo(rect.left + topLeft, rect.top)
            path.lineTo(rect.right - topRight, rect.top)
            path.quadTo(rect.right, rect.top, rect.right, rect.top + topRight)
            path.lineTo(rect.right, rect.bottom - bottomRight)
            path.quadTo(rect.right, rect.bottom, rect.right - bottomRight, rect.bottom)
            path.lineTo(rect.left + bottomLeft, rect.bottom)
            path.quadTo(rect.left, rect.bottom, rect.left, rect.bottom - bottomLeft)
            path.lineTo(rect.left, rect.top + topLeft)
            path.quadTo(rect.left, rect.top, rect.left + topLeft, rect.top)
            path.close()
            return path
        }
    }
}