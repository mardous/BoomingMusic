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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mardous.booming.R
import com.mardous.booming.data.model.Suggestion
import com.mardous.booming.ui.IHomeCallback
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * @author Christians M. A. (mardous)
 */
@SuppressLint("NotifyDataSetChanged")
class HomeAdapter(
    dataSet: List<Suggestion>,
    private val callback: IHomeCallback
) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    var dataSet: List<Suggestion> by Delegates.observable(dataSet) { _: KProperty<*>, _: List<Suggestion>, _: List<Suggestion> ->
        notifyDataSetChanged()
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_suggestion, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = dataSet[position]
        holder.headingTitle?.setText(suggestion.type.titleRes)
        if (holder.recyclerView != null) {
            holder.recyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, RecyclerView.HORIZONTAL, false)
            holder.recyclerView.adapter = callback.createSuggestionAdapter(suggestion)
            holder.recyclerView.setItemViewCacheSize(0)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].hashCode().toLong()
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        internal val headingTitle: TextView? = itemView.findViewById(R.id.heading_title)
        internal val recyclerView: RecyclerView? = itemView.findViewById(R.id.recycler_view)

        init {
            headingTitle?.setOnClickListener(this)
        }

        private val current: Suggestion?
            get() = dataSet.getOrNull(layoutPosition)

        override fun onClick(view: View) {
            if (view === headingTitle) {
                val suggestion = current ?: return
                callback.suggestionClick(suggestion)
            }
        }
    }
}