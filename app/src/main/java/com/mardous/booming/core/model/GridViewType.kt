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

package com.mardous.booming.core.model

import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.Px
import com.mardous.booming.R

enum class GridViewType(@IdRes val itemId: Int, @LayoutRes val layoutRes: Int, val margin: Int = 4) {
    Normal(R.id.action_view_type_normal, R.layout.item_grid),
    Card(R.id.action_view_type_card, R.layout.item_card),
    ColoredCard(R.id.action_view_type_colored_card, R.layout.item_card_color),
    Circle(R.id.action_view_type_circle, R.layout.item_grid_circle),
    Image(R.id.action_view_type_image, R.layout.item_image_gradient, 0);

    companion object {
        @Px
        fun getMarginForLayout(@LayoutRes layoutRes: Int): Int {
            return entries.firstOrNull { type -> type.layoutRes == layoutRes }?.margin ?: 0
        }
    }
}