@file:SuppressLint("LocalContextGetResourceValueCall")

package com.mardous.booming.ui.component.compose.menu

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

@Immutable
sealed class MenuItem(val enabled: Boolean, val visible: Boolean) {
    sealed class Button(
        val text: String,
        val icon: Painter? = null,
        val dangerous: Boolean = false,
        val onClick: () -> Unit,
        enabled: Boolean = true,
        visible: Boolean = true
    ) : MenuItem(enabled, visible) {
        class Action(
            text: String,
            icon: Painter,
            onClick: () -> Unit,
            enabled: Boolean = true,
            visible: Boolean = true
        ) : Button(text, icon, false, onClick, enabled, visible)

        class DropDown(
            text: String,
            icon: Painter? = null,
            dangerous: Boolean = false,
            onClick: () -> Unit,
            enabled: Boolean = true,
            visible: Boolean = true
        ) : Button(text, icon, dangerous, onClick, enabled, visible)

        class Checkable(
            val isChecked: Boolean,
            text: String,
            icon: Painter? = null,
            val isSingleSelection: Boolean = false,
            val onCheckedChange: (Boolean) -> Unit,
            enabled: Boolean = true,
            visible: Boolean = true
        ) : Button(text, icon, false, { onCheckedChange(!isChecked) }, enabled, visible)
    }

    @Immutable
    object Divider : MenuItem(true, true)
}