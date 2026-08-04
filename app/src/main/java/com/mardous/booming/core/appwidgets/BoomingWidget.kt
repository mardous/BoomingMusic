package com.mardous.booming.core.appwidgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import com.mardous.booming.core.appwidgets.config.WidgetConfig
import com.mardous.booming.core.appwidgets.config.WidgetConfigStore
import com.mardous.booming.core.appwidgets.config.WidgetSetting
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.state.PlaybackStateDefinition

abstract class BoomingWidget : GlanceAppWidget() {

    abstract val settings: List<WidgetSetting>

    final override val stateDefinition = PlaybackStateDefinition

    final override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        // Glance keeps the process alive for this
        WidgetUpdater.ensureData(
            context, WidgetConfigStore.read(context, appWidgetId, settings).dataNeeds(settings)
        )
        provideContent {
            // Glance retains this composition. read settings from inside it.
            val playbackState = currentState<PlaybackState>()
            val config = WidgetConfigStore.read(context, appWidgetId, settings)
            GlanceTheme {
                Content(
                    playbackState = playbackState,
                    colors = playbackState.widgetColors(config.dynamicColors),
                    config = config
                )
            }
        }
    }

    @Composable
    protected abstract fun Content(
        playbackState: PlaybackState,
        colors: WidgetColors,
        config: WidgetConfig
    )
}
