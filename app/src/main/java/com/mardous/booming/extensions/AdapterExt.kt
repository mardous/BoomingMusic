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
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.ImageRequest
import coil3.toBitmap
import com.google.android.material.card.MaterialCardView
import com.mardous.booming.coil.placeholderDrawableRes
import com.mardous.booming.core.model.PaletteColor
import com.mardous.booming.core.palette.PaletteProcessor
import com.mardous.booming.extensions.resources.getPrimaryTextColor
import com.mardous.booming.extensions.resources.isColorLight
import com.mardous.booming.extensions.resources.toColorStateList
import com.mardous.booming.extensions.resources.withAlpha
import com.mardous.booming.ui.component.base.MediaEntryViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun MediaEntryViewHolder.setColor(color: PaletteColor) {
    if (paletteColorContainer == null) return

    if (paletteColorContainer is CardView) {
        paletteColorContainer.setCardBackgroundColor(color.backgroundColor)
    } else {
        paletteColorContainer.setBackgroundColor(color.backgroundColor)
    }

    title?.setTextColor(color.primaryTextColor)
    text?.setTextColor(color.secondaryTextColor)
    imageText?.setTextColor(color.secondaryTextColor)
    menu?.iconTint = color.secondaryTextColor.toColorStateList()
    play?.backgroundTintList = color.primaryTextColor.withAlpha(0.85f).toColorStateList()
    play?.iconTint = getPrimaryTextColor(itemView.context, isDark = color.primaryTextColor.isColorLight)
        .toColorStateList()
}

fun MediaEntryViewHolder.loadPaletteImage(
    data: Any?,
    placeholderRes: Int,
    builder: ImageRequest.Builder.() -> Unit = {}
) =
    image?.load(data) {
        placeholderDrawableRes(itemView.context, placeholderRes)
        builder()
        listener(
            onError = { request, result ->
                setColor(PaletteColor.errorColor(itemView.context))
            },
            onSuccess = { request, result ->
                if (paletteColorContainer != null) {
                    itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                        val color = withContext(Dispatchers.Default) {
                            PaletteProcessor.getPaletteColor(
                                itemView.context,
                                result.image.toBitmap()
                            )
                        }
                        if (isActive) setColor(color)
                    }
                }
            })
    }

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

val RecyclerView.ViewHolder.isValidPosition: Boolean
    get() = layoutPosition > -1

val RecyclerView.Adapter<*>?.isNullOrEmpty: Boolean
    get() = this == null || isEmpty

val RecyclerView.Adapter<*>.isEmpty: Boolean
    get() = itemCount == 0