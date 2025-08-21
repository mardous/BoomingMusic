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

package com.mardous.booming.extensions

import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mardous.booming.extensions.resources.toColorStateList
import com.mardous.booming.ui.component.base.MediaEntryViewHolder
import com.mardous.booming.util.color.MediaNotificationProcessor

fun MediaEntryViewHolder.setColors(colors: MediaNotificationProcessor) {
    if (paletteColorContainer != null) {
        if (paletteColorContainer is CardView) {
            paletteColorContainer.setCardBackgroundColor(colors.backgroundColor)
        } else {
            paletteColorContainer.setBackgroundColor(colors.backgroundColor)
        }
        imageGradient?.backgroundTintList = colors.backgroundColor.toColorStateList()
    } else return

    title?.setTextColor(colors.primaryTextColor)
    text?.setTextColor(colors.secondaryTextColor)
    imageText?.setTextColor(colors.secondaryTextColor)
    menu?.iconTint = colors.secondaryTextColor.toColorStateList()
}

val RecyclerView.ViewHolder.isValidPosition: Boolean
    get() = layoutPosition > -1

val RecyclerView.Adapter<*>?.isNullOrEmpty: Boolean
    get() = this == null || isEmpty

val RecyclerView.Adapter<*>.isEmpty: Boolean
    get() = itemCount == 0

var MediaEntryViewHolder.isActivated: Boolean
    get() = if (itemView is MaterialCardView) (itemView as MaterialCardView).isChecked else itemView.isActivated
    set(activated) {
        if (itemView is MaterialCardView) {
            (itemView as MaterialCardView).apply {
                isCheckable = true
                isChecked = activated
            }
        } else {
            itemView.isActivated = activated
        }
    }