/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.ui.component.menu

import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu

typealias MenuConsumer = (Menu) -> Unit

fun newPopupMenu(anchor: View, menuRes: Int, menuConsumer: MenuConsumer? = null): PopupMenu {
    return PopupMenu(anchor.context, anchor).apply {
        inflate(menuRes)
        if (menuConsumer != null) {
            menuConsumer(menu)
        }
    }
}

abstract class OnClickMenu : View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    override fun onClick(v: View) {
        newPopupMenu(v, popupMenuRes) { menu ->
            onPreparePopup(menu)
        }.also { popup ->
            popup.setOnMenuItemClickListener(this)
            popup.show()
        }
    }

    protected abstract val popupMenuRes: Int

    protected open fun onPreparePopup(menu: Menu) {}
}