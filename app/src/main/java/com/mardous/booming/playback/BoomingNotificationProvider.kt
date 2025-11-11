package com.mardous.booming.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import com.mardous.booming.R
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.util.NotificationExtraText
import com.mardous.booming.util.Preferences

@OptIn(UnstableApi::class)
class BoomingNotificationProvider(
    context: Context,
    notificationId: Int,
    channelId: String,
    channelDescriptionRes: Int
) : DefaultMediaNotificationProvider(context, { session -> notificationId }, channelId, channelDescriptionRes) {

    init {
        setSmallIcon(R.drawable.ic_stat_music_playback)
    }

    override fun getNotificationContentText(metadata: MediaMetadata): CharSequence? {
        return when (Preferences.notificationExtraTextLine) {
            NotificationExtraText.ARTIST_NAME -> metadata.artist
            NotificationExtraText.ALBUM_NAME -> metadata.albumTitle
            NotificationExtraText.ALBUM_AND_YEAR -> buildInfoString(metadata.albumTitle, metadata.releaseYear)
            NotificationExtraText.ALBUM_ARTIST_NAME -> metadata.albumArtist
            else -> metadata.albumTitle
        }
    }
}