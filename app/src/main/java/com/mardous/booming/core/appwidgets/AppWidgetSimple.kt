package com.mardous.booming.core.appwidgets

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import coil3.Image
import coil3.SingletonImageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import coil3.target.Target
import coil3.toBitmap
import com.mardous.booming.R
import com.mardous.booming.core.appwidgets.base.BaseAppWidget
import com.mardous.booming.extensions.resources.getDrawableCompat
import com.mardous.booming.playback.PlaybackService
import com.mardous.booming.ui.screen.MainActivity

class AppWidgetSimple : BaseAppWidget() {

    private var disposable: Disposable? = null // for cancellation

    /**
     * Initialize given widgets to default state, where we launch Music on default click and hide
     * actions if service not running.
     */
    override fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val appWidgetView = RemoteViews(context.packageName, R.layout.app_widget_simple)

        val radius = getWidgetRadius(context)
        val imageSize = getImageSize(context)
        appWidgetView.setImageViewBitmap(
            R.id.image,
            createRoundedBitmap(
                context.getDrawableCompat(R.drawable.default_audio_art),
                imageSize, imageSize, radius, 0f, radius, 0f
            )
        )

        linkButtons(context, appWidgetView)
        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    override fun performUpdate(service: PlaybackService, appWidgetIds: IntArray?) {
        val appWidgetView = RemoteViews(service.packageName, R.layout.app_widget_simple)

        val isPlaying = service.isPlaying
        val song = service.currentSong

        // Set the titles and artwork
        if (song.title.isEmpty() && song.artistName.isEmpty()) {
            appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE)
        } else {
            appWidgetView.setViewVisibility(R.id.media_titles, View.VISIBLE)
            appWidgetView.setTextViewText(R.id.title, song.title)
            appWidgetView.setTextViewText(R.id.text, getSongArtistAndAlbum(song))
        }

        // Set correct drawable for pause state
        val playPauseRes = if (isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_32dp
        appWidgetView.setImageViewResource(R.id.button_toggle_play_pause, playPauseRes)

        // Link actions buttons to intents
        linkButtons(service, appWidgetView)

        // Load the album cover async and push the update on completion
        val imageSize = getImageSize(service)
        uiHandler.post {
            if (disposable != null) {
                disposable?.dispose()
                disposable = null
            }
            disposable = SingletonImageLoader.get(service).enqueue(
                ImageRequest.Builder(service)
                    .data(song)
                    .size(imageSize)
                    .scale(Scale.FILL)
                    .crossfade(false)
                    .target(object : Target {
                        override fun onError(error: Image?) {
                            update(null)
                        }

                        override fun onSuccess(result: Image) {
                            update(result.toBitmap())
                        }

                        private fun update(bitmap: Bitmap?) {
                            val image = getAlbumArtDrawable(service, bitmap)
                            val widgetRadius = getWidgetRadius(service)
                            val roundedBitmap = createRoundedBitmap(image, imageSize, imageSize, widgetRadius, 0F, widgetRadius, 0F)
                            appWidgetView.setImageViewBitmap(R.id.image, roundedBitmap)

                            pushUpdate(service, appWidgetIds, appWidgetView)
                        }
                    })
                    .build()
            )
        }
    }

    /**
     * Link up various button actions using [PendingIntent].
     */
    private fun linkButtons(context: Context, views: RemoteViews) {
        val action = Intent(context, MainActivity::class.java)
        val serviceName = ComponentName(context, PlaybackService::class.java)

        // Home
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        var pendingIntent = PendingIntent.getActivity(context, 0, action, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.image, pendingIntent)
        views.setOnClickPendingIntent(R.id.media_titles, pendingIntent)

        // Play and pause
        pendingIntent = buildPendingIntent(context, PlaybackService.ACTION_TOGGLE_PAUSE, serviceName)
        views.setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent)
    }

    private fun getImageSize(context: Context): Int {
        if (imageSize == 0) {
            imageSize = context.resources.getDimensionPixelSize(R.dimen.app_widget_simple_image_size)
        }
        return imageSize
    }

    companion object {

        const val NAME = "app_widget_simple"

        private var mInstance: AppWidgetSimple? = null
        private var imageSize = 0

        val instance: AppWidgetSimple
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = AppWidgetSimple()
                }
                return mInstance!!
            }
    }
}
