package com.mardous.booming.core.appwidgets.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.fillMaxSize
import com.mardous.booming.core.appwidgets.component.RoundWidget
import com.mardous.booming.core.appwidgets.component.WidgetArtwork
import com.mardous.booming.core.appwidgets.component.WidgetShape
import com.mardous.booming.core.appwidgets.config.WidgetConfig
import com.mardous.booming.core.appwidgets.config.WidgetSetting
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.WidgetColors
import com.mardous.booming.core.appwidgets.BoomingWidget
import com.mardous.booming.core.appwidgets.openApp

class CookieWidget : BoomingWidget() {

    override val settings = listOf(
        WidgetSetting.DynamicColors,
        WidgetSetting.ShowControls,
        WidgetSetting.SideButtons
    )

    override val sizeMode = SizeMode.Exact

    @Composable
    override fun Content(playbackState: PlaybackState, colors: WidgetColors, config: WidgetConfig) {
        RoundWidget(
            playbackState = playbackState,
            colors = colors,
            showControls = config.showControls,
            satellite = config.leading,
            controlsAt = Alignment.TopStart,
            playAt = Alignment.BottomEnd,
            playShape = WidgetShape.Button
        ) {
            WidgetArtwork(
                songId = playbackState.songId,
                colors = colors,
                modifier = GlanceModifier.fillMaxSize().openApp(),
                shape = WidgetShape.Scallop
            )
        }
    }
}
