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
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.mardous.booming.R
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.dip
import com.mardous.booming.extensions.glide.getSongGlideModel
import com.mardous.booming.extensions.glide.songOptions
import com.mardous.booming.extensions.hasS
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.service.MusicService
import com.mardous.booming.service.constants.ServiceAction
import com.mardous.booming.ui.screen.MainActivity

class PlayingNotificationImpl24(
    context: Context,
    channelId: String,
    mediaSessionToken: MediaSessionCompat.Token
) : PlayingNotification(context, channelId) {

    private var currentTarget: CustomTarget<Bitmap>? = null

    init {
        val action = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val clickIntent = PendingIntent.getActivity(
            context, 0, action, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val serviceName = ComponentName(context, MusicService::class.java)
        val intent = Intent(ServiceAction.ACTION_QUIT)
            .setComponent(serviceName)

        val deleteIntent = PendingIntent.getService(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val toggleFavorite = buildFavoriteAction(false)
        val playPauseAction = buildPlayAction(true)
        val previousAction = NotificationCompat.Action(
            R.drawable.ic_previous_24dp,
            context.getString(R.string.action_previous),
            retrievePlaybackAction(ServiceAction.ACTION_PREVIOUS)
        )
        val nextAction = NotificationCompat.Action(
            R.drawable.ic_next_24dp,
            context.getString(R.string.action_next),
            retrievePlaybackAction(ServiceAction.ACTION_NEXT)
        )
        val dismissAction = NotificationCompat.Action(
            R.drawable.ic_close_24dp,
            context.getString(R.string.action_cancel),
            retrievePlaybackAction(ServiceAction.ACTION_QUIT)
        )

        setSmallIcon(R.drawable.ic_stat_music_playback)
        setContentIntent(clickIntent)
        setDeleteIntent(deleteIntent)
        setShowWhen(false)
        addAction(toggleFavorite) //0
        addAction(previousAction) //1
        addAction(playPauseAction) //2
        addAction(nextAction) //3
        if (hasS()) {
            addAction(dismissAction) //4
        }

        setStyle(
            MediaStyle()
                .setMediaSession(mediaSessionToken)
                .setShowActionsInCompactView(1, 2, 3)
        )
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        priority = notificationPriority
    }

    override fun updateMetadata(song: Song, onUpdate: () -> Unit) {
        if (song == Song.emptySong) return

        setContentTitle(song.title)
        setContentText(song.displayArtistName())
        setSubText(getExtraTextString(song))
        val bigNotificationImageSize = context.dip(R.dimen.notification_big_image_size)

        setAlbumArtImage(null)

        currentTarget?.let { Glide.with(context).clear(it) }
        currentTarget = Glide.with(context)
            .asBitmap()
            .songOptions(song)
            .load(song.getSongGlideModel())
            .centerCrop()
            .into(object : CustomTarget<Bitmap>(bigNotificationImageSize, bigNotificationImageSize) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    setAlbumArtImage(resource)
                    onUpdate()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    setAlbumArtImage(null)
                    onUpdate()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    setAlbumArtImage(null)
                    onUpdate()
                }
            })
    }

    @SuppressLint("RestrictedApi")
    override fun setPlaying(isPlaying: Boolean) {
        mActions[2] = buildPlayAction(isPlaying)
    }

    @SuppressLint("RestrictedApi")
    override fun updateFavorite(isFavorite: Boolean) {
        mActions[0] = buildFavoriteAction(isFavorite)
    }

    private fun setAlbumArtImage(image: Bitmap?) {
        if (image == null) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.default_audio_art))
        } else {
            setLargeIcon(image)
        }
    }

    private fun buildPlayAction(isPlaying: Boolean): NotificationCompat.Action {
        val playButtonResId = if (isPlaying) R.drawable.ic_pause_48dp else R.drawable.ic_play_48dp
        return NotificationCompat.Action.Builder(
            playButtonResId,
            context.getString(R.string.action_play_pause),
            retrievePlaybackAction(ServiceAction.ACTION_TOGGLE_PAUSE)
        ).build()
    }

    private fun buildFavoriteAction(isFavorite: Boolean): NotificationCompat.Action {
        val favoriteResId =
            if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
        return NotificationCompat.Action.Builder(
            favoriteResId,
            context.getString(R.string.toggle_favorite),
            retrievePlaybackAction(ServiceAction.ACTION_TOGGLE_FAVORITE)
        ).build()
    }

    private fun retrievePlaybackAction(action: String): PendingIntent {
        val serviceName = ComponentName(context, MusicService::class.java)
        val intent = Intent(action)
        intent.component = serviceName
        return PendingIntent.getService(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}