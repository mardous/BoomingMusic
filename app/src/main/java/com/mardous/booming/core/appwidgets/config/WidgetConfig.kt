package com.mardous.booming.core.appwidgets.config

import com.mardous.booming.core.appwidgets.WidgetData

data class WidgetConfig(
    val dynamicColors: Boolean = false,
    val showProgress: Boolean = true,
    val showControls: Boolean = true,
    val showArtist: Boolean = true,
    val showNavigation: Boolean = true,
    val leading: WidgetAction = WidgetAction.None,
    val trailing: WidgetAction = WidgetAction.None,
    val source: SongSource = SongSource.Recent,
    val page: Int = 0
) {
    fun dataNeeds(settings: List<WidgetSetting>): Set<WidgetData> = buildSet {
        for (setting in settings) when (setting) {
            is WidgetSetting.Switch -> if (setting.read(this@WidgetConfig)) {
                setting.requires?.let(::add)
            }

            is WidgetSetting.Choice -> if (setting.isAvailable(this@WidgetConfig)) {
                setting.read(this@WidgetConfig).requires?.let(::add)
            }
        }
    }
}
