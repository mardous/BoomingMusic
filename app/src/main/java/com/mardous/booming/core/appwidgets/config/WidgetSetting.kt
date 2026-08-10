package com.mardous.booming.core.appwidgets.config

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mardous.booming.R
import com.mardous.booming.core.appwidgets.WidgetData

sealed class WidgetSetting(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val key: String
) {

    class Switch(
        @StringRes title: Int,
        @StringRes val summary: Int,
        @DrawableRes icon: Int,
        key: String,
        val default: Boolean,
        val read: (WidgetConfig) -> Boolean,
        val write: (WidgetConfig, Boolean) -> WidgetConfig,
        val requires: WidgetData? = null
    ) : WidgetSetting(title, icon, key)

    class Choice(
        @StringRes title: Int,
        @DrawableRes icon: Int,
        key: String,
        val values: List<WidgetValue>,
        val default: WidgetValue,
        val read: (WidgetConfig) -> WidgetValue,
        val write: (WidgetConfig, WidgetValue) -> WidgetConfig,
        val isAvailable: (WidgetConfig) -> Boolean = { true }
    ) : WidgetSetting(title, icon, key)

    companion object {

        val DynamicColors = Switch(
            title = R.string.widget_dynamic_colors_title,
            summary = R.string.widget_dynamic_colors_summary,
            icon = R.drawable.ic_palette_24dp,
            key = "DynamicColors",
            default = false,
            read = { it.dynamicColors },
            write = { config, value -> config.copy(dynamicColors = value) },
            requires = WidgetData.Palette
        )

        val ShowProgress = Switch(
            title = R.string.widget_show_progress_title,
            summary = R.string.widget_show_progress_summary,
            icon = R.drawable.ic_squiggly_seekbar_24dp,
            key = "ShowProgress",
            default = true,
            read = { it.showProgress },
            write = { config, value -> config.copy(showProgress = value) },
            requires = WidgetData.Progress
        )

        val ShowControls = Switch(
            title = R.string.widget_show_controls_title,
            summary = R.string.widget_show_controls_summary,
            icon = R.drawable.ic_play_24dp,
            key = "ShowControls",
            default = true,
            read = { it.showControls },
            write = { config, value -> config.copy(showControls = value) }
        )

        val ShowArtist = Switch(
            title = R.string.widget_show_artist_title,
            summary = R.string.widget_show_artist_summary,
            icon = R.drawable.ic_artist_24dp,
            key = "ShowArtist",
            default = true,
            read = { it.showArtist },
            write = { config, value -> config.copy(showArtist = value) }
        )

        val ShowNavigation = Switch(
            title = R.string.widget_show_navigation_title,
            summary = R.string.widget_show_navigation_summary,
            icon = R.drawable.ic_arrow_forward_24dp,
            key = "ShowNavigation",
            default = true,
            read = { it.showNavigation },
            write = { config, value -> config.copy(showNavigation = value) }
        )

        val LeadingAction =
            leading(WidgetAction.flanking, WidgetAction.None, R.drawable.ic_shuffle_24dp)

        val TrailingAction =
            trailing(WidgetAction.flanking, WidgetAction.None, R.drawable.ic_repeat_24dp)

        val CellLeadingAction = leading(
            WidgetAction.substituting(WidgetAction.Previous),
            WidgetAction.Previous,
            R.drawable.ic_previous_m3_24dp
        )

        val CellTrailingAction = trailing(
            WidgetAction.substituting(WidgetAction.Next),
            WidgetAction.Next,
            R.drawable.ic_next_m3_24dp
        )

        val SideButtons = leading(
            values = WidgetAction.substituting(WidgetAction.Skip),
            default = WidgetAction.Skip,
            icon = R.drawable.ic_next_m3_24dp,
            title = R.string.widget_side_buttons_title
        )

        val Source = Choice(
            title = R.string.widget_source_title,
            icon = R.drawable.ic_library_music_24dp,
            key = "Source",
            values = SongSource.entries,
            default = SongSource.Recent,
            read = { it.source },
            write = { config, value -> config.copy(source = value as SongSource) }
        )

        private fun leading(
            values: List<WidgetAction>,
            default: WidgetAction,
            @DrawableRes icon: Int,
            @StringRes title: Int = R.string.widget_leading_action_title
        ) = Choice(
            title = title,
            icon = icon,
            key = "LeadingAction",
            values = values,
            default = default,
            read = { it.leading },
            write = { config, value -> config.copy(leading = value as WidgetAction) },
            isAvailable = { it.showControls }
        )

        private fun trailing(
            values: List<WidgetAction>,
            default: WidgetAction,
            @DrawableRes icon: Int
        ) = Choice(
            title = R.string.widget_trailing_action_title,
            icon = icon,
            key = "TrailingAction",
            values = values,
            default = default,
            read = { it.trailing },
            write = { config, value -> config.copy(trailing = value as WidgetAction) },
            isAvailable = { it.showControls }
        )
    }
}
