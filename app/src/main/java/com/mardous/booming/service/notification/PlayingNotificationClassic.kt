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

package com.mardous.booming.service.notification

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.mardous.booming.R
import com.mardous.booming.activities.MainActivity
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.glide.asBitmapPalette
import com.mardous.booming.extensions.glide.getSongGlideModel
import com.mardous.booming.extensions.glide.songOptions
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.resources.getPrimaryTextColor
import com.mardous.booming.extensions.resources.getSecondaryTextColor
import com.mardous.booming.extensions.resources.isColorLight
import com.mardous.booming.extensions.resources.toBitmap
import com.mardous.booming.glide.palette.BitmapPaletteWrapper
import com.mardous.booming.helper.color.MediaNotificationProcessor
import com.mardous.booming.model.Song
import com.mardous.booming.service.MusicService
import com.mardous.booming.service.constants.ServiceAction
import com.mardous.booming.util.Preferences

@SuppressLint("RestrictedApi")
class PlayingNotificationClassic(context: Context) : PlayingNotification(context) {

    private var primaryColor: Int = 0

    private var target: Target<*>? = null

    private fun getCombinedRemoteViews(collapsed: Boolean, song: Song): RemoteViews {
        val remoteViews = RemoteViews(
            context.packageName,
            if (collapsed) R.layout.layout_notification_collapsed else R.layout.layout_notification_expanded
        )
        if (hasPie() && !hasS()) {
            remoteViews.setTextViewText(
                R.id.info, "${context.getString(R.string.app_name)} • ${getExtraTextString(song)}"
            )
        } else {
            remoteViews.setTextViewText(R.id.info, getExtraTextString(song))
        }
        remoteViews.setTextViewText(R.id.title, song.title)
        remoteViews.setTextViewText(R.id.subtitle, song.displayArtistName())
        linkButtons(remoteViews)
        return remoteViews
    }

    override fun updateMetadata(song: Song, onUpdate: () -> Unit) {
        if (song == Song.emptySong) return
        val notificationLayout = getCombinedRemoteViews(true, song)
        val notificationLayoutBig = getCombinedRemoteViews(false, song)

        val action = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val clickIntent = PendingIntent
            .getActivity(context, 0, action, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val deleteIntent = buildPendingIntent(context, ServiceAction.ACTION_QUIT, null)

        setSmallIcon(R.drawable.ic_stat_music_playback)
        setContentIntent(clickIntent)
        setDeleteIntent(deleteIntent)
        setCategory(NotificationCompat.CATEGORY_SERVICE)
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setCustomContentView(notificationLayout)
        setCustomBigContentView(notificationLayoutBig)
        setOngoing(true)
        priority = notificationPriority

        val bigNotificationImageSize = context.dip(R.dimen.notification_big_image_size)
        if (target != null) {
            Glide.with(context).clear(target)
        }
        target = Glide.with(context)
            .asBitmapPalette()
            .load(song.getSongGlideModel())
            .songOptions(song)
            .centerCrop()
            .into(object : CustomTarget<BitmapPaletteWrapper>(bigNotificationImageSize, bigNotificationImageSize) {
                override fun onResourceReady(
                    resource: BitmapPaletteWrapper,
                    transition: Transition<in BitmapPaletteWrapper>?,
                ) {
                    val colors = MediaNotificationProcessor(context, resource.bitmap)
                    update(resource.bitmap, colors.backgroundColor)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    update(null, context.resolveColor(com.google.android.material.R.attr.colorSurface, Color.WHITE))
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    update(null, context.resolveColor(com.google.android.material.R.attr.colorSurface, Color.WHITE))
                }

                fun update(bitmap: Bitmap?, bgColor: Int) {
                    var bgColorFinal = bgColor
                    if (bitmap != null) {
                        contentView.setImageViewBitmap(R.id.largeIcon, bitmap)
                        bigContentView.setImageViewBitmap(R.id.largeIcon, bitmap)
                    } else {
                        contentView.setImageViewResource(
                            R.id.largeIcon,
                            R.drawable.default_audio_art
                        )
                        bigContentView.setImageViewResource(
                            R.id.largeIcon,
                            R.drawable.default_audio_art
                        )
                    }

                    // Android 12 applies a standard Notification template to every notification
                    // which will in turn have a default background so setting a different background
                    // than that, looks weird
                    if (!hasS()) {
                        if (!Preferences.coloredNotification) {
                            bgColorFinal = context.resolveColor(com.google.android.material.R.attr.colorSurface, Color.WHITE)
                        }
                        setBackgroundColor(bgColorFinal)
                        setNotificationContent(bgColorFinal.isColorLight)
                    } else {
                        if (Preferences.coloredNotification) {
                            setColorized(true)
                            color = bgColor
                            setNotificationContent(color.isColorLight)
                        } else {
                            setNotificationContent(!context.isNightMode)
                        }
                    }
                    onUpdate()
                }

                private fun setBackgroundColor(color: Int) {
                    contentView.setInt(R.id.root, "setBackgroundColor", color)
                    bigContentView.setInt(R.id.root, "setBackgroundColor", color)
                }

                private fun setNotificationContent(dark: Boolean) {
                    val primary = getPrimaryTextColor(context, dark)
                    val secondary = getSecondaryTextColor(context, dark)
                    primaryColor = primary

                    val close = context.getTintedDrawable(
                        R.drawable.ic_close_24dp,
                        primary
                    )!!.toBitmap()
                    val prev = context.getTintedDrawable(
                        R.drawable.ic_previous_24dp,
                        primary
                    )!!.toBitmap()
                    val next = context.getTintedDrawable(
                        R.drawable.ic_next_24dp,
                        primary
                    )!!.toBitmap()
                    val playPause = getPlayPauseBitmap(true)

                    contentView.setTextColor(R.id.title, primary)
                    contentView.setTextColor(R.id.subtitle, secondary)
                    contentView.setTextColor(R.id.appName, secondary)

                    contentView.setImageViewBitmap(R.id.action_prev, prev)
                    contentView.setImageViewBitmap(R.id.action_next, next)
                    contentView.setImageViewBitmap(R.id.action_play_pause, playPause)

                    bigContentView.setTextColor(R.id.title, primary)
                    bigContentView.setTextColor(R.id.subtitle, secondary)
                    bigContentView.setTextColor(R.id.appName, secondary)
                    bigContentView.setTextColor(R.id.info, secondary)

                    bigContentView.setImageViewBitmap(R.id.action_quit, close)
                    bigContentView.setImageViewBitmap(R.id.action_prev, prev)
                    bigContentView.setImageViewBitmap(R.id.action_next, next)
                    bigContentView.setImageViewBitmap(R.id.action_play_pause, playPause)

                    contentView.setImageViewBitmap(
                        R.id.smallIcon,
                        context.getTintedDrawable(R.drawable.ic_stat_music_playback, secondary)!!.toBitmap(0.6f)
                    )
                    bigContentView.setImageViewBitmap(
                        R.id.smallIcon,
                        context.getTintedDrawable(R.drawable.ic_stat_music_playback, secondary)!!.toBitmap(0.6f)
                    )
                }
            })
    }

    override fun setPlaying(isPlaying: Boolean) {
        getPlayPauseBitmap(isPlaying).also {
            contentView?.setImageViewBitmap(R.id.action_play_pause, it)
            bigContentView?.setImageViewBitmap(R.id.action_play_pause, it)
        }
    }

    override fun updateFavorite(isFavorite: Boolean) {}

    private fun getPlayPauseBitmap(isPlaying: Boolean): Bitmap {
        return context.getTintedDrawable(
            if (isPlaying)
                R.drawable.ic_pause_48dp
            else
                R.drawable.ic_play_48dp, primaryColor
        )!!.toBitmap()
    }

    private fun linkButtons(contentView: RemoteViews) {
        var pendingIntent: PendingIntent
        val serviceName = ComponentName(context, MusicService::class.java)

        // Previous track
        pendingIntent = buildPendingIntent(context, ServiceAction.ACTION_PREVIOUS, serviceName)
        contentView.setOnClickPendingIntent(R.id.action_prev, pendingIntent)

        // Play and pause
        pendingIntent = buildPendingIntent(context, ServiceAction.ACTION_TOGGLE_PAUSE, serviceName)
        contentView.setOnClickPendingIntent(R.id.action_play_pause, pendingIntent)

        // Next track
        pendingIntent = buildPendingIntent(context, ServiceAction.ACTION_NEXT, serviceName)
        contentView.setOnClickPendingIntent(R.id.action_next, pendingIntent)

        // Close
        pendingIntent = buildPendingIntent(context, ServiceAction.ACTION_QUIT, serviceName)
        contentView.setOnClickPendingIntent(R.id.action_quit, pendingIntent)
    }

    private fun buildPendingIntent(context: Context, action: String, serviceName: ComponentName?): PendingIntent {
        return Intent(action)
            .setComponent(serviceName).let { PendingIntent.getService(context, 0, it, PendingIntent.FLAG_IMMUTABLE) }
    }

    companion object {

        fun from(context: Context, notificationManager: NotificationManager): PlayingNotification {
            createNotificationChannel(context, notificationManager)
            return PlayingNotificationClassic(context)
        }
    }
}