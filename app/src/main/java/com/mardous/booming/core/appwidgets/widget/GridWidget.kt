package com.mardous.booming.core.appwidgets.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import com.mardous.booming.core.appwidgets.component.ActionButton
import com.mardous.booming.core.appwidgets.component.PlayPauseButton
import com.mardous.booming.core.appwidgets.component.WidgetArtwork
import com.mardous.booming.core.appwidgets.component.WidgetShape
import com.mardous.booming.core.appwidgets.config.WidgetAction
import com.mardous.booming.core.appwidgets.config.WidgetConfig
import com.mardous.booming.core.appwidgets.config.WidgetSetting
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.WidgetColors
import com.mardous.booming.core.appwidgets.BoomingWidget
import com.mardous.booming.core.appwidgets.openApp

/** launcher cell size after host padding */
private val MIN_CELL = 72.dp

/** requires SizeMode.Exact because responsive sizes are breakpoints */
internal fun cellsAcross(edge: Dp, gap: Dp, max: Int, min: Dp = MIN_CELL): Int =
    ((edge + gap) / (min + gap)).toInt().coerceIn(1, max)

class GridWidget : BoomingWidget() {

    /** No hide-controls here, but the actions can still be changed. */
    override val settings = listOf(
        WidgetSetting.DynamicColors,
        WidgetSetting.CellLeadingAction,
        WidgetSetting.CellTrailingAction
    )

    override val sizeMode = SizeMode.Exact

    @Composable
    override fun Content(playbackState: PlaybackState, colors: WidgetColors, config: WidgetConfig) {
        val size = LocalSize.current
        val across = cellsAcross(size.width, GAP, max = CELLS)
        val down = cellsAcross(size.height, GAP, max = CELLS, min = MIN_DOWN)
        when {
            across == 2 && down == 2 -> Block(playbackState, colors, config)
            down > across -> Line(playbackState, colors, config, vertical = true, count = down)
            else -> Line(playbackState, colors, config, vertical = false, count = across)
        }
    }

    @Composable
    private fun Block(playbackState: PlaybackState, colors: WidgetColors, config: WidgetConfig) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                val cell = GlanceModifier.defaultWeight().fillMaxHeight()
                Artwork(playbackState, colors, cell)
                Spacer(GlanceModifier.width(GAP))
                PlayPauseButton(cell, playbackState, colors, WidgetShape.Button)
            }
            val leading = config.leading
            val trailing = config.trailing
            if (leading == WidgetAction.None && trailing == WidgetAction.None) return@Column
            Spacer(GlanceModifier.height(GAP))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                val cell = GlanceModifier.defaultWeight().fillMaxHeight()
                ActionButton(leading, cell, playbackState, colors, WidgetShape.Button)
                if (leading != WidgetAction.None && trailing != WidgetAction.None) {
                    Spacer(GlanceModifier.width(GAP))
                }
                ActionButton(trailing, cell, playbackState, colors, WidgetShape.Button)
            }
        }
    }

    @Composable
    private fun Line(
        playbackState: PlaybackState,
        colors: WidgetColors,
        config: WidgetConfig,
        vertical: Boolean,
        count: Int
    ) {
        if (vertical) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Cells(
                    playbackState, colors, config, count,
                    cell = GlanceModifier.defaultWeight().fillMaxWidth(),
                    gap = GlanceModifier.height(GAP)
                )
            }
        } else {
            Row(modifier = GlanceModifier.fillMaxSize()) {
                Cells(
                    playbackState, colors, config, count,
                    cell = GlanceModifier.defaultWeight().fillMaxHeight(),
                    gap = GlanceModifier.width(GAP)
                )
            }
        }
    }

    @Composable
    private fun Cells(
        playbackState: PlaybackState,
        colors: WidgetColors,
        config: WidgetConfig,
        count: Int,
        cell: GlanceModifier,
        gap: GlanceModifier
    ) {
        Artwork(playbackState, colors, cell)
        if (count >= 4 && config.leading != WidgetAction.None) {
            Spacer(gap)
            ActionButton(config.leading, cell, playbackState, colors, WidgetShape.Button)
        }
        if (count >= 2) {
            Spacer(gap)
            PlayPauseButton(cell, playbackState, colors, WidgetShape.Button)
        }
        if (count >= 3 && config.trailing != WidgetAction.None) {
            Spacer(gap)
            ActionButton(config.trailing, cell, playbackState, colors, WidgetShape.Button)
        }
    }

    @Composable
    private fun Artwork(playbackState: PlaybackState, colors: WidgetColors, modifier: GlanceModifier) {
        WidgetArtwork(
            songId = playbackState.songId,
            colors = colors,
            modifier = modifier.openApp(),
            shape = WidgetShape.Button
        )
    }

    private companion object {
        val GAP = 8.dp

        val MIN_DOWN = 96.dp

        const val CELLS = 4
    }
}
