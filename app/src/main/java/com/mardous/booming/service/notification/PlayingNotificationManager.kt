package com.mardous.booming.service.notification

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.mardous.booming.R
import com.mardous.booming.ui.screen.MainActivity
import com.mardous.booming.data.model.Song
import com.mardous.booming.service.playback.PlaybackManager
import com.mardous.booming.service.queue.QueueManager
import com.mardous.booming.util.Preferences

class PlayingNotificationManager(
    private val context: Context,
    private val mediaSession: MediaSessionCompat,
    private val playbackManager: PlaybackManager,
    private val queueManager: QueueManager
) {
    private var notificationManager: NotificationManager = requireNotNull(context.getSystemService())
    private var playingNotification: PlayingNotification? = null

    init {
        var notificationChannel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
        if (notificationChannel == null) {
            val channelName = context.getString(R.string.playing_notification_name)
            val channelDescription = context.getString(R.string.playing_notification_name)
            notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW).apply {
                description = channelDescription
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }
        createNotification()
    }

    private fun createNotification() {
        playingNotification = if (Preferences.classicNotification) {
            PlayingNotificationClassic(context, NOTIFICATION_CHANNEL_ID)
        } else {
            PlayingNotificationImpl24(context, NOTIFICATION_CHANNEL_ID, mediaSession.sessionToken)
        }
    }

    private fun createContentPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun recreateNotification(service: Service) {
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        createNotification()
    }

    fun displayPlayingNotification(
        song: Song = queueManager.currentSong,
        isPlaying: Boolean = playbackManager.isPlaying(),
        isFavorite: Boolean? = null
    ): Notification? {
        if (!queueManager.isEmpty && song != Song.emptySong) {
            playingNotification?.updateMetadata(song) {
                playingNotification?.setPlaying(isPlaying)
                if (isFavorite != null) {
                    playingNotification?.updateFavorite(isFavorite)
                }
                notificationManager.notify(NOTIFICATION_ID, playingNotification!!.build())
            }
            return playingNotification!!.build()
        }
        return null
    }

    fun displayPlayingNotification(isPlaying: Boolean): Notification {
        playingNotification?.setPlaying(isPlaying)
        val notification = playingNotification!!.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        return notification
    }

    fun displayPendingNotification(): Notification {
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.preparing_playback_title))
            .setContentText(context.getString(R.string.restoring_playback_state))
            .setSmallIcon(R.drawable.ic_stat_music_playback)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(createContentPendingIntent())
            .setShowWhen(false)
            .setSilent(true)
            .setOngoing(true)
            .build().also {
                notificationManager.notify(NOTIFICATION_ID, it)
            }
    }

    fun displayEmptyQueueNotification(): Notification {
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.playback_stopped_title))
            .setContentText(context.getString(R.string.empty_play_queue))
            .setSmallIcon(R.drawable.ic_stat_music_playback)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(createContentPendingIntent())
            .setShowWhen(false)
            .setSilent(true)
            .setOngoing(true)
            .build().also {
                notificationManager.notify(NOTIFICATION_ID, it)
            }
    }

    fun startForeground(service: Service, notificationProvider: PlayingNotificationManager.() -> Notification?): Boolean {
        val notification = notificationProvider()
        if (notification != null) {
            startForeground(service, notification)
            return true
        }
        return false
    }

    @SuppressLint("InlinedApi")
    fun startForeground(service: Service, notification: Notification) {
        ServiceCompat.startForeground(
            service,
            NOTIFICATION_ID,
            notification,
            FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "playing_notification"
    }
}