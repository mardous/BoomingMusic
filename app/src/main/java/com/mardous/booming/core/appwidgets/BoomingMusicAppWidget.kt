package com.mardous.booming.core.appwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import coil3.Image
import coil3.SingletonImageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import coil3.target.Target
import coil3.toBitmap
import com.mardous.booming.R
import com.mardous.booming.core.model.widget.WidgetLayout
import com.mardous.booming.core.model.widget.WidgetState
import com.mardous.booming.extensions.getTintedDrawable
import com.mardous.booming.extensions.resources.getDrawableCompat
import com.mardous.booming.extensions.resources.getPrimaryTextColor
import com.mardous.booming.extensions.resources.toBitmap
import com.mardous.booming.playback.PlaybackService
import com.mardous.booming.ui.screen.MainActivity
import com.mardous.booming.util.WidgetUtils.createRoundedBitmap
import kotlinx.serialization.json.Json

class BoomingMusicAppWidget : AppWidgetProvider() {

    private var imageDisposable: Disposable? = null

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        val state = deserializeState(context)

        val layout = when {
            minWidth >= 280 && minHeight >= 100 -> WidgetLayout.Big
            minWidth >= 280 -> WidgetLayout.Small
            else -> WidgetLayout.Simple
        }

        val views = RemoteViews(context.packageName, layout.layoutRes)
        views.setupWidgetLayout(context, layout, state) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        views.setupButtons(context)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun RemoteViews.setupWidgetLayout(
        context: Context,
        layout: WidgetLayout,
        state: WidgetState,
        onImage: () -> Unit
    ) {
        if (state == WidgetState.empty) {
            setViewVisibility(R.id.media_titles, View.INVISIBLE)
        } else {
            setViewVisibility(R.id.media_titles, View.VISIBLE)
            setTextViewText(R.id.title, state.title)
            setTextViewText(R.id.text, "${state.artist} • ${state.album}")
        }

        val playPauseRes = if (state.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp
        if (layout == WidgetLayout.Big) {
            val iconColor = getPrimaryTextColor(context, isDark = false)
            setTintedDrawable(context, R.id.button_next, R.drawable.ic_next_24dp, iconColor)
            setTintedDrawable(context, R.id.button_prev, R.drawable.ic_previous_24dp, iconColor)
            setTintedDrawable(context, R.id.button_toggle_play_pause, playPauseRes, iconColor)
        } else {
            setImageViewResource(R.id.button_next, R.drawable.ic_next_24dp)
            setImageViewResource(R.id.button_prev, R.drawable.ic_previous_24dp)
            setImageViewResource(R.id.button_toggle_play_pause, playPauseRes)
        }

        if (imageDisposable != null) {
            imageDisposable?.dispose()
            imageDisposable = null
        }

        imageDisposable = if (state.artwork == null) {
            setAlbumArt(context, layout, R.id.image, null)
            null
        } else {
            SingletonImageLoader.get(context).enqueue(
                ImageRequest.Builder(context)
                    .data(state.artwork)
                    .size(context.resources.getDimensionPixelSize(layout.imageSizeRes))
                    .scale(Scale.FILL)
                    .crossfade(false)
                    .target(object : Target {
                        override fun onError(error: Image?) {
                            setAlbumArt(context, layout, R.id.image, null)
                            onImage()
                        }

                        override fun onSuccess(result: Image) {
                            setAlbumArt(context, layout, R.id.image, result.toBitmap())
                            onImage()
                        }
                    })
                    .build()
            )
        }
    }

    private fun RemoteViews.setAlbumArt(
        context: Context,
        layout: WidgetLayout,
        @IdRes viewId: Int,
        bitmap: Bitmap?
    ) {
        val size = context.resources.getDimensionPixelSize(layout.imageSizeRes)
        val radius = when (layout) {
            WidgetLayout.Simple,
            WidgetLayout.Big -> context.resources.getDimension(R.dimen.app_widget_background_radius)
            WidgetLayout.Small -> context.resources.getDimension(R.dimen.app_widget_inner_radius)
        }
        if (radius > 0f) {
            val artwork = bitmap?.toDrawable(context.resources)
                ?: context.getDrawableCompat(R.drawable.default_audio_art)
            checkNotNull(artwork)

            val roundedBitmap = when (layout) {
                WidgetLayout.Simple -> createRoundedBitmap(artwork, size, size, radius, 0f, radius, 0f)
                WidgetLayout.Small,
                WidgetLayout.Big -> createRoundedBitmap(artwork, size, radius)
            }
            setImageViewBitmap(viewId, roundedBitmap)
        } else {
            if (bitmap == null) {
                setImageViewResource(viewId, R.drawable.default_audio_art)
            } else {
                setImageViewBitmap(viewId, bitmap)
            }
        }
    }

    private fun RemoteViews.setTintedDrawable(
        context: Context,
        @IdRes viewId: Int,
        @DrawableRes resourceId: Int,
        @ColorInt color: Int
    ) {
        val tintedDrawable = context.getTintedDrawable(resourceId, color)
        checkNotNull(tintedDrawable)
        setImageViewBitmap(viewId, tintedDrawable.toBitmap())
    }

    private fun RemoteViews.setupButtons(context: Context) {
        val action = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(context, 0, action, PendingIntent.FLAG_IMMUTABLE)
        setOnClickPendingIntent(R.id.image, pendingIntent)
        setOnClickPendingIntent(R.id.media_titles, pendingIntent)

        setButtonAction(context, R.id.button_prev, PlaybackService.ACTION_PREVIOUS)
        setButtonAction(context, R.id.button_next, PlaybackService.ACTION_NEXT)
        setButtonAction(context, R.id.button_toggle_play_pause, PlaybackService.ACTION_TOGGLE_PAUSE)
    }

    private fun RemoteViews.setButtonAction(context: Context, buttonId: Int, action: String) {
        setOnClickPendingIntent(
            buttonId,
            PendingIntent.getForegroundService(
                context,
                0,
                Intent(context, PlaybackService::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    companion object {
        private const val PREFERENCE_NAME = "widget_preferences"
        private const val WIDGET_STATE = "last_state"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = false
        }

        fun serializeState(context: Context, state: WidgetState) {
            val widgetPrefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            widgetPrefs.edit(commit = true) { putString(WIDGET_STATE, json.encodeToString(state)) }
        }

        fun deserializeState(context: Context): WidgetState {
            val widgetPrefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            val state = widgetPrefs.getString(WIDGET_STATE, null)
            return if (state == null) WidgetState.empty else json.decodeFromString(state)
        }
    }
}