package com.mardous.booming.core.appwidgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartService
import com.mardous.booming.core.appwidgets.config.SongSource
import com.mardous.booming.playback.PlaybackService
import com.mardous.booming.ui.screen.MainActivity
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityIntent

fun playbackAction(context: Context, mediaKeyCode: Int): Action {
    val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
    intent.component = ComponentName(context, PlaybackService::class.java)
    intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyCode))
    return actionStartService(intent, true)
}

fun toggleShuffleAction(context: Context): Action =
    widgetAction(context, WidgetActionReceiver.ACTION_TOGGLE_SHUFFLE)

fun cycleRepeatAction(context: Context): Action =
    widgetAction(context, WidgetActionReceiver.ACTION_CYCLE_REPEAT)

fun toggleFavoriteAction(context: Context): Action =
    widgetAction(context, WidgetActionReceiver.ACTION_TOGGLE_FAVORITE)

private fun widgetAction(context: Context, action: String): Action {
    val intent = Intent(action)
    intent.component = ComponentName(context, WidgetActionReceiver::class.java)
    return actionSendBroadcast(intent)
}

fun GlanceModifier.openApp(): GlanceModifier = clickable(actionStartActivity<MainActivity>())

/** Opens the list itself */
@Composable
fun GlanceModifier.openList(source: SongSource): GlanceModifier =
    clickable(openListAction(LocalContext.current, source))

private fun openListAction(context: Context, source: SongSource): Action {
    val intent = Intent(MainActivity.ACTION_SHOW_CONTENT)
    intent.component = ComponentName(context, MainActivity::class.java)
    intent.putExtra(MainActivity.EXTRA_CONTENT_TYPE, source.contentType.name)
    intent.data = "booming://widget/list/${source.name}".toUri()
    return actionStartActivityIntent(intent)
}

@Composable
fun GlanceModifier.playSong(songId: Long, source: SongSource): GlanceModifier =
    clickable(playSongAction(LocalContext.current, songId, source))

private fun playSongAction(context: Context, songId: Long, source: SongSource): Action {
    val intent = Intent(WidgetActionReceiver.ACTION_PLAY_SONG)
    intent.component = ComponentName(context, WidgetActionReceiver::class.java)
    intent.putExtra(WidgetActionReceiver.EXTRA_SONG_ID, songId)
    intent.putExtra(WidgetActionReceiver.EXTRA_SONG_SOURCE, source.name)
    // Distinct data prevents PendingIntent reuse across covers
    intent.data = "booming://widget/play/${source.name}/$songId".toUri()
    return actionSendBroadcast(intent)
}