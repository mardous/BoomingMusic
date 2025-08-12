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

package com.mardous.booming.ui.adapters.preference

import android.annotation.SuppressLint
import com.mardous.booming.R
import com.mardous.booming.ui.adapters.DraggableItemAdapter
import com.mardous.booming.extensions.showToast
import com.mardous.booming.core.model.CategoryInfo

class CategoryInfoAdapter(
    categoryInfos: MutableList<CategoryInfo>
) : DraggableItemAdapter<CategoryInfo>(categoryInfos) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val categoryInfo = items[position]

        holder.title.text = holder.title.resources.getString(categoryInfo.category.titleRes)
        holder.checkBox.isChecked = categoryInfo.visible

        holder.itemView.setOnClickListener {
            if (!canCheckCategory(categoryInfo)) {
                holder.itemView.context.showToast(R.string.you_cannot_select_more_than_five_categories)
                return@setOnClickListener
            }

            if (!(categoryInfo.visible && isLastCheckedCategory(categoryInfo))) {
                categoryInfo.visible = !categoryInfo.visible
                holder.checkBox.isChecked = categoryInfo.visible
            } else {
                holder.itemView.context.showToast(R.string.you_have_to_select_at_least_one_category)
            }
        }
    }

    private fun canCheckCategory(categoryInfo: CategoryInfo): Boolean {
        return items.count { it.visible } < CategoryInfo.MAX_VISIBLE_CATEGORIES || categoryInfo.visible
    }

    private fun isLastCheckedCategory(categoryInfo: CategoryInfo): Boolean {
        if (categoryInfo.visible) {
            for (c in items) {
                if (c != categoryInfo && c.visible) return false
            }
        }
        return true
    }

}