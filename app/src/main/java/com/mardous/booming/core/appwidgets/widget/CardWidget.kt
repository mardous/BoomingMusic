package com.mardous.booming.core.appwidgets.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.TextAlign
import com.mardous.booming.core.appwidgets.component.ActionButton
import com.mardous.booming.core.appwidgets.component.WidgetArtwork
import com.mardous.booming.core.appwidgets.component.NextButton
import com.mardous.booming.core.appwidgets.component.PlayPauseButton
import com.mardous.booming.core.appwidgets.component.PreviousButton
import com.mardous.booming.core.appwidgets.component.TrackText
import com.mardous.booming.core.appwidgets.component.WavyProgress
import com.mardous.booming.core.appwidgets.component.WidgetShape
import com.mardous.booming.core.appwidgets.component.fillBackground
import com.mardous.booming.core.appwidgets.config.WidgetAction
import com.mardous.booming.core.appwidgets.config.WidgetConfig
import com.mardous.booming.core.appwidgets.config.WidgetSetting
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.WidgetColors
import com.mardous.booming.core.appwidgets.BoomingWidget
import com.mardous.booming.core.appwidgets.openApp

class CardWidget : BoomingWidget() {

    override val settings = listOf(
        WidgetSetting.DynamicColors,
        WidgetSetting.ShowProgress,
        WidgetSetting.ShowControls,
        WidgetSetting.LeadingAction,
        WidgetSetting.TrailingAction
    )

    override val sizeMode = SizeMode.Exact

    @Composable
    override fun Content(playbackState: PlaybackState, colors: WidgetColors, config: WidgetConfig) {
        val size = LocalSize.current
        when {
            size.height < STACK_FROM -> SingleRow(playbackState, colors, config)
            // Hero wants a cover worth showing, which is a question of shape rather than height alone
            size.height >= HERO_FROM && size.height >= size.width * HERO_RATIO ->
                Hero(playbackState, colors, config)

            else -> Stacked(
                playbackState, colors, config,
                // The only thing the extra height buys, now that the header is a fixed size
                timeline = config.showProgress && size.height >= TIMELINE_FROM
            )
        }
    }

    /** Both stacked layouts inset their content by [GAP] on each side. */
    @Composable
    private fun trackWidth(): Dp = LocalSize.current.width - GAP * 2

    @Composable
    private fun SingleRow(playbackState: PlaybackState, colors: WidgetColors, config: WidgetConfig) {
        val size = LocalSize.current
        val content = size.height - GAP * 2
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .fillBackground(colors.surface, WidgetShape.Card)
                .openApp()
                .padding(GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetArtwork(playbackState.songId, colors, GlanceModifier.size(min(ROW_ART, content)))
            Details(playbackState, colors)
            if (config.showControls) {
                val button = min(ROW_BUTTON, content)
                // Three columns leave the title too little room to also carry a second button
                if (size.width >= FOUR_COLUMNS) {
                    NextButton(GlanceModifier.size(button), colors, WidgetShape.Scallop)
                    Spacer(GlanceModifier.width(GAP))
                }
                PlayPauseButton(GlanceModifier.size(button), playbackState, colors, WidgetShape.Stadium)
            }
        }
    }

    @Composable
    private fun Hero(playbackState: PlaybackState, colors: WidgetColors, config: WidgetConfig) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .fillBackground(colors.surface, WidgetShape.Card)
                .openApp()
                .padding(GAP),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WidgetArtwork(playbackState.songId, colors, GlanceModifier.fillMaxWidth().defaultWeight())
            Spacer(GlanceModifier.height(GAP))
            TrackText(playbackState, colors, 16.sp, 13.sp, align = TextAlign.Center)
            if (config.showProgress) {
                Spacer(GlanceModifier.height(GAP))
                WavyProgress(playbackState, colors, trackWidth = trackWidth())
            }
            if (config.showControls) {
                Spacer(GlanceModifier.height(GAP))
                Transport(playbackState, colors, config)
            }
        }
    }

    @Composable
    private fun Stacked(
        playbackState: PlaybackState,
        colors: WidgetColors,
        config: WidgetConfig,
        timeline: Boolean
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .fillBackground(colors.surface, WidgetShape.Card)
                .openApp()
                .padding(GAP),
            // Only reached when nothing below has a weight to spread, i.e. controls and timeline are off
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WidgetArtwork(playbackState.songId, colors, GlanceModifier.size(ROW_ART))
                Details(playbackState, colors)
            }
            if (timeline) {
                Spacer(GlanceModifier.defaultWeight())
                WavyProgress(playbackState, colors, trackWidth = trackWidth())
            }
            if (config.showControls) {
                Spacer(GlanceModifier.defaultWeight())
                Transport(playbackState, colors, config)
            }
        }
    }

    @Composable
    private fun RowScope.Details(playbackState: PlaybackState, colors: WidgetColors) {
        Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = GAP)) {
            TrackText(playbackState, colors, 16.sp, 13.sp, titleLines = 2)
        }
    }
    @Composable
    private fun Transport(playbackState: PlaybackState, colors: WidgetColors, config: WidgetConfig) {
        val width = LocalSize.current.width
        val flanked = width >= FIVE_FROM
        val skips = width >= THREE_FROM
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(CONTROL_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (flanked && config.leading != WidgetAction.None) {
                Control(config.leading, playbackState, colors)
                Spacer(GlanceModifier.width(CONTROL_GAP))
            }
            if (skips) {
                PreviousButton(GlanceModifier.size(CONTROL_HEIGHT), colors, WidgetShape.Scallop)
                Spacer(GlanceModifier.width(CONTROL_GAP))
            }
            // The signature pill: a fixed width, never traded away to fit another control
            PlayPauseButton(
                modifier = GlanceModifier.width(PLAY_WIDTH).fillMaxHeight(),
                playbackState = playbackState,
                colors = colors,
                shape = WidgetShape.Stadium
            )
            if (skips) {
                Spacer(GlanceModifier.width(CONTROL_GAP))
                NextButton(GlanceModifier.size(CONTROL_HEIGHT), colors, WidgetShape.Scallop)
            }
            if (flanked && config.trailing != WidgetAction.None) {
                Spacer(GlanceModifier.width(CONTROL_GAP))
                Control(config.trailing, playbackState, colors)
            }
        }
    }

    @Composable
    private fun Control(action: WidgetAction, playbackState: PlaybackState, colors: WidgetColors) {
        ActionButton(action, GlanceModifier.size(CONTROL_HEIGHT), playbackState, colors, WidgetShape.Scallop)
    }

    private companion object {
        /** The single row's own metrics; it insets by [GAP] like the stacked layouts do */
        val ROW_ART = 80.dp
        val ROW_BUTTON = 56.dp

        /** Below four columns the title has no room for a second button beside i. */
        val FOUR_COLUMNS = 300.dp

        val GAP = 12.dp
        val CONTROL_HEIGHT = 48.dp
        val PLAY_WIDTH = 96.dp
        val CONTROL_GAP = 8.dp
        val STACK_FROM = 170.dp
        val TIMELINE_FROM = 200.dp
        val HERO_FROM = 260.dp
        const val HERO_RATIO = 0.7f

        /** Derived from the metrics */
        val THREE_FROM = CONTROL_HEIGHT * 2 + CONTROL_GAP * 2 + PLAY_WIDTH + GAP * 2
        val FIVE_FROM = CONTROL_HEIGHT * 4 + CONTROL_GAP * 4 + PLAY_WIDTH + GAP * 2
    }
}
