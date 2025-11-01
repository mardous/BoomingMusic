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

package com.mardous.booming.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * @author Christians M. A. (mardous)
 */
@SuppressLint("NotifyDataSetChanged")
class SimpleItemAdapter<T>(
    private val layoutRes: Int = android.R.layout.simple_list_item_1,
    private val textViewId: Int = android.R.id.text1,
    items: List<T> = listOf(),
    private val callback: Callback<T>
) : RecyclerView.Adapter<SimpleItemAdapter<T>.ViewHolder>() {

    var items: List<T> = items
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context).inflate(layoutRes, parent, false).let {
            ViewHolder(it)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (callback.bindData(holder.itemView, position, items[position]))
            return

        holder.textView.text = items[position].toString()
    }

    override fun getItemCount(): Int = items.size

    interface Callback<T> {
        fun bindData(itemView: View, position: Int, item: T) = false
        fun itemClick(itemView: View, position: Int, item: T)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val textView: TextView = itemView.findViewById(textViewId)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val position = layoutPosition
            callback.itemClick(view, position, items[position])
        }
    }
}