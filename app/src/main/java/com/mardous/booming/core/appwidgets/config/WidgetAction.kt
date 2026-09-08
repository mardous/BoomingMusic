package com.mardous.booming.core.appwidgets.config

import com.mardous.booming.R
import com.mardous.booming.core.appwidgets.WidgetData

enum class WidgetAction(
    override val label: Int,
    override val requires: WidgetData? = null
) : WidgetValue {
    Previous(R.string.action_previous),
    Next(R.string.action_next),

    Skip(R.string.widget_side_buttons_skip),
    Shuffle(R.string.shuffle_action),
    Favourite(R.string.widget_action_favourite, WidgetData.Favourite),
    Repeat(R.string.widget_action_repeat),
    None(R.string.label_none);

    companion object {
        val flanking = listOf(Shuffle, Favourite, Repeat, None)

        fun substituting(default: WidgetAction) = listOf(default) + flanking
    }
}
