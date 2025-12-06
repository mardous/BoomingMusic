package com.mardous.booming.core.model.widget

import androidx.annotation.DimenRes
import androidx.annotation.LayoutRes
import com.mardous.booming.R

enum class WidgetLayout(@LayoutRes val layoutRes: Int, @DimenRes val imageSizeRes: Int) {
    Big(R.layout.app_widget_big, R.dimen.app_widget_big_image_size),
    Small(R.layout.app_widget_small, R.dimen.app_widget_small_image_size),
    Simple(R.layout.app_widget_simple, R.dimen.app_widget_simple_image_size)
}