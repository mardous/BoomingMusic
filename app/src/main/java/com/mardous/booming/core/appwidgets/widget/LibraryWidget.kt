package com.mardous.booming.core.appwidgets.widget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.mardous.booming.R
import com.mardous.booming.core.appwidgets.component.WidgetArtwork
import com.mardous.booming.core.appwidgets.component.WidgetIconButton
import com.mardous.booming.core.appwidgets.component.WidgetShape
import com.mardous.booming.core.appwidgets.component.fillBackground
import com.mardous.booming.core.appwidgets.config.SongSource
import com.mardous.booming.core.appwidgets.config.WidgetConfig
import com.mardous.booming.core.appwidgets.config.WidgetConfigStore
import com.mardous.booming.core.appwidgets.config.WidgetSetting
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.WidgetColors
import com.mardous.booming.core.appwidgets.BoomingWidget
import com.mardous.booming.core.appwidgets.openList
import com.mardous.booming.core.appwidgets.playSong
import com.mardous.booming.core.appwidgets.WidgetUpdater

class LibraryWidget : BoomingWidget() {

    override val settings = listOf(
        WidgetSetting.DynamicColors,
        WidgetSetting.ShowNavigation,
        WidgetSetting.Source
    )

    override val sizeMode = SizeMode.Exact

    @Composable
    override fun Content(playbackState: PlaybackState, colors: WidgetColors, config: WidgetConfig) {
        val size = LocalSize.current
        val columns = cellsAcross(size.width, GAP, max = MAX_COLUMNS)
        val widthCover = (size.width - GAP * (columns - 1)) / columns

        // The bar only while a row of covers still fits beside it
        val bar = config.showNavigation && size.height >= NAV_HEIGHT + GAP + MIN_COVER
        val forCovers = size.height - if (bar) NAV_HEIGHT + GAP else 0.dp
        val rows = cellsAcross(forCovers, GAP, max = MAX_ROWS, min = widthCover)
        val cover = if (rows == 1) min(widthCover, forCovers) else widthCover

        val songIds = playbackState.songIdsFor(config.source)
        val perPage = columns * rows
        val pages = ((songIds.size + perPage - 1) / perPage).coerceAtLeast(1)
        val page = config.page.mod(pages)

        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (bar) {
                Navigation(config.source, colors, paged = pages > 1)
            }
            repeat(rows) { row ->
                if (bar || row > 0) Spacer(GlanceModifier.height(GAP))
                Row(modifier = GlanceModifier.height(cover)) {
                    repeat(columns) { column ->
                        if (column > 0) Spacer(GlanceModifier.width(GAP))
                        val index = page * perPage + row * columns + column
                        CoverSlot(songIds, config.source, colors, index, cover)
                    }
                }
            }
        }
    }

    @Composable
    private fun Navigation(source: SongSource, colors: WidgetColors, paged: Boolean) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(NAV_HEIGHT)
                .fillBackground(colors.surface, WidgetShape.Card)
                .padding(horizontal = NAV_INSET),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (paged) {
                StepButton(-1, R.drawable.ic_back_24dp, R.string.widget_page_previous, colors)
            }
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(horizontal = NAV_INSET)
                    .openList(source),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = LocalContext.current.getString(source.label),
                    style = TextStyle(
                        color = colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
            if (paged) {
                StepButton(1, R.drawable.ic_arrow_forward_24dp, R.string.widget_page_next, colors)
            }
        }
    }

    @Composable
    private fun StepButton(
        step: Int,
        @DrawableRes icon: Int,
        @StringRes description: Int,
        colors: WidgetColors
    ) {
        WidgetIconButton(
            modifier = GlanceModifier.size(NAV_BUTTON),
            action = actionRunCallback<CyclePageAction>(
                actionParametersOf(CyclePageAction.STEP to step)
            ),
            backgroundColor = colors.control,
            iconColor = colors.onControl,
            icon = icon,
            contentDescription = LocalContext.current.getString(description),
            shape = WidgetShape.Button
        )
    }

    @Composable
    private fun CoverSlot(
        songIds: List<Long>,
        source: SongSource,
        colors: WidgetColors,
        index: Int,
        cover: Dp
    ) {
        val songId = songIds.getOrNull(index)
        if (songId != null) {
            WidgetArtwork(
                songId = songId,
                colors = colors,
                modifier = GlanceModifier.size(cover).playSong(songId, source),
                shape = WidgetShape.Button,
                contentDescription = LocalContext.current.getString(R.string.action_play)
            )
        } else {
            Box(GlanceModifier.size(cover).fillBackground(colors.surface, WidgetShape.Button)) {}
        }
    }

    companion object {
        private val GAP = 6.dp
        private val NAV_HEIGHT = 56.dp
        private val NAV_BUTTON = 44.dp
        private val NAV_INSET = 6.dp
        private val MIN_COVER = 64.dp

        private const val MAX_COLUMNS = 4
        private const val MAX_ROWS = 2

        const val POOL = MAX_COLUMNS * MAX_ROWS * 5
    }
}

class CyclePageAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val step = parameters[STEP] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        WidgetConfigStore.stepPage(context, appWidgetId, step)
        WidgetUpdater.render(context, LibraryWidget(), glanceId)
    }

    companion object {
        val STEP = ActionParameters.Key<Int>("step")
    }
}
